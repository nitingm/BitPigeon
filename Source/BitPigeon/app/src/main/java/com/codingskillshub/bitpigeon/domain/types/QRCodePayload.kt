package com.codingskillshub.bitpigeon.domain.types

data class QRCodePayload(
    val userId: String = "",
    val userName: String = "",
    val deviceAddress: String = "",
    val deviceName: String = ""
)