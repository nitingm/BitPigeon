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
    val profilePicturePath: String? = null
) {
    @Ignore
    var status: String = "Hey there i'm using BitPigeon"

    constructor(
        id: String,
        name: String,
        deviceAddress: String,
        phoneNumber: String,
        email: String,
        profilePicturePath: String? = null,
        status: String
    ) : this(id, name, deviceAddress, phoneNumber, email, profilePicturePath) {
        this.status = status
    }
}
