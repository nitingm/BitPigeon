package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.infrastructure.ServerSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

import javax.inject.Singleton

@Singleton
class AdhocServer {
    private var serverSocketManager: ServerSocketManager? = null
    // Thread-safe list for clients
    private val clients = CopyOnWriteArrayList<Client>()
    private val chatRooms: MutableMap<String, MutableList<Client>> = mutableMapOf()

    private val chatGroups: MutableList<ChatGroup> = mutableListOf()

    // Mutex to synchronize access to clients list
    private val clientsMutex = Mutex()

    // ...existing code...
    private val serverDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val serverScope = CoroutineScope(serverDispatcher + SupervisorJob())

    fun startServer(port: Int) {
        serverScope.launch {
            serverSocketManager = ServerSocketManager(port, "CSS").apply {
                onMessageReceived = { message, clientId -> handleClientRequest(message, clientId) }
                onClientConnected = { client -> handleClientConnection(client) }
                onClientDisconnected = { clientId -> handleClientDisconnection(clientId) }
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
        serverScope.launch {
            clientsMutex.withLock {
                clients.add(client)
                Log.d("AdhocServer", "Client connected: ${client.user}")
                sendAvailableClientsLocked(clients.toList())
            }
        }
    }

    private fun handleClientDisconnection(clientId: String) {
        serverScope.launch {
            clientsMutex.withLock {
                val disconnectedClient = clients.find { it.user.id == clientId }
                clients.remove(disconnectedClient)
                Log.d("AdhocServer", "Client disconnected: ${disconnectedClient?.user}")
                // Also remove from any chat rooms
                chatRooms.forEach { (_, members) ->
                    members.remove(disconnectedClient)
                }
                sendAvailableClientsLocked(clients.toList())
            }
        }
    }

    private fun handleClientRequest(message: ActionMessage, clientId: String) {
        when (message.actionType) {
            "CREATE_DIRECT_CHAT" -> {
                relayDirectChatCreationRequest(message, clientId)
            }
            "SYNC_ONLINE_CHAT_GROUPS" -> {
                @Suppress("UNCHECKED_CAST")
                val groups = message.data as List<ChatGroup>
                handleOnlineChatGroupsUpdate(groups, clientId)
            }
            "SEND_CHAT_MESSAGE" -> {
                relayChatMessage(message, clientId)
            }
            "SEND_USER_INFO" -> {
                relayUserInfoUpdate(message, clientId)
            }
            "GET_PROFILE_PICTURE" -> {
                relayGetProfilePictureRequest(message, clientId)
            }
        }
        Log.d("AdhocServer", "Received Client Request Message: $message")
    }

    fun relayGroupCreationRequest(group: ChatGroup) {

    }

    fun relayDirectChatCreationRequest(message: ActionMessage, clientId: String) {
        val group = message.data as ChatGroup
        for (member in group.members) {
            val memberClient = clients.find { it.user.id == member.userId && it.user.id != clientId }
            if (memberClient != null) {
                serverScope.launch {
                    serverSocketManager?.sendMessageToClient(message, memberClient.user.id)
                }
            }
        }
    }

    fun sendGroupInfoUpdate() {

    }
    fun sendGroupEvent() {

    }
    fun relayUserInfoUpdate(message: ActionMessage, clientId: String) {
        val user = message.data as User
        for (client in clients) {
            if (client.user.id != clientId) {
                serverScope.launch {
                    serverSocketManager?.sendMessageToClient(message, client.user.id)
                }
            }
        }
    }

    fun relayGetProfilePictureRequest(message: ActionMessage, clientId: String) {
        val appFileRequest = message.data as AppFileRequest
        val requestToUserId = appFileRequest.requestToUserId
        val client = clients.find { it.user.id == requestToUserId }
        if (client != null) {
            serverScope.launch {
                serverSocketManager?.sendMessageToClient(message, client.user.id)
            }
        } else {
            Log.e("AdhocServer","GetProfilePicture request failed: user $requestToUserId not found")
        }
    }

    fun relayChatMessage(message: ActionMessage, clientId: String) {
        val chatMessage = message.data as ChatMessage
        val roomId = chatMessage.chatGroupId
        val roomClients = chatRooms[roomId]
        if (roomClients != null) {
            for (client in roomClients) {
                if (client.user.id != clientId) {
                    serverScope.launch {
                        serverSocketManager?.sendMessageToClient(message, client.user.id)
                    }
                }
            }
        } else {
            Log.e("AdhocServer", "No clients in room $roomId")
        }
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

    private suspend fun sendAvailableClientsLocked(clientSnapshot: List<Client>) {
        for (recipientClient in clientSnapshot) {
            // Send all clients except the recipient itself
            val otherClients = clientSnapshot.filter {
                it.user.id != recipientClient.user.id
            }

            val message = ActionMessage(
                actionType = "AVAILABLE_CLIENTS_UPDATE",
                data = otherClients
            )

            serverScope.launch {
                try {
                    serverSocketManager?.sendMessageToClient(message, recipientClient.user.id)
                    Log.d("AdhocServer", "Sent ${otherClients.size} available clients to ${recipientClient.user.name}")
                } catch (e: Exception) {
                    Log.e("AdhocServer", "Failed to send clients to ${recipientClient.user.name}: ${e.message}")
                }
            }
        }
    }

    private fun handleOnlineChatGroupsUpdate(chatGroups: List<ChatGroup>, clientId: String) {
        // Get the IDs of existing groups for efficient lookup
        val existingGroupIds = this.chatGroups.map { it.group.id }.toSet()

        // Filter out groups that are already in our list
        val newGroups = chatGroups.filter { incomingGroup ->
            incomingGroup.group.id !in existingGroupIds
        }

        // Add the new groups to our private list
        this.chatGroups.addAll(newGroups)

        // Find the client that sent this update
        val client = clients.find { it.user.id == clientId }
        if (client != null) {
            // Add this client to the chat rooms for the new groups
            chatGroups.forEach { group ->
                val groupId = group.group.id
                val roomClients = chatRooms.getOrPut(groupId) { mutableListOf() }
                if (client !in roomClients) {
                    roomClients.add(client)
                }
            }
        }
    }
}