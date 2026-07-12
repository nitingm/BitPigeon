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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class DiscoveryModel @Inject constructor(
    private val userDao: UserDao,
    private val qrCodeService: QRCodeService,
    private val onlineChatService: OnlineChatService,
    private val wifiService: WifiCommunicationService,
    private val configurationService: ConfigurationService
){

    private val _scannedPeers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())

    val nearbyPeers: StateFlow<List<WifiDirectPeer>> = combine(
        wifiService.discoveredPeers,
        _scannedPeers,
        wifiService.peersList
    ) { discovered, scanned, peers ->
        val filteredScanned = scanned.filter { sPeer ->
            peers.any { it.deviceName == sPeer.deviceName || it.deviceAddress == sPeer.deviceMacAddress }
        }
        (discovered + filteredScanned).distinctBy { it.deviceMacAddress }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun prepareQrPayloadText(): String {
        val myId = configurationService.userIdFlow.firstOrNull() ?: ""
        val user = userDao.getUserById(myId).firstOrNull() ?: User(id = myId, name = "Me", deviceAddress = "",  "", "")
        val localUserId = user.id
        val localUserName = user.name
        val localDeviceInfo = wifiService.getLocalDeviceInfo()
        val localDeviceName = localDeviceInfo?.deviceName ?: "Unknown Device"
        val localDeviceAddress = localDeviceInfo?.deviceAddress ?: "Unknown Address"
        val connectionInfo = wifiService.connectionInfo.value
        var payloadText: String

        if (connectionInfo != null && !connectionInfo.isGroupOwner) {
            val groupOwnerClient = onlineChatService.availablePeerClients.value.find { it.isGroupOwner }
            val groupOwnerUser = groupOwnerClient?.user
            val ownerDevice = nearbyPeers.value.firstOrNull { nPeer ->
                nPeer.userId == groupOwnerUser?.id
            }

            payloadText = qrCodeService.createPayloadText(
                userId = groupOwnerUser?.id ?: localUserId,
                userName = groupOwnerUser?.name ?: localUserName,
                deviceAddress = ownerDevice?.deviceMacAddress ?: localDeviceAddress,
                deviceName = ownerDevice?.deviceName ?: "Group Owner"
            )
            return payloadText
        } else {
            payloadText = qrCodeService.createPayloadText(
                userId = localUserId,
                userName = localUserName,
                deviceAddress = localDeviceAddress,
                deviceName = localDeviceName
            )
        }
        Log.d("DiscoverViewModel", "QR Payload text = $payloadText")

        return payloadText
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
            wifiService.connectToPeer(device)
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
        wifiService.leaveGroup()
        _scannedPeers.value = emptyList()
    }
}
