package com.codingskillshub.bitpigeon.domain.types

data class WifiDirectPeer(
    val deviceName: String,
    val deviceMacAddress: String,
    val isGroupOwner: Boolean = false,
    val userId: String = "",
    val userName: String = ""
)
