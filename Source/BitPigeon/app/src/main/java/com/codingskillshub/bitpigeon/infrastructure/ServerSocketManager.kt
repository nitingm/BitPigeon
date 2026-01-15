package com.codingskillshub.bitpigeon.infrastructure

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket  
import java.util.Collections
import kotlinx.coroutines.*
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.MessageData

class ServerSocketManager(private val port: Int) {
    private lateinit var serverSocket: ServerSocket
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class Client(val socket: Socket, val out: ObjectOutputStream, val `in`: ObjectInputStream, val name: String)
    private val clients = Collections.synchronizedList(mutableListOf<Client>())

    fun start() {
        serverSocket = ServerSocket(port)
        println("Server started on port $port")

        serverScope.launch {
            while (!serverSocket.isClosed) {
                try {
                    val socket = serverSocket.accept()
                    val out = ObjectOutputStream(socket.getOutputStream())
                    val input = ObjectInputStream(socket.getInputStream())
                    // Read client name as first object (expect String)
                    val clientName = input.readObject() as? String ?: socket.inetAddress.hostAddress
                    val client = Client(socket, out, input, clientName)

                    clients.add(client)
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
                            }
                        } catch (e: Exception) {
                            // ignore read errors
                        } finally {
                            removeClient(client)
                        }
                    }
                } catch (e: Exception) {
                    if (!serverSocket.isClosed) e.printStackTrace()
                }
            }
        }
    }

    private fun removeClient(client: Client) {
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

    fun onClientConnect(callback: (Socket) -> Unit) {
        serverScope.launch {
            while (!serverSocket.isClosed) {
                val client = serverSocket.accept()
                callback(client)
            }
        }
    }

    fun sendMessage(message: String) {
        val msg = ChatMessage(
            id = "0",
            chatGroupId = "0",
            senderId = "SERVER",
            data = MessageData(text = message),
            timestamp = System.currentTimeMillis().toString()
        )
        broadcast(msg)
    }

    private fun broadcast(msg: ChatMessage) {
        val snapshot = synchronized(clients) { clients.toList() }
        for (c in snapshot) {
            try {
                c.out.writeObject(msg)
                c.out.flush()
            } catch (e: Exception) {
                // ignore failures; cleanup will happen elsewhere
            }
        }
    }

    fun receiveMessage(): String? {
        return null
    }

    fun stop() {
        try {
            val snapshot = synchronized(clients) { clients.toList() }
            snapshot.forEach { removeClient(it) }
        } catch (_: Exception) {}
        try { serverSocket.close() } catch (_: Exception) {}
        serverScope.cancel()
        println("Server stopped")
    }
}