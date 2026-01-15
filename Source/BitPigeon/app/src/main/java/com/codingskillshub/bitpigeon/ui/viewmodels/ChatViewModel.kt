package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.models.ChatModel
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatModel: ChatModel,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Extract the chatId from the SavedStateHandle
    // The key "chatId" must match the argument name used in your NavHost
    val chatId: String = savedStateHandle["userId"] ?: "unknown_user"

    init {
        // You can now use chatId to load data
        println("Loading chat for ID: $chatId")
    }

    fun sendMessage(messageText: String) {
        val messageData = MessageData(
            messageText
        )
        val message = ChatMessage(
            "0",
            chatId,
            "nitin",
            messageData,
            "12:10"
        )
        chatModel.sendMessage(message)
    }
}