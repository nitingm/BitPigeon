package com.codingskillshub.bitpigeon.domain.models

import android.R.id.message
import android.util.Log
import androidx.compose.ui.autofill.ContentDataType
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.common.HashService
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatModel @Inject constructor(
    private val chatDao: ChatDao,
    val onlineChatService: OnlineChatService,
    private val hashService: HashService,
    private val appSystemModel: AppSystemModel,
    private val userDao: UserDao,
    private val configurationService: ConfigurationService
) {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _activeChatGroup = MutableStateFlow<ChatGroup?>(null)

    /**
     * Exposes a reactive list of messages for the currently active chat group.
     * flatMapLatest ensures that as soon as the activeChatGroupId changes,
     * we stop observing the old chat and start observing the new one in the DB.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: kotlinx.coroutines.flow.StateFlow<List<ChatMessage>> = _activeChatGroup
        .flatMapLatest { chatGroup ->
            if (chatGroup == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                chatDao.getAllMessagesForChatGroup(chatGroup.group.id)
            }
        }
        .stateIn(
            // We use the application-level scope usually or define one in the model
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()),
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        serviceScope.launch {
            onlineChatService.incomingMessages.collect { message ->
                chatDao.insertMessage(message)
            }
        }
    }

    /**
     * Updates the chat group currently being viewed.
     * This triggers the [messages] StateFlow to switch to the new group.
     */
    fun setActiveChatGroup(chatGroup: ChatGroup?) {
        _activeChatGroup.value = chatGroup
    }

    suspend fun sendMessage(messageText: String, chatId: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val uniqueId = hashService.generateMessageId(messageText, appSystemModel.getMyUserId(),timestamp)

        val message = ChatMessage(
            id = uniqueId,
            chatGroupId = chatId,
            senderId = appSystemModel.getMyUserId(),
            data = MessageData(messageText),
            timestamp = timestamp
        )

        Log.d("ChatModel","Send message: $message")

        if (!isPersonalChat(_activeChatGroup.value)) {
//            onlineChatService.sendMessageOnline(message)
        }

        chatDao.insertMessage(message)
    }

    fun getLatestMessageForChatGroup(chatGroupId: String): Flow<ChatMessage?> {
        return chatDao.getLatestMessageForChatGroup(chatGroupId)
    }

    /**
     * Returns a Flow of the List of Users who are members of the currently active chat group.
     * It updates automatically whenever the activeChatGroup changes or user data in DB changes.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getMembersForActiveChatGroup(): Flow<List<User>> {
        return _activeChatGroup.flatMapLatest { chatGroup ->
            if (chatGroup == null || chatGroup.members.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                // Map each member ID to a Flow<User?> from the DAO
                val userFlows = chatGroup.members.map { member ->
                    userDao.getUserById(member.userId)
                }

                // Combine all individual user flows into one flow of list
                combine(userFlows) { users ->
                    // Filter out nulls in case a user exists in members but not in users table
                    users.filterNotNull()
                }
            }
        }
    }

    private fun isPersonalChat(chatGroup: ChatGroup?): Boolean {
        return chatGroup?.group?.type == ChatGroupType.PERSONAL
    }
}