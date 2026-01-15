package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

enum class ChatGroupType {
    DIRECT,
    GROUP
}

@Entity(tableName = "chat_groups")
data class ChatGroup(
    @PrimaryKey val id: String,
    val name: String,
    val type: ChatGroupType = ChatGroupType.DIRECT,
    @Ignore val lastMessage: String,
    @Ignore val timestamp: String
)
