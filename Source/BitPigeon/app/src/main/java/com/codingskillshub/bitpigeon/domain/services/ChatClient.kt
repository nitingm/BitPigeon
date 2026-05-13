package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.infrastructure.ClientSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        }
    }

    fun connectToServer(ip: String, port: Int) {
        clientScope.launch {
            clientSocketManager?.connect(ip, port)
            clientSocketManager?.sendMessage(selfUser)
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
    fun sendUserInfoUpdate() {

    }
    fun sendChatMessage(chatMessage: ChatMessage) {
        val message = ActionMessage("SEND_CHAT_MESSAGE", chatMessage)
        sendRequestMessage(message)
    }

    fun syncChatGroupsWithServer(chatGroups: List<ChatGroup>) {
        val message = ActionMessage("SYNC_ONLINE_CHAT_GROUPS", chatGroups)
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

    private fun handleServerDisconnection() {
        Log.d("ChatClient", "Server disconnected")
        onServerDisconnection?.invoke()
    }
}