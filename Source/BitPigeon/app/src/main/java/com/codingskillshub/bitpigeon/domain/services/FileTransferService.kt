package com.codingskillshub.bitpigeon.domain.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.Attachment
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.StoreIn
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.AttachmentDao
import com.codingskillshub.bitpigeon.infrastructure.ClientSocketManager
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import com.codingskillshub.bitpigeon.infrastructure.ServerSocketManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationService: ConfigurationService,
    private val attachmentDao: AttachmentDao,
    private val fileStorageService: FileStorageService
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocketManager: ServerSocketManager? = null
    private var selfUser: User? = null
    private val clients = CopyOnWriteArrayList<Client>()
    private val clientsMutex = Mutex()

    // StateFlow exposing a list of (attachmentId, progress) pairs
    private val _transferProgress = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val transferProgress: StateFlow<List<Pair<String, Int>>> = _transferProgress.asStateFlow()

    companion object {
        private const val PORT = 8889
        private const val BUFFER_SIZE = 4096
    }

    fun startFileTransferServer() {
        serviceScope.launch {
            val myId = configurationService.userIdFlow.firstOrNull() ?: ""
            val myName = configurationService.userNameFlow.firstOrNull() ?: "Me"
            selfUser = User(id = myId, name = myName, deviceAddress = "", phoneNumber = "", email = "")

            serverSocketManager = ServerSocketManager(PORT).apply {
                onClientConnected = { client -> handleClientConnection(client) }
                onClientDisconnected = { clientId -> handleClientDisconnection(clientId) }
            }
            serverSocketManager?.startForFileTransfer()
        }
    }

    fun stopFileTransferServer() {
        serverSocketManager?.stop()
    }

    private fun updateProgress(attachmentId: String, progress: Int?) {
        _transferProgress.update { currentList ->
            if (progress == null) {
                currentList.filter { it.first != attachmentId }
            } else {
                val index = currentList.indexOfFirst { it.first == attachmentId }
                if (index != -1) {
                    currentList.toMutableList().apply {
                        this[index] = attachmentId to progress
                    }
                } else {
                    currentList + (attachmentId to progress)
                }
            }
        }
    }

    private suspend fun receiveFile(attachment: Attachment, clientId: String) {
        try {
            attachmentDao.insertAttachment(attachment.copy(transferStatus = TransferStatus.TRANSFERRING))
            updateProgress(attachment.id, 0)

            val (outputStream, uri) = fileStorageService.getOutputStream(attachment.fileName, attachment.storeIn == StoreIn.PRIVATE_STORAGE)

            outputStream.use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var totalRead = 0L
                while (totalRead < attachment.fileSize) {
                    val toRead = if (attachment.fileSize - totalRead < BUFFER_SIZE.toLong()) {
                        (attachment.fileSize - totalRead).toInt()
                    } else {
                        BUFFER_SIZE
                    }

                    val bytesRead = serverSocketManager?.readBytesFromClient(clientId, buffer, toRead) ?: -1
                    if (bytesRead <= 0) break

                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val progress = if (attachment.fileSize > 0) (totalRead * 100 / attachment.fileSize).toInt() else 100
                    updateProgress(attachment.id, progress)
                }
            }
            if (attachment.storeIn == StoreIn.PUBLIC_STORAGE) {
                fileStorageService.finalizeFile(uri)
            }
            Log.d("FileTransferService", "File received successfully: $uri")
            attachmentDao.insertAttachment(attachment.copy(filePath = uri.toString(), transferStatus = TransferStatus.COMPLETED))
        } catch (e: Exception) {
            Log.e("FileTransferService", "Error receiving file: ${e.message}")
            attachmentDao.updateTransferStatus(attachment.id, TransferStatus.FAILED)
        } finally {
            updateProgress(attachment.id, null)
            waitForIncomingFileFromClient(clientId)
        }
    }

    suspend fun sendAttachmentToClient(attachment: Attachment, fileUri: String, client: Client) = withContext(Dispatchers.IO) {
        val clientSocketManager = ClientSocketManager()
        try {
            attachmentDao.insertAttachment(attachment.copy(filePath = fileUri,transferStatus = TransferStatus.TRANSFERRING))
            updateProgress(attachment.id, 0)
            Log.d("FileTransferService","Connecting to FileTransfer Server at ${client.ipAddress}:${PORT}")
            clientSocketManager.connect(client.ipAddress, PORT)

            selfUser?.let { clientSocketManager.sendMessage(it) }

            val actionMessage = ActionMessage("SEND_ATTACHMENT", attachment)
            clientSocketManager.sendMessage(actionMessage)

            context.contentResolver.openInputStream(Uri.parse(fileUri))?.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalSent = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    clientSocketManager.sendBytes(buffer, bytesRead)
                    totalSent += bytesRead
                    val progress = if (attachment.fileSize > 0) (totalSent * 100 / attachment.fileSize).toInt() else 100
                    updateProgress(attachment.id, progress)
                }
                clientSocketManager.flushBuffer()
            }
            Log.d("FileTransferService", "File sent successfully to ${client.ipAddress}")
            attachmentDao.insertAttachment(attachment.copy(transferStatus = TransferStatus.COMPLETED))
        } catch (e: Exception) {
            Log.e("FileTransferService", "Error sending file: ${e.message}")
            attachmentDao.updateTransferStatus(attachment.id, TransferStatus.FAILED)
        } finally {
            Log.d("FileTransferService","Disconnecting from FileTransfer Server at ${client.ipAddress}:${PORT}")
            clientSocketManager.disconnect()
            updateProgress(attachment.id, null)
        }
    }

    suspend fun sendAttachmentToLocal(attachment: Attachment, fileUri: String) = withContext(Dispatchers.IO) {
        try {
            updateProgress(attachment.id, 0)
            val (outputStream, uri) = fileStorageService.getOutputStream(attachment.fileName, attachment.storeIn == StoreIn.PRIVATE_STORAGE)

            context.contentResolver.openInputStream(Uri.parse(fileUri))?.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalCopied = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        val progress = if (attachment.fileSize > 0) (totalCopied * 100 / attachment.fileSize).toInt() else 100
                        updateProgress(attachment.id, progress)
                    }
                }
            }
            if (attachment.storeIn == StoreIn.PUBLIC_STORAGE) {
                fileStorageService.finalizeFile(uri)
            }
            Log.d("FileTransferService", "File copied locally: $uri")
            attachmentDao.insertAttachment(attachment.copy(filePath = uri.toString(), transferStatus = TransferStatus.COMPLETED))
        } catch (e: Exception) {
            Log.e("FileTransferService", "Error copying file locally: ${e.message}")
        } finally {
            updateProgress(attachment.id, null)
        }
    }

    private fun handleClientConnection(client: Client) {
        serviceScope.launch {
            clientsMutex.withLock {
                clients.add(client)
                waitForIncomingFileFromClient(clientId = client.user.id)
            }
        }
    }

    private suspend fun waitForIncomingFileFromClient(clientId: String) = withContext(Dispatchers.IO) {
        if (clients.indexOfFirst { it.user.id == clientId } != -1) {
            serverSocketManager?.readNextMessageFromClient(clientId).let { message ->
                if (message == null) {
                    Log.d("FileTransferService", "Client disconnected: $clientId")
                    return@withContext
                }
                if (message.actionType == "SEND_ATTACHMENT") {
                    val attachment = message.data as Attachment
                    receiveFile(attachment, clientId)
                }
            }
        } else {
            Log.e("FileTransferService", "Client not found: $clientId")
        }
    }

    private fun handleClientDisconnection(clientId: String) {
        serviceScope.launch {
            clientsMutex.withLock {
                val disconnectedClient = clients.find { it.user.id == clientId }
                clients.remove(disconnectedClient)
            }
        }
    }
}
