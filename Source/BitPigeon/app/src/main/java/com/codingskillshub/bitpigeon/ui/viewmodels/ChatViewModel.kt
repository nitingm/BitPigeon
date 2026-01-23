package com.codingskillshub.bitpigeon.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.ChatMessageUIExtented
import com.codingskillshub.bitpigeon.domain.models.ChatModel
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.firstOrNull
import kotlin.text.format

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatModel: ChatModel,
    val conversationModel: ConversationModel,
    val configurationService: ConfigurationService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Extract the chatId from the SavedStateHandle
    // The key "chatId" must match the argument name used in your NavHost
    val chatId: String = savedStateHandle["chatId"] ?: "unknown_user"

    /**
     * Combines raw messages, group members, and current user ID
     * to produce the UI-extended message list.
     */
    val messages: StateFlow<List<ChatMessageUIExtented>> = combine(
        chatModel.messages,
        chatModel.getMembersForActiveChatGroup(),
        configurationService.userIdFlow
    ) { rawMessages, members, myId ->
        rawMessages.map { message ->
            val sender = members.find { it.id == message.senderId }

            ChatMessageUIExtented(
                message = message,
                userName = sender?.name ?: "Unknown",
                isSentByMe = message.senderId == myId,
                isDelivered = false, // Kept blank/false
                isRead = false       // Kept blank/false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatGroup: StateFlow<ChatGroup?> = conversationModel.getChatGroupById(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // You can now use chatId to load data
        println("Loading chat for ID: $chatId")
        // Observe the chatGroup flow and update the active chat in ChatModel
        chatGroup
            .onEach { group ->
                if (group != null) {
                    chatModel.setActiveChatGroup(group)
                    println("Active chat set for: ${group.group.name}")
                }
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(messageText: String) {
        viewModelScope.launch {
            chatModel.sendMessage(messageText, chatId)
        }
    }
}