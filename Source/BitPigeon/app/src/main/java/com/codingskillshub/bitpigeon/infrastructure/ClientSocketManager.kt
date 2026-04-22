package com.codingskillshub.bitpigeon.infrastructure

import android.util.Log
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlinx.coroutines.*
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.domain.entities.User

class ClientSocketManager() {
    private var socket: Socket? = null
    private var outStream: ObjectOutputStream? = null
    private var inStream: ObjectInputStream? = null
    private var listeningJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var running = false

    var onMessageReceived: ((ActionMessage) -> Unit)? = null

    suspend fun connect(host: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            val s = Socket(host, port)
            socket = s
            val out = ObjectOutputStream(s.getOutputStream())
            outStream = out
            val input = ObjectInputStream(s.getInputStream())
            inStream = input
            
            running = true
            listeningJob = clientScope.launch {
                try {
                    while (running) {
                        try {
                            val obj = input.readObject() ?: break
                            onReceiveMessage(obj)
                        } catch (e: Exception) {
                            Log.e("ClientSocketManager", "Read error: ${e.message}")
                            // Continue reading in case of stream corruption
                        }
                    }
                    Log.d("ClientSocketManager", "Listening stopped")
                } catch (e: Exception) {
                    Log.e("ClientSocketManager", "Listening error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Connection error: ${e.message}")
            throw e
        }
    }

    suspend fun sendMessage(message: Any) = withContext(Dispatchers.IO) {
        try {
            outStream?.writeObject(message)
            outStream?.flush()
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Send error: ${e.message}")
        }
    }

    private fun onReceiveMessage(message: Any) {
        Log.d("ClientSocketManager", "Received message: $message")
        if (message is ActionMessage) {
            onMessageReceived?.invoke(message)
        } else {
            Log.w("ClientSocketManager", "Received invalid message: $message")
        }
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