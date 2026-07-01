package com.codingskillshub.bitpigeon.domain.services

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.types.QRCodePayload
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeService @Inject constructor(
    private val wifiCommunicationService: WifiCommunicationService
) {
    private val gson = Gson()

    fun createPayloadText(user: User, device: WifiP2pDevice): String {
        return createPayloadText(
            userId = user.id,
            userName = user.name,
            deviceAddress = device.deviceAddress,
            deviceName = device.deviceName
        )
    }

    fun createPayloadText(
        userId: String,
        userName: String,
        deviceAddress: String,
        deviceName: String
    ): String {
        return gson.toJson(
            QRCodePayload(
                userId = userId,
                userName = userName,
                deviceAddress = deviceAddress,
                deviceName = deviceName
            )
        )
    }

    fun parsePayloadText(qrText: String): QRCodePayload? {
        return try {
            gson.fromJson(qrText, QRCodePayload::class.java)
        } catch (error: Exception) {
            Log.e("QRCodeService", "Failed to parse QR payload: ${error.message}")
            null
        }
    }
}
