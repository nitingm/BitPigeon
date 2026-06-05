package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val deviceAddress: String,
    val phoneNumber: String,
    val email: String,
    val profilePicturePath: String = ""
) : Serializable {
    @Ignore
    var status: String = "Hey there i'm using BitPigeon"

    constructor(
        id: String,
        name: String,
        deviceAddress: String,
        phoneNumber: String,
        email: String,
        profilePicturePath: String = "",
        status: String
    ) : this(id, name, deviceAddress, phoneNumber, email, profilePicturePath) {
        this.status = status
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
