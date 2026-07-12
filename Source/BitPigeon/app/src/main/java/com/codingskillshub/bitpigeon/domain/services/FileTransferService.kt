package com.codingskillshub.bitpigeon.domain.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.ActionMessage
import com.codingskillshub.bitpigeon.domain.entities.AppFile
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.Attachment
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.StoreIn
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.interfaces.dao.AttachmentDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.infrastructure.ClientSocketManager
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import com.codingskillshub.bitpigeon.infrastructure.ServerSocketManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.sql.ClientInfoStatus
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationService: ConfigurationService,
    private val attachmentDao: AttachmentDao,
    private val userDao: UserDao,
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

    private val _incomingAppFile = MutableSharedFlow<AppFile>()
    val incomingAppFile = _incomingAppFile.asSharedFlow()

    companion object {
        private const val PORT = 8889
        private const val BUFFER_SIZE = 4096
    }

    fun startFileTransferServer() {
        serviceScope.launch {
            val myId = configurationService.userIdFlow.firstOrNull() ?: ""
            val myName = configurationService.userNameFlow.firstOrNull() ?: "Me"
            selfUser = userDao.getUserById(myId).firstOrNull() ?: User(id = myId, name = "Me", deviceAddress = "",  "", "")

            serverSocketManager = ServerSocketManager(PORT, "FSS").apply {
                onClientConnected = { client -> handleClientConnection(client) }
                onClientDisconnected = { clientId -> handleClientDisconnection(clientId) }
                startForFileTransfer()
            }
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

    /**
     * Get input stream from file:// or content:// URI
     * @param fileUri The URI string (file:// or content://)
     * @param functionName The name of the calling function (for logging)
     * @return InputStream if successful, null otherwise
     */
    private fun getInputStream(fileUri: String, functionName: String): java.io.InputStream? {
        return try {
            val uri = Uri.parse(fileUri)
            when {
                uri.scheme == "file" -> {
                    Log.d("FileTransferService","[$functionName] Using FileInputStream for file:// URI")
                    val filePath = uri.path
                    if (filePath == null) {
                        Log.e("FileTransferService","[$functionName] ✗ Failed to extract path from file URI: $fileUri")
                        return null
                    }
                    val file = java.io.File(filePath)
                    if (!file.exists()) {
                        Log.e("FileTransferService","[$functionName] ✗ File does not exist at path: $filePath")
                        return null
                    }
                    Log.d("FileTransferService","[$functionName] File exists: ${file.absolutePath}, size: ${file.length()} bytes")
                    java.io.FileInputStream(file)
                }
                else -> {
                    Log.d("FileTransferService","[$functionName] Using ContentResolver for content:// URI")
                    context.contentResolver.openInputStream(uri)
                }
            }
        } catch (e: Exception) {
            Log.e("FileTransferService","[$functionName] ✗ Exception getting input stream: ${e.message}", e)
            null
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
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: SecurityException) {
                Log.e("FileTransferService", "SecurityException while taking persistable URI permission for ${attachment.filePath} \n ${e.message}")
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

    suspend fun sendAttachmentsToClient(attachmentsWithUris: List<Pair<Attachment, Uri>>, client: Client) {
        val clientSocketManager = ClientSocketManager("FCS")
        try {
            Log.d("FileTransferService","[sendAttachmentsToClient] Connecting to FileTransfer Server at ${client.ipAddress}:${PORT}")
            clientSocketManager.connect(client.ipAddress, PORT)
            selfUser?.let { clientSocketManager.sendMessage(it) }
            attachmentsWithUris.forEach { (attachment, uri) ->
                sendAttachmentToClient(attachment, uri.toString(), clientSocketManager)
            }
        } catch (e: Exception) {
            Log.e("FileTransferService","[sendAttachmentsToClient] ✗ Error sending attachments: ${e.message}", e)
        } finally {
            clientSocketManager.disconnect()
        }
    }

    private suspend fun sendAttachmentToClient(attachment: Attachment, fileUri: String, clientSocketManager: ClientSocketManager) = withContext(Dispatchers.IO) {
        try {
            attachmentDao.insertAttachment(attachment.copy(filePath = fileUri, transferStatus = TransferStatus.TRANSFERRING))
            updateProgress(attachment.id, 0)

            val actionMessage = ActionMessage("SEND_ATTACHMENT", attachment)
            clientSocketManager.sendMessage(actionMessage)

            Log.d("FileTransferService","[sendAttachmentToClient] File URI: $fileUri, Size: ${attachment.fileSize} bytes")
            val inputStream = getInputStream(fileUri, "sendAttachmentToClient")

            if (inputStream == null) {
                Log.e("FileTransferService","[sendAttachmentToClient] ✗ Failed to open input stream for URI: $fileUri")
                throw Exception("Failed to open input stream")
            }

            var totalSent = 0L
            inputStream.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    clientSocketManager.sendBytes(buffer, bytesRead)
                    totalSent += bytesRead
                    val progress = if (attachment.fileSize > 0) (totalSent * 100 / attachment.fileSize).toInt() else 100
                    updateProgress(attachment.id, progress)

                    if (totalSent % (BUFFER_SIZE.toLong() * 10) == 0L) {
                        Log.d("FileTransferService","[sendAttachmentToClient] Progress: $totalSent/${attachment.fileSize} bytes ($progress%)")
                    }
                }
                clientSocketManager.flushBuffer()
            }

            if (totalSent == attachment.fileSize) {
                Log.d("FileTransferService","[sendAttachmentToClient] ✓ File sent successfully ${attachment.fileName}. Sent: $totalSent bytes")
            } else {
                Log.w("FileTransferService","[sendAttachmentToClient] ⚠ File size mismatch. Expected: ${attachment.fileSize}, Sent: $totalSent bytes")
            }
            attachmentDao.insertAttachment(attachment.copy(transferStatus = TransferStatus.COMPLETED))
        } catch (e: Exception) {
            Log.e("FileTransferService","[sendAttachmentToClient] ✗ Error sending file: ${e.message}", e)
            attachmentDao.updateTransferStatus(attachment.id, TransferStatus.FAILED)
        } finally {
            updateProgress(attachment.id, null)
        }
    }

    suspend fun sendAttachmentToLocal(attachment: Attachment, fileUri: String) = withContext(Dispatchers.IO) {
        try {
            Log.d("FileTransferService","[sendAttachmentToLocal] Starting local copy")
            Log.d("FileTransferService","[sendAttachmentToLocal] File URI: $fileUri, Size: ${attachment.fileSize} bytes")

            updateProgress(attachment.id, 0)
            val (outputStream, uri) = fileStorageService.getOutputStream(attachment.fileName, attachment.storeIn == StoreIn.PRIVATE_STORAGE)

            val inputStream = getInputStream(fileUri, "sendAttachmentToLocal")

            if (inputStream == null) {
                Log.e("FileTransferService","[sendAttachmentToLocal] ✗ Failed to open input stream for URI: $fileUri")
                throw Exception("Failed to open input stream")
            }

            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalCopied = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        val progress = if (attachment.fileSize > 0) (totalCopied * 100 / attachment.fileSize).toInt() else 100
                        updateProgress(attachment.id, progress)

                        if (totalCopied % (BUFFER_SIZE.toLong() * 10) == 0L) {
                            Log.d("FileTransferService","[sendAttachmentToLocal] Progress: $totalCopied/${attachment.fileSize} bytes ($progress%)")
                        }
                    }
                }
            }

            if (attachment.storeIn == StoreIn.PUBLIC_STORAGE) {
                fileStorageService.finalizeFile(uri)
            }
            Log.d("FileTransferService", "[sendAttachmentToLocal] ✓ File copied locally: $uri")
            attachmentDao.insertAttachment(attachment.copy(filePath = uri.toString(), transferStatus = TransferStatus.COMPLETED))
        } catch (e: Exception) {
            Log.e("FileTransferService", "[sendAttachmentToLocal] ✗ Error copying file locally: ${e.message}", e)
        } finally {
            updateProgress(attachment.id, null)
        }
    }

    private suspend fun receiveAppFile(appFile: AppFile, clientId: String) {
        try {

            val (outputStream, uri) = fileStorageService.getOutputStream(appFile.fileName, appFile.storeIn == StoreIn.PRIVATE_STORAGE)

            outputStream.use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var totalRead = 0L
                while (totalRead < appFile.fileSize) {
                    val toRead = if (appFile.fileSize - totalRead < BUFFER_SIZE.toLong()) {
                        (appFile.fileSize - totalRead).toInt()
                    } else {
                        BUFFER_SIZE
                    }

                    val bytesRead = serverSocketManager?.readBytesFromClient(clientId, buffer, toRead) ?: -1
                    if (bytesRead <= 0) break

                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val progress = if (appFile.fileSize > 0) (totalRead * 100 / appFile.fileSize).toInt() else 100
                    updateProgress(appFile.id, progress)
                }
            }
            if (appFile.storeIn == StoreIn.PUBLIC_STORAGE) {
                fileStorageService.finalizeFile(uri)
            }
            Log.d("FileTransferService", "App File received successfully: $uri")
            _incomingAppFile.emit(appFile)
        } catch (e: Exception) {
            Log.e("FileTransferService", "Error receiving App file: ${e.message}")
        } finally {
            waitForIncomingFileFromClient(clientId)
        }
    }

    suspend fun sendAppFileToClient(appFile: AppFile, fileUri: String, client: Client) = withContext(Dispatchers.IO) {
        val clientSocketManager = ClientSocketManager("FCS")
        try {
            Log.d("FileTransferService","[sendAppFileToClient] Starting transfer to ${client.ipAddress}:${PORT}")
            Log.d("FileTransferService","[sendAppFileToClient] File URI: $fileUri, Size: ${appFile.fileSize} bytes")

            clientSocketManager.connect(client.ipAddress, PORT)
            Log.d("FileTransferService","[sendAppFileToClient] Connected to ${client.ipAddress}:${PORT}")

            selfUser?.let { clientSocketManager.sendMessage(it) }

            val actionMessage = ActionMessage("SEND_APP_FILE", appFile)
            clientSocketManager.sendMessage(actionMessage)

            val inputStream = getInputStream(fileUri, "sendAppFileToClient")

            if (inputStream == null) {
                Log.e("FileTransferService","[sendAppFileToClient] ✗ Failed to open input stream for URI: $fileUri")
                throw Exception("Failed to open input stream")
            }

            var totalSent = 0L
            inputStream.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    clientSocketManager.sendBytes(buffer, bytesRead)
                    totalSent += bytesRead
                    val progress = if (appFile.fileSize > 0) (totalSent * 100 / appFile.fileSize).toInt() else 100
                    updateProgress(appFile.id, progress)

                    if (totalSent % (BUFFER_SIZE.toLong() * 10) == 0L) {
                        Log.d("FileTransferService","[sendAppFileToClient] Progress: $totalSent/${appFile.fileSize} bytes ($progress%)")
                    }
                }
                clientSocketManager.flushBuffer()
            }

            if (totalSent == appFile.fileSize) {
                Log.d("FileTransferService","[sendAppFileToClient] ✓ File sent successfully to ${client.ipAddress}. Sent: $totalSent bytes")
            } else {
                Log.w("FileTransferService","[sendAppFileToClient] ⚠ File size mismatch. Expected: ${appFile.fileSize}, Sent: $totalSent bytes")
            }
        } catch (e: Exception) {
            Log.e("FileTransferService","[sendAppFileToClient] ✗ Error sending file: ${e.message}", e)
        } finally {
            Log.d("FileTransferService","[sendAppFileToClient] Disconnecting from ${client.ipAddress}:${PORT}")
            clientSocketManager.disconnect()
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
                } else if (message.actionType == "SEND_APP_FILE") {
                    val appFile = message.data as AppFile
                    receiveAppFile(appFile, clientId)
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
