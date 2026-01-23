package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

data class MessageData(
    val text: String,
    val attachments: List<String> = emptyList()
)

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey val id: String,
    val chatGroupId: String,
    val senderId: String,
    val data: MessageData,
    val timestamp: String
)

data class ChatMessageUIExtented(
    val message: ChatMessage,
    val userName: String,
    val isSentByMe: Boolean,
    val isDelivered: Boolean,
    val isRead: Boolean
)