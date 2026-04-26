package com.codingskillshub.bitpigeon.domain.entities

import java.io.Serializable

data class Client (
    val deviceName: String,
    val ipAddress: String,
    val isGroupOwner: Boolean,
    val user: User
) : Serializable {}