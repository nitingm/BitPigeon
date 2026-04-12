package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.infrastructure.ClientSocketManager
import com.codingskillshub.bitpigeon.infrastructure.ServerSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineChatService @Inject constructor(
    private val wifiCommunicationService: WifiCommunicationService,
    private val configurationService: ConfigurationService
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _incomingMessages = MutableSharedFlow<ChatMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _incomingUsers = MutableSharedFlow<User>()
    val incomingUsers = _incomingUsers.asSharedFlow()

    private var serverManager: ServerSocketManager? = null
    private var clientManager: ClientSocketManager? = null

    private var _selfUser: User = User("", "", "", "", "")

    companion object {
        private const val PORT = 8888
    }

    init {
        serviceScope.launch {
            val myId = configurationService.userIdFlow.firstOrNull() ?: ""
            val myName = configurationService.userNameFlow.firstOrNull() ?: "Me"
            _selfUser = User(id = myId, name = myName, deviceAddress = "",  "", "")
            wifiCommunicationService.connectionInfo.collectLatest { info ->
                if (info == null) {
                    stopAll()
                } else {
                    if (info.isGroupOwner) {
                        startServer()

                    } else {
                        val host = info.groupOwnerAddress?.hostAddress
                        if (host != null) {
                            startClient(host)
                        }
                    }
                }
            }
        }
    }

    private fun startServer() {
        if (serverManager == null) {
            Log.d("OnlineChatService", "Starting ServerSocketManager on port $PORT")
            // Ensure client is disconnected if we are becoming group owner
            clientManager?.disconnect()
            clientManager = null



            serverManager = ServerSocketManager(PORT)
            serverManager?.onMessageReceived = { message ->
                serviceScope.launch {
                    _incomingMessages.emit(message)
                }
            }
            serverManager?.onUserInfoReceived = { user ->
                serviceScope.launch {
                    _incomingUsers.emit(user)
                }
                sendUserInfo(_selfUser)
            }
            serverManager?.start()
        }
    }

    private fun startClient(host: String) {
        if (clientManager == null) {
            Log.d("OnlineChatService", "Starting ClientSocketManager connecting to $host:$PORT")
            // Ensure server is stopped if we are joining as client
            serverManager?.stop()
            serverManager = null

            clientManager = ClientSocketManager(host, PORT)
            clientManager?.onMessageReceived = { message ->
                serviceScope.launch {
                    _incomingMessages.emit(message)
                }
            }
            clientManager?.onUserInfoReceived = { user ->
                serviceScope.launch {
                    _incomingUsers.emit(user)
                }
            }
            
            serviceScope.launch {
                try {
                    clientManager?.connect(_selfUser)
                } catch (e: Exception) {
                    Log.e("OnlineChatService", "Failed to connect to group owner: ${e.message}")
                    clientManager = null
                }
            }
        }
    }

    private fun stopAll() {
        Log.d("OnlineChatService", "Stopping all socket managers")
        serverManager?.stop()
        serverManager = null
        clientManager?.disconnect()
        clientManager = null
    }

    fun sendMessageOnline(message: ChatMessage) {
        serviceScope.launch {
            if (serverManager != null) {
                Log.d("OnlineChatService", "Broadcasting message via ServerSocketManager")
                serverManager?.sendMessage(message)
            } else if (clientManager != null) {
                Log.d("OnlineChatService", "Sending message via ClientSocketManager")
                clientManager?.sendMessage(message)
            } else {
                Log.w("OnlineChatService", "No active connection to send message")
            }
        }
    }

    private fun sendUserInfo(user: User) {
        serviceScope.launch {
            if (serverManager != null) {
                Log.d("OnlineChatService", "Broadcasting message via ServerSocketManager")
                serverManager?.sendUserInfo(user)
            } else if (clientManager != null) {
                Log.d("OnlineChatService", "Sending message via ClientSocketManager")
                clientManager?.sendUserInfo(user)
            } else {
                Log.w("OnlineChatService", "No active connection to send message")
            }
        }
    }
}
