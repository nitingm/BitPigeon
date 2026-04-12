package com.codingskillshub.bitpigeon.infrastructure

import android.util.Log
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket  
import java.util.Collections
import kotlinx.coroutines.*
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.domain.entities.User

class ServerSocketManager(private val port: Int) {
    private var serverSocket: ServerSocket? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class Client(val socket: Socket,
                              val out: ObjectOutputStream,
                              val `in`: ObjectInputStream,
                              val name: String)
    private val clients = Collections.synchronizedList(mutableListOf<Client>())
    var onMessageReceived: ((ChatMessage) -> Unit)? = null
    var onUserInfoReceived: ((User) -> Unit)? = null

    fun start() {
        serverScope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                Log.d("ServerSocketManager", "Server started on port $port")

                while (!ss.isClosed) {
                    try {
                        val socket = ss.accept()
                        val out = ObjectOutputStream(socket.getOutputStream())
                        val input = ObjectInputStream(socket.getInputStream())

                        Log.d("ServerSocketManager", "Client connected: ${socket.inetAddress.hostAddress}")
                        // Read user info as first object (expect User)
                        val user = input.readObject() as? User
                        val clientName = user?.name ?: socket.inetAddress.hostAddress
                        val client = Client(socket, out, input, clientName)

                        clients.add(client)
                        if (user != null) {
                            onUserInfoReceived?.invoke(user)
                        }
                        val joinMsg = ChatMessage(
                            id = "0",
                            chatGroupId = "0",
                            senderId = clientName,
                            data = MessageData(text = "$clientName connected."),
                            timestamp = System.currentTimeMillis().toString()
                        )
                        broadcast(joinMsg)

                        // Start a coroutine for this client
                        serverScope.launch {
                            try {
                                while (true) {
                                    val obj = input.readObject() ?: break
                                    if (obj is ChatMessage) {
                                        broadcast(obj)
                                    }
                                    onReceiveMessage(obj)
                                }
                            } catch (e: Exception) {
                                // ignore read errors
                            } finally {
                                removeClient(client)
                            }
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) Log.e("ServerSocketManager", "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerSocketManager", "Server error: ${e.message}")
            }
        }
    }

    private suspend fun removeClient(client: Client) = withContext(Dispatchers.IO) {
        try { client.`in`.close() } catch (_: Exception) {}
        try { client.out.close() } catch (_: Exception) {}
        try { client.socket.close() } catch (_: Exception) {}
        clients.remove(client)
        val leaveMsg = ChatMessage(
            id = "0",
            chatGroupId = "0",
            senderId = client.name,
            data = MessageData(text = "${client.name} disconnected."),
            timestamp = System.currentTimeMillis().toString()
        )
        broadcast(leaveMsg)
    }

    suspend fun sendMessage(message: ChatMessage) {
        broadcast(message)
    }

    suspend fun sendUserInfo(user: User) = withContext(Dispatchers.IO) {
        val snapshot = synchronized(clients) { clients.toList() }
        for (c in snapshot) {
            try {
                c.out.writeObject(user)
                c.out.flush()
            } catch (e: Exception) {
                // ignore failures; cleanup will happen elsewhere
            }
        }
    }

    private suspend fun broadcast(msg: ChatMessage) = withContext(Dispatchers.IO) {
        val snapshot = synchronized(clients) { clients.toList() }
        for (c in snapshot) {
            try {
                c.out.writeObject(msg)
                c.out.flush()
            } catch (e: Exception) {
                // ignore failures; cleanup will happen elsewhere
            }
        }
        if (clients.isEmpty()) {
            Log.d("ServerSocketManager", "No clients connected")
        }
    }

    fun onReceiveMessage(message: Any) {
        Log.d("ServerSocketManager", "Received message: $message")
        if (message is ChatMessage)
            onMessageReceived?.invoke(message)
        else if (message is User)
            onUserInfoReceived?.invoke(message)
    }

    fun stop() {
        serverScope.launch(Dispatchers.IO) {
            try {
                val snapshot = synchronized(clients) { clients.toList() }
                snapshot.forEach { client ->
                    try { client.`in`.close() } catch (_: Exception) {}
                    try { client.out.close() } catch (_: Exception) {}
                    try { client.socket.close() } catch (_: Exception) {}
                }
                clients.clear()
            } catch (_: Exception) {}
            try { serverSocket?.close() } catch (_: Exception) {}
            serverScope.cancel()
            Log.d("ServerSocketManager", "Server stopped")
        }
    }
}