package com.codingskillshub.bitpigeon.domain.models

import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ConversationModel @Inject constructor(
    private val chatModel: ChatModel
) {
    private val _conversations = MutableStateFlow<List<ChatGroup>>(emptyList())

    val conversations: StateFlow<List<ChatGroup>> = _conversations


//    private fun getConversations(): List<ChatGroup> {
////        return conversations
//    }
}