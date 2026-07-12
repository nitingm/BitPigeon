package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.infrastructure.ClientSocketManager
import com.codingskillshub.bitpigeon.infrastructure.ServerSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineChatService @Inject constructor(
    private val userDao: UserDao,
    private val wifiCommunicationService: WifiCommunicationService,
    private val configurationService: ConfigurationService
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _incomingMessages = MutableSharedFlow<ChatMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()
    private val _incomingUsers = MutableSharedFlow<User>()
    val incomingUsers = _incomingUsers.asSharedFlow()
    private val _availablePeerClients = MutableStateFlow<List<Client>>(emptyList())
    val availablePeerClients: StateFlow<List<Client>> = _availablePeerClients.asStateFlow()

    private val _incomingPeerClients = MutableSharedFlow<ChatGroup>()
    val incomingPeerClients = _incomingPeerClients.asSharedFlow()

    private val _incomingNewChatGroup = MutableSharedFlow<ChatGroup>()
    val incomingNewChatGroup = _incomingNewChatGroup.asSharedFlow()

    private val _incomingGetProfilePictureRequest = MutableSharedFlow<AppFileRequest>()
    val incomingGetProfilePictureRequest = _incomingGetProfilePictureRequest.asSharedFlow()

    private var adhocServer: AdhocServer? = null
    private var chatClient: ChatClient? = null

    private var _selfUser: User = User("", "", "", "", "")

    companion object {
        private const val PORT = 8888
    }

    init {
        serviceScope.launch {
            val myId = configurationService.userIdFlow.firstOrNull() ?: ""
//            val myName = configurationService.userNameFlow.firstOrNull() ?: "Me"
//            _selfUser = User(id = myId, name = myName, deviceAddress = "",  "", "")
            _selfUser = userDao.getUserById(myId).firstOrNull() ?: User(id = myId, name = "Me", deviceAddress = "",  "", "")
//            wifiCommunicationService.startServiceAdvertising(_selfUser)
            wifiCommunicationService.setUserDetails(_selfUser)
            wifiCommunicationService.connectionInfo.collectLatest { info ->
                if (info == null) {
                    stopAll()
                } else {
                    if (info.isGroupOwner) {
                        startServer()
                        wifiCommunicationService.stopServiceAdvertising()
                        wifiCommunicationService.onServiceAdvertisingChanged = { isAdvertising ->
                            if (!isAdvertising) {
                                wifiCommunicationService.startServiceAdvertising(_selfUser, true)
                            }
                        }
                        val host = info.groupOwnerAddress?.hostAddress
                        if (host != null) {
                            startClient(host)
                        }
                    } else {
                        val host = info.groupOwnerAddress?.hostAddress
                        if (host != null) {
                            startClient(host)
                            wifiCommunicationService.stopServiceAdvertising()
                        }
                    }
                }
            }
        }
    }

    fun sendCreateDirectChatRequest(chatGroup: ChatGroup) {
        chatClient?.createDirectChat(chatGroup)
    }

    fun sendUserInfoUpdate(user: User) {
        chatClient?.sendUserInfoUpdate(user)
    }

    private fun startServer() {
        if (adhocServer == null) {
            Log.d("OnlineChatService", "Starting AdhocServer on port $PORT")
            adhocServer = AdhocServer()
            adhocServer?.startServer(PORT)
        }
    }

    private fun startClient(host: String) {
        if (chatClient == null) {
            Log.d("OnlineChatService", "Starting ChatClient connecting to $host:$PORT")

            chatClient = ChatClient(_selfUser).apply {
                onAvailablePeerClientsUpdated = { clients ->
                    _availablePeerClients.value = clients
                }
                onChatMessageReceived = { message ->
                    serviceScope.launch {
                        _incomingMessages.emit(message)
                    }
                }
                onCreateDirectChat = { group ->
                    serviceScope.launch {
                        _incomingNewChatGroup.emit(group)
                    }
                }
                onServerDisconnection = {
                    _availablePeerClients.value = emptyList()
                    chatClient = null
                    wifiCommunicationService.startServiceAdvertising(_selfUser)
                }
                onUserInfoReceived = { user ->
                    serviceScope.launch {
                        _incomingUsers.emit(user)
                    }
                }
                onGetProfilePictureRequest = { appFileRequest ->
                    serviceScope.launch {
                        _incomingGetProfilePictureRequest.emit(appFileRequest)
                    }
                }
            }
            chatClient?.connectToServer(host, PORT)
        }
    }

    fun sendMessageOnline(chatMessage: ChatMessage) {
        chatClient?.sendChatMessage(chatMessage)
    }

    fun syncOnlineChatGroups(groups: List<ChatGroup>) {
        chatClient?.syncChatGroupsWithServer(groups)
    }

    fun sendGetProfilePictureRequest(appFileRequest: AppFileRequest) {
        chatClient?.getProfilePicture(appFileRequest)
    }

    fun getPeerClientById(clientId: String): Client? {
        return availablePeerClients.value.find { it.user.id == clientId }
    }

    fun disconnectFromGroup() {
        chatClient?.disconnectFromServer()
        chatClient = null
    }

    fun destroyGroup() {
        stopAll()
    }

    private fun stopAll() {
        Log.d("OnlineChatService", "Stopping all connection")
        adhocServer?.stopServer()
        adhocServer = null
        chatClient?.disconnectFromServer()
        chatClient = null
    }
}
