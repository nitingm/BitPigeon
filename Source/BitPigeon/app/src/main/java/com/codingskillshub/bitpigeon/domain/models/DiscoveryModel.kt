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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DiscoveryModel @Inject constructor(
    private val userDao: UserDao,
    private val qrCodeService: QRCodeService,
    private val onlineChatService: OnlineChatService,
    private val wifiService: WifiCommunicationService,
    private val configurationService: ConfigurationService
){

    private val _nearbyPeers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())
    val nearbyPeers: StateFlow<List<WifiDirectPeer>> = _nearbyPeers

    val discoveredUsers: StateFlow<Map<String, Pair<User, WifiP2pDevice>>> = wifiService.discoveredUsers


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
            val ownerDevice = discoveredUsers.value.values.firstOrNull { (user, _) ->
                user.id == groupOwnerUser?.id
            }?.second

            payloadText = qrCodeService.createPayloadText(
                userId = groupOwnerUser?.id ?: localUserId,
                userName = groupOwnerUser?.name ?: localUserName,
                deviceAddress = ownerDevice?.deviceAddress ?: localDeviceAddress,
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

    fun findDeviceForPayload(payload: QRCodePayload): WifiP2pDevice? {
        val discoveredPeers = wifiService.getDiscoveredPeers()
        return discoveredPeers.find { it.deviceAddress == payload.deviceAddress || it.deviceName == payload.deviceName }
    }

    fun connectToPeerFromPayloadText(qrText: String): Boolean {
        val payload = qrCodeService.parsePayloadText(qrText) ?: return false
        val device = findDeviceForPayload(payload)
        Log.d("QRCodeService", "qrText = $qrText, payload = $payload")
        if (device != null) {
            wifiService.connectToPeer(device)
        } else {
            Log.d("QRCodeService", "Device Not found in discovered peers")
        }

        return true
    }
}