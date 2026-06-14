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
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress

class ClientSocketManager() {
    private var socket: Socket? = null
    private var outStream: ObjectOutputStream? = null
    private var inStream: ObjectInputStream? = null
    private var listeningJob: Job? = null
    
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("ClientSocketManager", "Unhandled exception in clientScope: ${throwable.message}")
    }
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    
    @Volatile private var running = false

    var onMessageReceived: ((ActionMessage) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    suspend fun connect(host: String, port: Int) = withContext(Dispatchers.IO) {
        try {
            Log.d("ClientSocketManager", "Attempting to connect to $host:$port")
            val s = Socket()
            // Setting a timeout can help prevent long hangs, though ECONNREFUSED is usually fast
            s.connect(InetSocketAddress(host, port), 5000)
            s.keepAlive = true
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
                            // readObject throws EOFException if peer closes connection
                            val obj = input.readObject() ?: break
                            onReceiveMessage(obj)
                        } catch (e: EOFException) {
                            Log.d("ClientSocketManager", "Peer closed connection gracefully.")
                            break
                        } catch (e: IOException) {
                            Log.e("ClientSocketManager", "Connection lost: ${e.message}")
                            break
                        } catch (e: ClassNotFoundException) {
                            Log.e("ClientSocketManager", "Class not found: ${e.message}")
                        }
                    }
                    Log.d("ClientSocketManager", "Listening stopped")
                } catch (e: Exception) {
                    Log.e("ClientSocketManager", "Listening error: ${e.message}")
                } finally {
                    disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Connection error to $host:$port: ${e.message}")
            throw e
        }
    }

    suspend fun sendMessage(message: Any) = withContext(Dispatchers.IO) {
        if (!running) {
            Log.w("ClientSocketManager", "Cannot send message, not connected.")
            return@withContext
        }
        try {
            outStream?.writeObject(message)
            outStream?.flush()
        } catch (e: IOException) {
            Log.e("ClientSocketManager", "Send error (Broken pipe): ${e.message}")
            disconnect() // Peer went offline
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Send error: ${e.message}")
        }
    }

    suspend fun sendBytes(buffer: ByteArray, toSend: Int) = withContext(Dispatchers.IO) {
        try {
            outStream?.write(buffer, 0, toSend)
        } catch (e: IOException) {
            Log.e("ClientSocketManager", "Byte send error: ${e.message}")
            disconnect()
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Generic send error: ${e.message}")
        }
    }

    fun flushBuffer() {
        try {
            outStream?.flush()
        } catch (e: Exception) {
            Log.e("ClientSocketManager", "Flush error: ${e.message}")
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
        if (!running && socket == null) return

        Log.i("ClientSocketManager", "Disconnecting and cleaning up...")
        running = false

        // Notify higher layers immediately
        onDisconnected?.invoke()

        // Close resources in background to avoid blocking the caller
        clientScope.launch {
            try { listeningJob?.cancel() } catch (_: Exception) {}
            try { inStream?.close() } catch (_: Exception) {}
            try { outStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
            socket = null
        }
    }
}
