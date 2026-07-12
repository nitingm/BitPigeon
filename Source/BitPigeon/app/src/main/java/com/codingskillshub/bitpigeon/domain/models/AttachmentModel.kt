package com.codingskillshub.bitpigeon.domain.models

import android.util.Log
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.codingskillshub.bitpigeon.domain.entities.Attachment
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import com.codingskillshub.bitpigeon.domain.interfaces.dao.AttachmentDao
import com.codingskillshub.bitpigeon.domain.interfaces.dao.ChatGroupDao
import com.codingskillshub.bitpigeon.domain.services.FileTransferService
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.exists

@Singleton
class AttachmentModel @Inject constructor(
    private val fileTransferService: FileTransferService,
    private val appSystemModel: AppSystemModel,
    private val chatGroupDao: ChatGroupDao,
    private val attachmentDao: AttachmentDao,
    private val onlineChatService: OnlineChatService,
    private val wifiCommunicationService: WifiCommunicationService,
    private val fileStorageService: FileStorageService,
    @ApplicationContext private val context: Context
) {
    private val modelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val attachmentsDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "BitPigeon")
    }

    init {
        modelScope.launch {
            wifiCommunicationService.connectionInfo.collectLatest { info ->
                if (info == null) {
                    fileTransferService.stopFileTransferServer()
                } else {
                    fileTransferService.startFileTransferServer()
                }
            }
        }
    }

    /**
     * Creates attachment metadata and initiates file transfer.
     * Performs initial database insertion synchronously to avoid race conditions.
     * @return A list of generated attachment IDs.
     */
    suspend fun sendAttachments(uris: List<Uri>, messageId: String, chatGroupId: String, isPersonalChat: Boolean): List<String> = withContext(Dispatchers.IO) {
        val attachmentIds = mutableListOf<String>()
        val attachmentsWithUris = mutableListOf<Pair<Attachment, Uri>>()

        uris.forEach { uri ->
            val attachment = createAttachmentFromUri(uri, messageId, chatGroupId)
            if (attachment != null) {
                attachmentIds.add(attachment.id)
                attachmentsWithUris.add(Pair(attachment, uri))
            }
        }

        // 1. Initial Insert (PENDING) - Done synchronously before returning to avoid race conditions
        attachmentsWithUris.forEach { (attachment, _) ->
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(attachment.filePath.toUri(), takeFlags)
            } catch (e: SecurityException) {
                Log.e("AttachmentModel", "SecurityException while taking persistable URI permission for ${attachment.filePath} \n ${e.message}")
            }
            attachmentDao.insertAttachment(attachment)
        }

        // 2. Background transfer processing
        modelScope.launch {
            if (isPersonalChat) {
                attachmentsWithUris.forEach { (attachment, uri) ->
                    attachmentDao.insertAttachment(attachment.copy(transferStatus = TransferStatus.TRANSFERRING))
                    fileTransferService.sendAttachmentToLocal(attachment, uri.toString())
                }
            } else {
                val clients = getOnlineClientsForChatGroup(chatGroupId)
                if (clients.isEmpty()) return@launch

                clients.forEach { client ->
                    launch {
                        try {
                            fileTransferService.sendAttachmentsToClient(attachmentsWithUris, client)
                        } catch (e: Exception) {
                            // Transfer failed for this client
                        }
                    }
                }
            }
        }

        return@withContext attachmentIds
    }

    private fun createAttachmentFromUri(uri: Uri, messageId: String, chatGroupId: String): Attachment? {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                val name = if (nameIndex != -1) it.getString(nameIndex) else "file_${System.currentTimeMillis()}"
                val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
                val type = contentResolver.getType(uri) ?: "application/octet-stream"

                Attachment(
                    id = UUID.randomUUID().toString(),
                    messageId = messageId,
                    chatGroupId = chatGroupId,
                    senderId = appSystemModel.getMyUserId(),
                    fileName = name,
                    fileSize = size,
                    fileType = type,
                    filePath = uri.toString(),
                    timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    transferStatus = TransferStatus.PENDING
                )
            } else null
        }
    }

    private suspend fun getOnlineClientsForChatGroup(chatGroupId: String): List<Client> {
        val members = chatGroupDao.getChatGroupById(chatGroupId).firstOrNull()?.members ?: return emptyList()
        return onlineChatService.availablePeerClients.value.filter { client ->
            members.any { member -> member.userId == client.user.id }
        }
    }

    fun getAttachmentPreviewData(uris: List<Uri>): List<AttachmentPreviewData> {
        val contentResolver = context.contentResolver
        return uris.map { uri ->
            var fileName = "unknown"
            val type = contentResolver.getType(uri) ?: "application/octet-stream"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: "unknown"
                }
            }

            AttachmentPreviewData(
                id = "",
                fileName = fileName,
                fileType = type,
                fileUri = uri
            )
        }
    }

    fun getAttachmentPreviewDataForUri(uri: Uri): AttachmentPreviewData {
        val contentResolver = context.contentResolver
        var fileName = "unknown"
        val type = contentResolver.getType(uri) ?: "application/octet-stream"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex) ?: "unknown"
            }
        }

        return AttachmentPreviewData(
                id = "",
                fileName = fileName,
                fileType = type,
                fileUri = uri
            )
    }

    fun getAllAttachmentsForChatGroup(chatGroupId: String): Flow<List<Attachment>> {
        return attachmentDao.getAllAttachmentsForChatGroup(chatGroupId)
    }

    suspend fun getAttachmentsForMessage(messageId: String): List<Attachment> {
        return attachmentDao.getAttachmentsForMessageSync(messageId)
    }

    fun getAllMediaAttachmentPreviewData(): Flow<List<AttachmentPreviewData>> {
        return attachmentDao.getAllAttachmentsSortedByChatGroup().map { attachments ->
            attachments.mapNotNull { it.toMediaPreviewOrNull() }
        }
    }

    private fun Attachment.toMediaPreviewOrNull(): AttachmentPreviewData? {
        val uri = filePath.toUri()
//        val isLocalImage = fileStorageService.checkFileExist(fileName, uri) &&
        val isLocalImage = (fileType.startsWith("image/", ignoreCase = true) ||
                        fileType.startsWith("video/", ignoreCase = true))

        return if (isLocalImage) {
            AttachmentPreviewData(
                id = id,
                fileName = fileName,
                fileType = fileType,
                // Recommendation: Use FileProvider if this URI is shared externally
                fileUri = uri
            )
        } else null
    }
    
    suspend fun getAttachmentPreviewDataForMessage(messageId: String): List<AttachmentPreviewData> {
        val attachments = attachmentDao.getAttachmentsForMessageSync(messageId)
        val directory = File(context.getExternalFilesDir(null), "BitPigeon")
        
        return attachments.mapNotNull { attachment ->
            val file = File(directory, attachment.fileName)
            val isProcessing = attachment.transferStatus == TransferStatus.TRANSFERRING || 
                              attachment.transferStatus == TransferStatus.PENDING
            
            if (isProcessing || file.exists()) {
                AttachmentPreviewData(
                    id = attachment.id,
                    fileName = attachment.fileName,
                    fileType = attachment.fileType,
                    fileUri = Uri.fromFile(file),
                    isTransferring = isProcessing,
                    progress = 0
                )
            } else {
                null
            }
        }
    }

    fun getPhotoAttachmentPreviewDataForChatGroup(chatGroupId: String): Flow<List<AttachmentPreviewData>> {
        return attachmentDao.getAllAttachmentsForChatGroup(chatGroupId).map { attachments ->
            attachments.mapNotNull { it.toImagePreviewOrNull() }
        }
    }

    private fun Attachment.toImagePreviewOrNull(): AttachmentPreviewData? {
        val uri = filePath.toUri()
        val isLocalImage = fileStorageService.checkFileExist(fileName, uri) &&
                fileType.startsWith("image/", ignoreCase = true)

        return if (isLocalImage) {
            AttachmentPreviewData(
                id = id,
                fileName = fileName,
                fileType = fileType,
                // Recommendation: Use FileProvider if this URI is shared externally
                fileUri = uri
            )
        } else null
    }

    fun getVideoAttachmentPreviewDataForInChatGroup(chatGroupId: String): Flow<List<AttachmentPreviewData>> {
        return attachmentDao.getAllAttachmentsForChatGroup(chatGroupId).map { attachments ->
            attachments.mapNotNull { it.toVideoPreviewOrNull() }
        }
    }

    private fun Attachment.toVideoPreviewOrNull(): AttachmentPreviewData? {
        val uri = filePath.toUri()
        val isLocalVideo = fileStorageService.checkFileExist(fileName, uri) &&
                fileType.startsWith("video/", ignoreCase = true)

        return if (isLocalVideo) {
            AttachmentPreviewData(
                id = id,
                fileName = fileName,
                fileType = fileType,
                // Recommendation: Use FileProvider if this URI is shared externally
                fileUri = uri
            )
        } else null
    }

    fun getFileAttachmentPreviewDataForInChatGroup(chatGroupId: String): Flow<List<AttachmentPreviewData>> {
        return attachmentDao.getAllAttachmentsForChatGroup(chatGroupId).map { attachments ->
            attachments.mapNotNull { it.toFilePreviewOrNull() }
        }
    }

    private fun Attachment.toFilePreviewOrNull(): AttachmentPreviewData? {
        val uri = filePath.toUri()
        val isLocalFile = fileStorageService.checkFileExist(fileName, uri) &&
                fileType.startsWith("application/", ignoreCase = true)

        return if (isLocalFile) {
            AttachmentPreviewData(
                id = id,
                fileName = fileName,
                fileType = fileType,
                // Recommendation: Use FileProvider if this URI is shared externally
                fileUri = uri
            )
        } else null
    }
}
