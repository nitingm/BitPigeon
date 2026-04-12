package com.codingskillshub.bitpigeon.infrastructure

import android.util.Log
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlinx.coroutines.*
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.domain.entities.User

class ClientSocketManager(private val host: String, private val port: Int) {
    private var socket: Socket? = null
    private var outStream: ObjectOutputStream? = null
    private var inStream: ObjectInputStream? = null
    private var clientName: String = "Client"
    private var listeningJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var running = false

    var onMessageReceived: ((ChatMessage) -> Unit)? = null
    var onUserInfoReceived: ((User) -> Unit)? = null

    suspend fun connect(user: User) = withContext(Dispatchers.IO) {
        try {
            clientName = user.name
            val s = Socket(host, port)
            socket = s
            val out = ObjectOutputStream(s.getOutputStream())
            outStream = out
            val input = ObjectInputStream(s.getInputStream())
            inStream = input
            
            // Send user info to server as first object
            out.writeObject(user)
            out.flush()
            
            running = true
            listeningJob = clientScope.launch {
                try {
                    while (running) {
                        val obj = input.readObject() ?: break
//                        if (obj is ChatMessage) {
//                            // Print only if not sent by self
//                            if (obj.senderId != clientName || obj.senderId == "SERVER") {
//                                Log.d("ClientSocketManager", obj.data.text)
//                            }
//                        }
                        onReceiveMessage(obj)
                    }
                    Log.d("ClientSocketManager", "Listening stopped")
                } catch (e: Exception) {
                    Log.e("ClientSocketManager", "Read error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Connection error: ${e.message}")
            throw e
        }
    }

    suspend fun sendMessageText(message: String) = withContext(Dispatchers.IO) {
        val msg = ChatMessage(
            id = "0",
            chatGroupId = "0",
            senderId = clientName,
            data = MessageData(text = message),
            timestamp = System.currentTimeMillis().toString()
        )
        sendMessage(msg)
    }

    suspend fun sendMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            outStream?.writeObject(message)
            outStream?.flush()
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Send error: ${e.message}")
        }
    }

    suspend fun sendUserInfo(user: User) = withContext(Dispatchers.IO) {
        try {
            outStream?.writeObject(user)
            outStream?.flush()
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Send user info error: ${e.message}")
        }
    }

    fun onReceiveMessage(message: Any) {
        Log.d("ClientSocketManager", "Received message: $message")
        if (message is ChatMessage)
            onMessageReceived?.invoke(message)
        else if (message is User)
            onUserInfoReceived?.invoke(message)
    }

    fun disconnect() {
        running = false
        clientScope.launch(Dispatchers.IO) {
            try { listeningJob?.cancel() } catch (_: Exception) {}
            try { inStream?.close() } catch (_: Exception) {}
            try { outStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
            clientScope.cancel()
        }
    }
}