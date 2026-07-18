package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.types.WifiDirectPeer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    private val _wifiGroupPeers = MutableStateFlow<List<WifiDirectPeer>>(emptyList())
    val wifiGroupPeers: StateFlow<List<WifiDirectPeer>> = _wifiGroupPeers.asStateFlow()

    private val _incomingPeerClients = MutableSharedFlow<ChatGroup>()
    val incomingPeerClients = _incomingPeerClients.asSharedFlow()

    private val _incomingNewChatGroup = MutableSharedFlow<ChatGroup>()
    val incomingNewChatGroup = _incomingNewChatGroup.asSharedFlow()

    private val _incomingGetProfilePictureRequest = MutableSharedFlow<AppFileRequest>()
    val incomingGetProfilePictureRequest = _incomingGetProfilePictureRequest.asSharedFlow()

    private var adhocServer: AdhocServer? = null
    private var chatClient: ChatClient? = null

    companion object {
        private const val PORT = 8888
    }

    init {
        serviceScope.launch {
            val myId = configurationService.userIdFlow.firstOrNull() ?: ""
            val myName = configurationService.userNameFlow.firstOrNull() ?: ""
            val user = userDao.getUserById(myId).firstOrNull() ?: User(id = myId, name = myName, deviceAddress = "",  "", "")
            wifiCommunicationService.setUserDetails(user)
            wifiCommunicationService.connectionInfo.collectLatest { info ->
                if (info == null) {
                    // Momentary glitches in P2P connection can cause info to be null.
                    // Delaying stopAll allows the system a chance to recover without tearing down the app's session immediately.
                    delay(2000)
                    stopAll()
                } else {
                    if (info.isGroupOwner) {
                        startServer()
//                        wifiCommunicationService.stopServiceAdvertising()
//                        wifiCommunicationService.onServiceAdvertisingChanged = { isAdvertising ->
//                            if (!isAdvertising) {
//                                wifiCommunicationService.startServiceAdvertising(_selfUser, true)
//                            }
//                        }
                        val host = info.groupOwnerAddress?.hostAddress
                        if (host != null) {
                            startClient(host)
                        }
                    } else {
                        val host = info.groupOwnerAddress?.hostAddress
                        if (host != null) {
                            startClient(host)
//                            wifiCommunicationService.stopServiceAdvertising()
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

    private suspend fun startClient(host: String) {
        if (chatClient == null) {
            Log.d("OnlineChatService", "Starting ChatClient connecting to $host:$PORT")

            val myId = configurationService.userIdFlow.firstOrNull() ?: ""
            val myName = configurationService.userNameFlow.firstOrNull() ?: ""
            val user = userDao.getUserById(myId).firstOrNull() ?: User(id = myId, name = myName, deviceAddress = "",  "", "")

            chatClient = ChatClient(user).apply {
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
                    wifiCommunicationService.leaveGroup()
                    chatClient = null
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
                onWifiGroupPeersUpdate = { peers ->
                    _wifiGroupPeers.value = peers
                }
                onConnectedToServer = {
                    serviceScope.launch {
                        sendLocalWifiDirectPeerInfo()
                    }
                }
            }
            chatClient?.connectToServer(host, PORT)
        }
    }

    private suspend fun sendLocalWifiDirectPeerInfo() {
        delay(5000)
        val localDeviceInfo = wifiCommunicationService.getLocalDeviceInfo()
        val myId = configurationService.userIdFlow.firstOrNull() ?: ""
        val myName = configurationService.userNameFlow.firstOrNull() ?: ""
        val connectionInfo = wifiCommunicationService.connectionInfo.value
        if (localDeviceInfo != null && connectionInfo != null) {
            val myDevice = WifiDirectPeer(
                deviceName = localDeviceInfo.deviceName,
                deviceMacAddress = localDeviceInfo.deviceAddress,
                isGroupOwner = connectionInfo.isGroupOwner,
                userId = myId,
                userName = myName
            )
            chatClient?.syncWifiDirectPeerInfo(myDevice)
        } else {
            wifiCommunicationService.updateLocalDeviceInfo()
            sendLocalWifiDirectPeerInfo()
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
