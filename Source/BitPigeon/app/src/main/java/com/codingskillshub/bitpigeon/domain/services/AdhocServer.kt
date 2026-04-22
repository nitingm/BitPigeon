package com.codingskillshub.bitpigeon.domain.services

import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.infrastructure.ServerSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

import javax.inject.Singleton

@Singleton
class AdhocServer {
    private var serverSocketManager: ServerSocketManager? = null
    // Thread-safe list for clients
    private val clients = CopyOnWriteArrayList<Client>()
    private val chatRooms: MutableMap<String, MutableList<Client>> = mutableMapOf()

    // Dedicated background thread for this server's logic
    private val serverDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val serverScope = CoroutineScope(serverDispatcher + SupervisorJob())

    fun startServer(port: Int) {
        serverScope.launch {
            serverSocketManager = ServerSocketManager(port).apply {
                onMessageReceived = { message -> handleClientRequest(message) }
                onClientConnected = { client -> handleClientConnection(client) }
                onClientDisconnected = { client -> handleClientDisconnection(client) }
                start()
            }
        }
    }

    fun stopServer()  {
        serverScope.launch {
            serverSocketManager?.stop()
            clients.clear()
            chatRooms.clear()
        }
        serverDispatcher.close()
    }

    private fun handleClientConnection(client: Client) {
        clients.add(client)
        serverScope.launch {
            sendAvailableClients(clients)
        }
    }

    private fun handleClientDisconnection(client: Client) {
        clients.remove(client)
        // Also remove from any chat rooms
        chatRooms.forEach { (roomId, members) ->
            members.remove(client)
        }
        serverScope.launch {
            sendAvailableClients(clients)
        }
    }

    private fun handleClientRequest(message: ActionMessage) {
        when (message.actionType) {
            "REQUEST_CONNECTION" -> {
                val user = message.data as User
                relayPrivateConnectionRequest(user)
            }

        }
    }

    fun relayPrivateConnectionRequest(user: User) {

    }

    fun relayGroupCreationRequest(group: ChatGroup) {

    }
    fun sendGroupInfoUpdate() {

    }
    fun sendGroupEvent() {

    }
    fun relayUserInfoUpdate() {

    }
    fun relayChatMessage(message: ChatMessage) {

    }
    private suspend fun sendAvailableClients(clients: List<Client>) {
        val message = ActionMessage(
            actionType = "AVAILABLE_CLIENTS_UPDATE",
            data = clients
        )
        for (client: Client in clients) {
            serverSocketManager?.sendMessageToClient(message, client.user.id)
        }
    }
}