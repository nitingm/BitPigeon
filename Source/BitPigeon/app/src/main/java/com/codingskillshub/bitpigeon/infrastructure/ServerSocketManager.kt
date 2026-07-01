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
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.IOException
import java.util.Collections

// Socket type
// CSS - Chat Server Socket
// FSS - File Server Socket
class ServerSocketManager(
    private val port: Int,
    private val socketType: String = "CSS"
) {
    private var serverSocket: ServerSocket? = null
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class ClientSocket(val socket: Socket,
                                    val outputStream: ObjectOutputStream,
                                    val inputStream: ObjectInputStream,
                                    val name: String)
    private val clientsMap = Collections.synchronizedMap(mutableMapOf<String, ClientSocket>())

    var onMessageReceived: ((ActionMessage, String) -> Unit)? = null

    var onClientConnected: ((Client) -> Unit)? = null
    var onClientDisconnected: ((String) -> Unit)? = null

    fun start() {
        serverScope.launch {
            if (socketType == "CSS") {
                startForChatMessaging()
            } else if (socketType == "FSS") {
                startForFileTransfer()
            }
        }
    }

    fun startForChatMessaging() {
        serverScope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                Log.d("ServerSocketManager", "$socketType:: ServerSocket created on port $port")

                while (!ss.isClosed) {
                    try {
                        val socket = ss.accept()
                        socket.keepAlive = true
                        val out = ObjectOutputStream(socket.getOutputStream())
                        val input = ObjectInputStream(socket.getInputStream())

                        Log.d("ServerSocketManager", "$socketType:: Client connected: ${socket.inetAddress.hostAddress}")

                        // Read user info as first object
                        val user = input.readObject()
                        if  (user is User) {
                            clientsMap[user.id] = ClientSocket(socket, out, input, user.name)
                            onClientConnected?.invoke(Client(user.name, socket.inetAddress.hostAddress, socket.inetAddress.hostAddress == "192.168.49.1",user))

                            // Start a coroutine for this client
                            serverScope.launch {
                                try {
                                    while (true) {
                                        try {
                                            val obj = input.readObject() ?: break
                                            onReceiveMessage(obj, user.id)
                                        } catch (e: EOFException) {
                                            Log.d("ServerSocketManager", "$socketType:: Client ${user.id} disconnected gracefully.")
                                            break
                                        } catch (e: IOException) {
                                            Log.e("ServerSocketManager", "$socketType:: Client ${user.id} connection lost: ${e.message}")
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ServerSocketManager", "$socketType:: Client ${user.id} unexpected error: ${e.message}")
                                } finally {
                                    removeClient(user.id)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) Log.e("ServerSocketManager", "$socketType:: Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerSocketManager", "$socketType:: Server error: ${e.message}")
            }
        }
    }

    fun startForFileTransfer() {
        serverScope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                Log.d("ServerSocketManager", "$socketType:: ServerSocket created on port $port")

                while (!ss.isClosed) {
                    try {
                        val socket = ss.accept()
                        socket.keepAlive = true
                        val out = ObjectOutputStream(socket.getOutputStream())
                        val input = ObjectInputStream(socket.getInputStream())

                        Log.d("ServerSocketManager", "$socketType:: Client connected: ${socket.inetAddress.hostAddress}")

                        // Read user info as first object
                        val user = input.readObject()
                        if  (user is User) {
                            clientsMap[user.id] = ClientSocket(socket, out, input, user.name)
                            onClientConnected?.invoke(Client(user.name, socket.inetAddress.hostAddress ?: "", socket.inetAddress.hostAddress == "192.168.49.1",user))
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) Log.e("ServerSocketManager", "$socketType:: Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ServerSocketManager", "$socketType:: Server error: ${e.message}")
            }
        }
    }

    private suspend fun removeClient(clientId: String) = withContext(Dispatchers.IO) {
        val client = clientsMap.remove(clientId)
        if (client != null) {
            Log.d("ServerSocketManager", "$socketType:: Cleaning up client: $clientId")
            try { client.inputStream.close() } catch (_: Exception) {}
            try { client.outputStream.close() } catch (_: Exception) {}
            try { client.socket.close() } catch (_: Exception) {}
            onClientDisconnected?.invoke(clientId)
        }
    }

    suspend fun sendMessageToClient(message: ActionMessage, clientId: String) = withContext(Dispatchers.IO) {
        val client = clientsMap[clientId]
        if (client != null) {
            try {
                client.outputStream.writeObject(message)
                client.outputStream.flush()
            } catch (e: Exception) {
                Log.e("ServerSocketManager", "$socketType:: Send error to client $clientId: ${e.message}")
                removeClient(clientId)
            }
        } else {
            Log.e("ServerSocketManager", "$socketType:: Client $clientId not found for sending message")
        }
    }

    suspend fun readBytesFromClient(clientId: String, buffer: ByteArray, toRead: Int): Int = withContext(Dispatchers.IO) {
        val client = clientsMap[clientId]
        var bytesRead = -1
        if (client != null) {
            try {
                bytesRead = client.inputStream.read(buffer, 0, toRead)
            } catch (e: IOException) {
                Log.e("ServerSocketManager", "$socketType:: Byte read error from $clientId: ${e.message}")
                removeClient(clientId)
            }
        } else {
            Log.e("ServerSocketManager", "$socketType:: Client $clientId not found for reading bytes")
        }
        return@withContext bytesRead
    }

    suspend fun readNextMessageFromClient(clientId: String): ActionMessage? {
        val client = clientsMap[clientId]
        var message: ActionMessage? = null
        if (client != null) {
            try {
                val obj = client.inputStream.readObject()
                message = obj as ActionMessage
            } catch (e: EOFException) {
                Log.d("ServerSocketManager", "$socketType:: Client ${clientId} disconnected gracefully.")
                removeClient(clientId)
            } catch (e: IOException) {
                Log.e("ServerSocketManager", "$socketType:: Client ${clientId} connection lost: ${e.message}")
                removeClient(clientId)
            } catch (e: Exception) {
                Log.e("ServerSocketManager", "$socketType:: Client ${clientId} unexpected error: ${e.message}")
                removeClient(clientId)
            }
        } else {
            Log.e("ServerSocketManager", "$socketType:: Client $clientId not found for reading bytes")
        }
        return message
    }

    fun onReceiveMessage(message: Any, clientId: String) {
        Log.d("ServerSocketManager", "$socketType:: Received message: $message")
        if (message is ActionMessage) {
            onMessageReceived?.invoke(message, clientId)
        } else {
            Log.e("ServerSocketManager", "$socketType:: Received invalid message: $message")
        }
    }

    fun stop() {
        serverScope.launch(Dispatchers.IO) {
            val snapshot = synchronized(clientsMap) { clientsMap.keys.toList() }
            snapshot.forEach { id -> removeClient(id) }
            try { serverSocket?.close() } catch (_: Exception) {}
            serverScope.cancel()
            Log.d("ServerSocketManager", "$socketType:: Server stopped")
        }
    }
}