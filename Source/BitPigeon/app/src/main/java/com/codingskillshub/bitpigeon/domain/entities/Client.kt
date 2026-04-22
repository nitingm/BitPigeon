package com.codingskillshub.bitpigeon.domain.entities

data class Client (
    val deviceName: String,
    val ipAddress: String,
    val isGroupOwner: Boolean,
    val user: User
)