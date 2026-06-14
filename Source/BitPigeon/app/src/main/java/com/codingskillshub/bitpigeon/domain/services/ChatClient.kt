package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.AppFile
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.infrastructure.ClientSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ChatClient(
    private val selfUser: User
) {
    private var clientSocketManager: ClientSocketManager? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    var onAvailablePeerClientsUpdated: ((List<Client>) -> Unit)? = null
    var onCreateDirectChat: ((ChatGroup) -> Unit)? = null
    var onChatMessageReceived: ((ChatMessage) -> Unit)? = null
    var onServerDisconnection: (() -> Unit)? = null
    var onUserInfoReceived: ((User) -> Unit)? = null

    var onGetProfilePictureRequest: ((AppFileRequest) -> Unit)? = null

    init {
        clientSocketManager = ClientSocketManager()
        clientSocketManager?.onMessageReceived = { message ->
            handleReceivedMessage(message)
        }
        clientSocketManager?.onDisconnected = {
            handleServerDisconnection()
        }
    }

    private fun sendRequestMessage(message: ActionMessage) {
        Log.d("ChatClient","Sending message: $message")
        clientScope.launch {
            clientSocketManager?.sendMessage(message)
        }
    }
    private fun handleReceivedMessage(message: ActionMessage) {
        when (message.actionType) {
            "CREATE_DIRECT_CHAT" -> {
                val group = message.data as ChatGroup
                handleCreateDirectChatRequest(group)
            }
            "AVAILABLE_CLIENTS_UPDATE" -> {
                handleAvailableClientsUpdate(message.data)
            }
            "SEND_CHAT_MESSAGE" -> {
                val chatMessage = message.data as ChatMessage
                handleIncomingChatMessage(chatMessage)
            }
            "SEND_USER_INFO" -> {
                val user = message.data as User
                handleUserInfoUpdate(user)
            }
            "GET_PROFILE_PICTURE" -> {
                val appFile = message.data as AppFileRequest
                handleGetProfilePictureRequest(appFile)
            }
        }
    }

    fun connectToServer(ip: String, port: Int) {
        clientScope.launch {
            var connected = false
            var attempts = 0
            val maxAttempts = 5
            
            while (!connected && attempts < maxAttempts) {
                try {
                    clientSocketManager?.connect(ip, port)
                    clientSocketManager?.sendMessage(selfUser)
                    connected = true
                    Log.d("ChatClient", "Successfully connected to server at $ip:$port")
                } catch (e: Exception) {
                    attempts++
                    Log.e("ChatClient", "Connection attempt $attempts failed to $ip:$port: ${e.message}")
                    if (attempts < maxAttempts) {
                        delay(1000L * attempts)
                    } else {
                        Log.e("ChatClient", "Max connection attempts reached. Giving up.")
                        handleServerDisconnection()
                    }
                }
            }
        }
    }

    fun disconnectFromServer() {
        clientSocketManager?.disconnect()
    }

    fun createGroup(group: ChatGroup, users: List<User> ) {

    }

    fun createDirectChat(group: ChatGroup) {
        val message = ActionMessage("CREATE_DIRECT_CHAT", group)
        sendRequestMessage(message)
    }

    fun addUserToGroup(user: User) {

    }

    fun updateGroupInfo() {

    }

    fun sendUserInfoUpdate(user: User) {
        val message = ActionMessage("SEND_USER_INFO", user)
        sendRequestMessage(message)
    }

    fun sendChatMessage(chatMessage: ChatMessage) {
        val message = ActionMessage("SEND_CHAT_MESSAGE", chatMessage)
        sendRequestMessage(message)
    }

    fun syncChatGroupsWithServer(chatGroups: List<ChatGroup>) {
        val message = ActionMessage("SYNC_ONLINE_CHAT_GROUPS", chatGroups)
        sendRequestMessage(message)
    }

    fun getProfilePicture(appFileRequest: AppFileRequest) {
        val message = ActionMessage("GET_PROFILE_PICTURE", appFileRequest)
        sendRequestMessage(message)
    }

    private fun handleAvailableClientsUpdate(data: Any) {
        val clients = data as List<Client>
        onAvailablePeerClientsUpdated?.invoke(clients)
    }

    private fun handleCreateDirectChatRequest(group: ChatGroup) {
        onCreateDirectChat?.invoke(group)
    }

    private fun handleIncomingChatMessage(chatMessage: ChatMessage) {
        onChatMessageReceived?.invoke(chatMessage)
    }

    private fun handleUserInfoUpdate(user: User) {
        onUserInfoReceived?.invoke(user)
    }

    private fun handleGetProfilePictureRequest(appFileRequest: AppFileRequest) {
        onGetProfilePictureRequest?.invoke(appFileRequest)
    }

    private fun handleServerDisconnection() {
        Log.d("ChatClient", "Server disconnected")
        onServerDisconnection?.invoke()
    }
}
