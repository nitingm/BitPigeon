package com.codingskillshub.bitpigeon.domain.models

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.QRCodeService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.domain.types.QRCodePayload
import com.codingskillshub.bitpigeon.domain.types.WifiDirectPeer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class DiscoveryModel @Inject constructor(
    private val userDao: UserDao,
    private val qrCodeService: QRCodeService,
    private val onlineChatService: OnlineChatService,
    private val wifiService: WifiCommunicationService,
    private val configurationService: ConfigurationService
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _scannedPeers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())
    private val _rememberedPeers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())

    init {
        modelScope.launch {
            onlineChatService.wifiGroupPeers.collect { peers ->
                val currentPeers = _rememberedPeers.value.toMutableList()
                peers.forEach { peer ->
                    if (!currentPeers.contains(peer)) {
                        currentPeers.add(peer)
                    }
                }
                _rememberedPeers.value = currentPeers
            }
        }
    }

    val nearbyPeers: StateFlow<List<WifiDirectPeer>> = combine(
        wifiService.discoveredPeers,
        _scannedPeers,
        _rememberedPeers,
        wifiService.peersList
    ) { discovered, scanned, remembered, peers ->
        val visibleScanned = scanned.filter { sPeer ->
            peers.any { it.deviceAddress == sPeer.deviceMacAddress || it.deviceName == sPeer.deviceName }
        }
        val visibleRemembered = remembered.filter { rPeer ->
            peers.any { it.deviceAddress == rPeer.deviceMacAddress || it.deviceName == rPeer.deviceName }
        }

        (discovered + visibleScanned + visibleRemembered).distinctBy { it.deviceMacAddress }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val peerDevicesCount: StateFlow<Int> = wifiService.peersList.map {
        it.size
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val isPeerDiscoveryActive: StateFlow<Boolean> = wifiService.isPeerDiscoveryActive

    suspend fun prepareQrPayloadText(): Pair<String, Boolean> {
        val myId = configurationService.userIdFlow.firstOrNull() ?: ""
        val user = userDao.getUserById(myId).firstOrNull() ?: User(id = myId, name = "Me", deviceAddress = "",  "", "")
        val localUserId = user.id
        val localUserName = user.name
        val localDeviceInfo = wifiService.getLocalDeviceInfo()
        val localDeviceName = localDeviceInfo?.deviceName ?: "Unknown Device"
        val localDeviceAddress = localDeviceInfo?.deviceAddress ?: "Unknown Address"
        val connectionInfo = wifiService.connectionInfo.value
        
        val finalUserId: String
        val finalUserName: String
        val finalDeviceAddress: String
        val finalDeviceName: String

        if (connectionInfo != null && !connectionInfo.isGroupOwner) {
            val groupOwnerClient = onlineChatService.availablePeerClients.value.find { it.isGroupOwner }
            val groupOwnerUser = groupOwnerClient?.user
            val ownerDevice = nearbyPeers.value.firstOrNull { nPeer ->
                nPeer.userId == groupOwnerUser?.id
            }

            finalUserId = groupOwnerUser?.id ?: localUserId
            finalUserName = groupOwnerUser?.name ?: localUserName
            finalDeviceAddress = ownerDevice?.deviceMacAddress ?: localDeviceAddress
            finalDeviceName = ownerDevice?.deviceName ?: "Group Owner"
        } else {
            finalUserId = localUserId
            finalUserName = localUserName
            finalDeviceAddress = localDeviceAddress
            finalDeviceName = localDeviceName
        }

        val payloadText = qrCodeService.createPayloadText(
            userId = finalUserId,
            userName = finalUserName,
            deviceAddress = finalDeviceAddress,
            deviceName = finalDeviceName
        )
        
        val isValid = finalDeviceAddress != "Unknown Address" && 
                      finalDeviceAddress != "" && 
                      finalDeviceName != "Unknown Device"
        if (!isValid) {
            wifiService.updateLocalDeviceInfo()
        }

        Log.d("DiscoverViewModel", "QR Payload text = $payloadText, isValid = $isValid")

        return Pair(payloadText, isValid)
    }

    fun findDeviceInPeers(deviceName: String, deviceAddress: String): WifiP2pDevice? {
        val discoveredPeers = wifiService.getDiscoveredPeers()
        return discoveredPeers.find { it.deviceAddress == deviceAddress || it.deviceName == deviceName }
    }

    fun connectToPeerFromPayloadText(qrText: String): Boolean {
        val payload = qrCodeService.parsePayloadText(qrText) ?: return false
        addToScannedPeers(WifiDirectPeer(
            deviceName = payload.deviceName,
            deviceMacAddress = payload.deviceAddress,
            isGroupOwner = false,
            userId = payload.userId,
            userName = payload.userName
        ))
        val device = findDeviceInPeers(payload.deviceName, payload.deviceAddress)
        Log.d("QRCodeService", "qrText = $qrText, payload = $payload")
        if (device != null) {
            modelScope.launch {
                delay(1000)
                wifiService.connectToPeer(device)
            }
        } else {
            Log.d("QRCodeService", "Device Not found in discovered peers")
        }

        return true
    }

    fun connectToNearbyPeer(peer: WifiDirectPeer) {
        val device = findDeviceInPeers(peer.deviceName, peer.deviceMacAddress)
        wifiService.connectToPeer(device ?: return)
    }

    private fun addToScannedPeers(discoveredPeer: WifiDirectPeer) {
        val currentPeers = _scannedPeers.value.toMutableList()

        if (!currentPeers.contains(discoveredPeer)) {
            currentPeers.add(discoveredPeer)
            _scannedPeers.value = currentPeers
        }
    }
    
    fun exitGroup() {
        _scannedPeers.value = emptyList()
    }
}
