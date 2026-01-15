package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val deviceAddress: String,
    val phoneNumber: String,
    val email: String,
    val profilePicturePath: String? = null,
    @Ignore val status: String = "Hey there i'm using BitPigeon"
)
