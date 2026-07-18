package com.codingskillshub.bitpigeon.domain.services

import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.types.WifiDirectPeer
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

    private val peersInWifiGroup: MutableList<WifiDirectPeer> = mutableListOf()

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
                // Create immutable snapshot of clients to avoid concurrent modification
                val clientSnapshot = clients.toList()
                sendAvailableClientsLocked(clientSnapshot)
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
                // Create immutable snapshot of clients to avoid concurrent modification
                val clientSnapshot = clients.toList()
                sendAvailableClientsLocked(clientSnapshot)
            }
        }
    }

    private fun handleClientRequest(message: ActionMessage, clientId: String) {
        Log.d("AdhocServer", "Received Client Request Message: $message")
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
            "SYNC_WIFI_DIRECT_PEER_INFO" -> {
                handleWifiDirectPeerInfo(message, clientId)
            }
        }
    }

    fun relayGroupCreationRequest(group: ChatGroup) {

    }

    fun relayDirectChatCreationRequest(message: ActionMessage, clientId: String) {
        val group = message.data as ChatGroup
        handleOnlineChatGroupsUpdate(listOf(group)  , clientId)
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

    private suspend fun sendAvailableClientsLocked(clientSnapshot: List<Client>) {
        for (recipientClient in clientSnapshot) {
            // Send all clients except the recipient itself
            val otherClients = clientSnapshot.filter {
                it.user.id != recipientClient.user.id
            }

            // Create message with a copy of the list to avoid mutation issues
            val messageData: List<Client> = otherClients.toList()
            val message = ActionMessage(
                actionType = "AVAILABLE_CLIENTS_UPDATE",
                data = messageData
            )

            serverScope.launch {
                try {
                    serverSocketManager?.sendMessageToClient(message, recipientClient.user.id)
                    Log.d("AdhocServer", "Sent ${messageData.size} available clients to ${recipientClient.user.name}")
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

    private fun handleWifiDirectPeerInfo(message: ActionMessage, clientId: String) {
        // Handle the Wi-Fi Direct peer info update
        Log.d("AdhocServer", "Received Wi-Fi Direct peer info from client $clientId: ${message.data}")
        val peerInfo = message.data as WifiDirectPeer
        if (peersInWifiGroup.none { it.userId == peerInfo.userId }) {
            peersInWifiGroup.add(peerInfo)
            Log.d("AdhocServer", "Added new peer to WiFi group: ${peerInfo.userName}")
            // Launch on server scope to send updates to all connected clients
            serverScope.launch {
                clientsMutex.withLock {
                    // Create immutable snapshot of peers and clients before sending
                    val peerSnapshot = peersInWifiGroup.toList()
                    val clientSnapshot = clients.toList()
                    sendWifiGroupPeersUpdate(peerSnapshot, clientSnapshot)
                }
            }
        }
    }

    private suspend fun sendWifiGroupPeersUpdate(peers: List<WifiDirectPeer>, clientSnapshot: List<Client>) {
        // Create the message once with immutable data
        for (recipientClient in clientSnapshot) {
            val peersData: List<WifiDirectPeer> = peers.toList()  // Create immutable copy
            val message = ActionMessage(
                actionType = "WIFI_GROUP_PEERS_UPDATE",
                data = peersData
            )
            try {
                serverSocketManager?.sendMessageToClient(message, recipientClient.user.id)
                Log.d("AdhocServer", "Sent ${peersData.size} WiFi group peers to ${recipientClient.user.name}")
            } catch (e: Exception) {
                Log.e("AdhocServer", "Failed to send WiFi group peers to ${recipientClient.user.name}: ${e.message}")
            }
        }
    }
}
