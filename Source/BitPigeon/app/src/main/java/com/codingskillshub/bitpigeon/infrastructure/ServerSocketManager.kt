package com.codingskillshub.bitpigeon.infrastructure

import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket  
import kotlinx.coroutines.*
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import java.util.Collections

class ServerSocketManager(private val port: Int) {
    private var serverSocket: ServerSocket? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class ClientSocket(val socket: Socket,
                                    val outputStream: ObjectOutputStream,
                                    val inputStream: ObjectInputStream,
                                    val name: String)
    private val clientsMap = Collections.synchronizedMap(mutableMapOf<String, ClientSocket>())

    var onMessageReceived: ((ActionMessage) -> Unit)? = null

    var onClientConnected: ((Client) -> Unit)? = null
    var onClientDisconnected: ((Client) -> Unit)? = null

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

                        // Read user info as first object
                        val user = input.readObject()
                        if  (user is User) {
                            clientsMap[user.id] = ClientSocket(socket, out, input, user.name)
                            onClientConnected?.invoke(Client(user.name, socket.inetAddress.hostAddress, socket.inetAddress.hostAddress == ss.inetAddress.hostAddress,user))

                            // Start a coroutine for this client
                            serverScope.launch {
                                try {
                                    while (true) {
                                        val obj = input.readObject() ?: break
                                        onReceiveMessage(obj)
                                    }
                                } catch (e: Exception) {
                                    Log.e("ServerSocketManager", "Client read error: ${e.message}")
                                    removeClient(user.id)
                                }
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

    private suspend fun removeClient(clientId: String) = withContext(Dispatchers.IO) {
        var client = clientsMap[clientId]
        if (client != null) {
            try {
                client.inputStream.close()
            } catch (_: Exception) {
            }
            try {
                client.outputStream.close()
            } catch (_: Exception) {
            }
            try {
                client.socket.close()
            } catch (_: Exception) {
            }
            clientsMap.remove(clientId)
        }
    }

    suspend fun sendMessageToClient(message: ActionMessage, clientId: String) = withContext(Dispatchers.IO) {
        val client = clientsMap[clientId]
        if (client != null) {
            try {
                client.outputStream.writeObject(message)
                client.outputStream.flush()
            } catch (e: Exception) {
                Log.e("ServerSocketManager", "Send error to client $clientId: ${e.message}")
                removeClient(clientId)
            }
        } else {
            Log.e("ServerSocketManager", "Client $clientId not found for sending message")
        }
    }

    fun onReceiveMessage(message: Any) {
        Log.d("ServerSocketManager", "Received message: $message")
        if (message is ActionMessage) {
            onMessageReceived?.invoke(message)
        } else {
            Log.e("ServerSocketManager", "Received invalid message: $message")
        }
    }

    fun stop() {
        serverScope.launch(Dispatchers.IO) {
            val snapshot = synchronized(clientsMap) { clientsMap.values.toList() }
            snapshot.forEach { client ->
                try { client.inputStream.close() } catch (_: Exception) {}
                try { client.outputStream.close() } catch (_: Exception) {}
                try { client.socket.close() } catch (_: Exception) {}
            }
            clientsMap.clear()
            try { serverSocket?.close() } catch (_: Exception) {}
            serverScope.cancel()
            Log.d("ServerSocketManager", "Server stopped")
        }
    }
}