package com.codingskillshub.bitpigeon.domain.models

import android.util.Log
import androidx.activity.result.launch
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.common.HashService
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupDb
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupMember
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatGroupDao
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationModel @Inject constructor(
    private val chatModel: ChatModel,
    private val chatGroupDao: ChatGroupDao,
    private val onlineChatService: OnlineChatService,
    private val configurationService: ConfigurationService,
    private val hashService: HashService
) {
    // We create a dedicated scope for the model to keep the flow active
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        modelScope.launch {
            onlineChatService.incomingUsers.collect { user ->
                createDirectChat(user)
            }
        }
    }

    /**
     * Creates a Direct or Personal chat group.
     */
    suspend fun createDirectChat(peerUser: User, isPersonal: Boolean = false) {
        val myId = configurationService.userIdFlow.firstOrNull() ?: return
        // Generate a deterministic ID:
        // For personal chats, it's just based on myId.
        // For direct, it's based on both IDs sorted.
        val groupId = if (isPersonal) {
            hashService.generatePersonalChatId(myId)
        } else {
            hashService.generateDirectGroupId(myId, peerUser.id)
        }

        val groupDb = ChatGroupDb(
            id = groupId,
            name = if (isPersonal) "${peerUser.name} (You)" else peerUser.name,
            type = if (isPersonal) ChatGroupType.PERSONAL else ChatGroupType.DIRECT
        )

        // 1. Insert Group
        chatGroupDao.insertOrUpdateChatGroup(groupDb)

        // 2. Insert Members
        val members = mutableListOf<ChatGroupMember>()
        members.add(ChatGroupMember(id = 0, chatGroupId = groupId, userId = myId))

        if (!isPersonal) {
            members.add(ChatGroupMember(id = 0, chatGroupId = groupId, userId = peerUser.id))
        }

        chatGroupDao.insertMembers(members)
    }

    fun getAllConversations(): Flow<List<ChatGroup>> {
        return chatGroupDao.getAllChatGroups()
            .flatMapLatest { groups ->
                if (groups.isEmpty()) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    // Combine the latest message flows for every group in the list
                    combine(
                        groups.map { chatGroup ->
                            chatModel.getLatestMessageForChatGroup(chatGroup.group.id)
                                .map { latestMsg ->
                                    // Map the base ChatGroup to a new one containing UI metadata
                                    chatGroup.copy(
                                        lastMessage = latestMsg?.data?.text ?: "",
                                        timestamp = latestMsg?.timestamp ?: ""
                                    )
                                }
                        }
                    ) { it.toList() }
                }
            }
    }

    suspend fun createMyPersonalChat() {
        val myId = configurationService.userIdFlow.firstOrNull() ?: return
        val myName = configurationService.userNameFlow.firstOrNull() ?: "Me"
        val selfUser = User(id = myId, name = myName, deviceAddress = "",  "", "")
        createDirectChat(selfUser, isPersonal = true)
        Log.d("ConversationModel", "My Personal Chat created!!!")
    }

    fun getChatGroupById(chatId: String): Flow<ChatGroup?> {
        return chatGroupDao.getChatGroupById(chatId)
    }
}