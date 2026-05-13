package com.codingskillshub.bitpigeon.domain.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

data class MessageData(
    val text: String,
    val attachmentIds: List<String> = emptyList()
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey val id: String,
    val chatGroupId: String,
    val senderId: String,
    val data: MessageData,
    val timestamp: String
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class ChatMessageUIExtended(
    val message: ChatMessage,
    val userName: String,
    val isSentByMe: Boolean,
    val isDelivered: Boolean,
    val isRead: Boolean,
    val attachmentPreviewData: List<AttachmentPreviewData> = emptyList()
)