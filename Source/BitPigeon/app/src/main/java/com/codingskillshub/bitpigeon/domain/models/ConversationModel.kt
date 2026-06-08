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
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.map

@Singleton
class ConversationModel @Inject constructor(
    private val chatModel: ChatModel,
    private val chatGroupDao: ChatGroupDao,
    private val userDao: UserDao,
    private val onlineChatService: OnlineChatService,
    private val configurationService: ConfigurationService,
    private val appSystemModel: AppSystemModel,
    private val hashService: HashService
) {
    // We create a dedicated scope for the model to keep the flow active
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _usersOnline = MutableSharedFlow<List<User>>()
    val usersOnline = _usersOnline.asSharedFlow()

    private val _onlineGroups = MutableStateFlow<List<ChatGroup>>(emptyList())
    val onlineGroups: StateFlow<List<ChatGroup>> = _onlineGroups.asStateFlow()

    init {
        modelScope.launch {
            onlineChatService.incomingNewChatGroup.collect { chatGroup ->
                try {
                    handleIncomingNewChatGroup(chatGroup)
                } catch (e: Exception) {
                    Log.e("ConversationModel", "Error handling incoming chat group: ${e.message}", e)
                }
            }
        }
        modelScope.launch {
            onlineChatService.availablePeerClients.collect { clients ->
                Log.d("ConversationModel", "🔥 Processing ${clients.size} available clients")
                clients.forEach { client ->
                    handleUserInfoUpdate(client.user)
                }
                val users = clients.map { it.user }

                Log.d("ConversationModel", "✅ Processed ${users.size} users - ${users.map { it.name }}")
                _usersOnline.emit(users)
            }
        }
        modelScope.launch {
            usersOnline.collect { onlineUsers ->
                try {
                    Log.d("ConversationModel", "🔄 Syncing online groups for ${onlineUsers.size} users")
                    syncOnlineChatGroups(onlineUsers)
                } catch (e: Exception) {
                    Log.e("ConversationModel", "Error syncing online groups: ${e.message}", e)
                }
            }
        }
        modelScope.launch {
            onlineChatService.incomingUsers.collect { user ->
                handleUserInfoUpdate(user)
            }
        }
    }

    /**
     * Creates a Direct or Personal chat group.
     */
    suspend fun createDirectChat(peerUser: User, isPersonal: Boolean = false) : String {
        val myId = appSystemModel.getMyUserId()
        // Generate a deterministic ID:
        // For personal chats, it's just based on myId.
        // For direct, it's based on both IDs sorted.
        val groupId = if (isPersonal) {
            hashService.generatePersonalChatId(myId)
        } else {
            hashService.generateDirectGroupId(myId, peerUser.id)
        }

        userDao.insertOrUpdateUser(peerUser)

        // Check if the groupId already exists and return if true
        val existingGroup = chatGroupDao.getChatGroupById(groupId).firstOrNull()
        if (existingGroup != null) {
            return existingGroup.group.id
        }

        val groupDb = ChatGroupDb(
            id = groupId,
            name = peerUser.id,
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

        if (!isPersonal) {
            onlineChatService.sendCreateDirectChatRequest(ChatGroup(groupDb.copy(name = myId),members))
        }

        return groupDb.id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    suspend fun createMyPersonalChat() {
        val myId = configurationService.userIdFlow.firstOrNull() ?: return
        val myName = configurationService.userNameFlow.firstOrNull() ?: "Me"
        val selfUser = User(id = myId, name = myName, deviceAddress = "",  "", "")
        createDirectChat(selfUser, isPersonal = true)
        Log.i("ConversationModel", "My Personal Chat created!!!")
    }

    fun getChatGroupById(chatId: String): Flow<ChatGroup?> {
        return chatGroupDao.getChatGroupById(chatId)
    }

    fun getUserById(userId: String): Flow<User?> {
        return userDao.getUserById(userId)
    }

    private suspend fun handleIncomingNewChatGroup(chatGroup: ChatGroup) {
        val existingGroup = chatGroupDao.getChatGroupById(chatGroup.group.id).firstOrNull()
        if (existingGroup != null) {
            return
        }
        chatGroupDao.insertOrUpdateChatGroup(chatGroup.group)
        chatGroupDao.insertMembers(chatGroup.members)
    }

    // Any group containing more than 1 member online is considered an online group
    suspend fun syncOnlineChatGroups(onlineUsers: List<User>) {
        val onlineUserIds = onlineUsers.map { it.id }.toSet()

        // Get all chat groups
        val allGroups = chatGroupDao.getAllChatGroups().firstOrNull() ?: emptyList()

        // Filter groups that contain at least 1 online friend
        val groups =  allGroups.filter { group ->
            val onlineMemberCount = group.members.count { member ->
                member.userId in onlineUserIds
            }
            onlineMemberCount > 0
        }
        _onlineGroups.value = groups

        onlineChatService.syncOnlineChatGroups(groups)
        Log.i("ConversationModel", "Synced online chat groups: $groups")
    }

    private suspend fun handleUserInfoUpdate(user: User) {
        Log.d("ConversationModel", "💾 Saving/updating user to DB: ${user.id} - ${user.name}")
        val existingUser = userDao.getUserById(user.id).firstOrNull()
        var updatedUser: User = user
        if (existingUser != null) {
            val existingProfilePicture = existingUser.profilePicture
            val newProfilePicture = user.profilePicture
            if (existingProfilePicture != newProfilePicture) {
                appSystemModel.getProfilePicture(newProfilePicture, user.id)
                updatedUser = user.copy(profilePicture = existingProfilePicture)
            }
        } else {
            appSystemModel.getProfilePicture(user.profilePicture, user.id)
            updatedUser = user.copy(profilePicture = "")
        }
        userDao.insertOrUpdateUser(updatedUser)
    }
}