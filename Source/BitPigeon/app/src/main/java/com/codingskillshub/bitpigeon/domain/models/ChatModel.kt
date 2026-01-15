package com.codingskillshub.bitpigeon.domain.models

import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import javax.inject.Inject


class ChatModel @Inject constructor(
    val onlineChatService: OnlineChatService
) {

    fun sendMessage(message: ChatMessage) {

        onlineChatService.sendMessageOnline(message)
    }
}