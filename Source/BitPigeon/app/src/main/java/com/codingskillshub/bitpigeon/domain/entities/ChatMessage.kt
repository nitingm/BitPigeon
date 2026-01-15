package com.codingskillshub.bitpigeon.domain.entities

data class MessageData(
    val text: String,
    val attachments: List<String> = emptyList()
)

data class ChatMessage(
    val id: String,
    val chatGroupId: String,
    val senderId: String,
    val data: MessageData,
    val timestamp: String
)
