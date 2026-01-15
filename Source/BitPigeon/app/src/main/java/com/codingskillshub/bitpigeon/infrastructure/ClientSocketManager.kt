package com.codingskillshub.bitpigeon.infrastructure

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlinx.coroutines.*
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.MessageData

class ClientSocketManager(private val host: String, private val port: Int) {
    private lateinit var socket: Socket
    private lateinit var out: ObjectOutputStream
    private lateinit var `in`: ObjectInputStream
    private var clientName: String = "Client"
    private var listeningJob: Job? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile private var running = false

    fun connect(name: String = "Client") {
        clientName = name
        socket = Socket(host, port)
        out = ObjectOutputStream(socket.getOutputStream())
        `in` = ObjectInputStream(socket.getInputStream())
        // Send client name to server as first object
        out.writeObject(clientName)
        out.flush()
        running = true
        listeningJob = clientScope.launch {
            try {
                while (running) {
                    val obj = `in`.readObject() ?: break
                    if (obj is ChatMessage) {
                        // Print only if not sent by self
                        if (obj.senderId != clientName || obj.senderId == "SERVER") {
                            println(obj.data.text)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(message: String) {
        val msg = ChatMessage(
            id = "0",
            chatGroupId = "0",
            senderId = clientName,
            data = MessageData(text = message),
            timestamp = System.currentTimeMillis().toString()
        )
        out.writeObject(msg)
        out.flush()
    }

    fun receiveMessage(): String? {
        // Not needed anymore, but kept for compatibility
        return null
    }

    fun disconnect() {
        running = false
        try { listeningJob?.cancel() } catch (_: Exception) {}
        try { `in`.close() } catch (_: Exception) {}
        try { out.close() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
        clientScope.cancel()
    }
}