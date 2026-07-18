package com.codingskillshub.bitpigeon.domain.types

import java.io.Serializable

data class WifiDirectPeer(
    val deviceName: String,
    val deviceMacAddress: String,
    val isGroupOwner: Boolean = false,
    val userId: String = "",
    val userName: String = ""
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
