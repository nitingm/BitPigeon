package com.codingskillshub.bitpigeon.domain.services

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
    private val clientSocketManager: ClientSocketManager? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onAvailablePeerClientsUpdated: ((List<Client>) -> Unit)? = null

    init {
        clientSocketManager?.onMessageReceived = { message ->
            handleReceivedMessage(message)
        }
    }

    private fun sendRequestMessage(message: ActionMessage) {
        clientScope.launch {
            clientSocketManager?.sendMessage(message)
        }
    }
    private fun handleReceivedMessage(message: ActionMessage) {
        when (message.actionType) {
            "REQUEST_CONNECTION" -> {
                val user = message.data as User

            }
            "AVAILABLE_CLIENTS_UPDATE" -> {
                handleAvailableClientsUpdate(message.data)
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

    fun addUserToGroup(user: User) {

    }

    fun updateGroupInfo() {

    }
    fun sendUserInfoUpdate() {

    }
    fun sendChatMessage(message: ChatMessage) {

    }

    private fun handleAvailableClientsUpdate(data: Any) {
        val clients = data as List<Client>
        onAvailablePeerClientsUpdated?.invoke(clients)
    }
}