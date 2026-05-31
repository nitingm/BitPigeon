package com.codingskillshub.bitpigeon.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.ChatMessageUIExtended
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.models.ChatModel
import com.codingskillshub.bitpigeon.domain.models.AttachmentModel
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import com.codingskillshub.bitpigeon.domain.services.FileTransferService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatModel: ChatModel,
    val attachmentModel: AttachmentModel,
    val conversationModel: ConversationModel,
    val configurationService: ConfigurationService,
    val fileTransferService: FileTransferService,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    val chatId: String = savedStateHandle["chatId"] ?: "unknown_user"

    /**
     * Reactive messages list that observes:
     * 1. Chat Messages
     * 2. Group Members
     * 3. Current User ID
     * 4. Real-time Transfer Progress
     * 5. Attachment Database Updates (via getAllAttachmentsForChatGroup)
     */
    val messages: StateFlow<List<ChatMessageUIExtended>> = combine(
        chatModel.messages,
        chatModel.getMembersForActiveChatGroup(),
        configurationService.userIdFlow,
        fileTransferService.transferProgress,
        attachmentModel.getAllAttachmentsForChatGroup(chatId) // The critical reactive trigger
    ) { rawMessages, members, myId, transfers, dbAttachments ->
        var lastDate = ""
        rawMessages.map { message ->
            val sender = members.find { it.id == message.senderId }
            
            // Extract and map attachments for this message from the reactively updated list
            val attachmentPreviews = dbAttachments
                .filter { it.messageId == message.id }
                .mapNotNull { attachment ->
                    val uri = Uri.parse(attachment.filePath)
                    Log.d("ChatViewModel","uri: ${attachment.filePath}, attachment: ${attachment}")
                    // Check if there is an active transfer happening right now
                    val activeTransfer = transfers.find { it.first == attachment.id }

                    // An attachment is "processing" if it's in the active transfers flow
                    // or its DB status says it's still pending/transferring.
                    val isProcessing = activeTransfer != null ||
                                     attachment.transferStatus == TransferStatus.TRANSFERRING ||
                                     attachment.transferStatus == TransferStatus.PENDING


                    AttachmentPreviewData(
                        id = attachment.id,
                        fileName = attachment.fileName,
                        fileType = attachment.fileType,
                        fileUri = uri,
                        isTransferring = isProcessing,
                        progress = activeTransfer?.second ?: 0
                    )

                }

            val rawTimestamp = message.timestamp
            val currentDate = if (rawTimestamp.length >= 10) rawTimestamp.substring(0, 10) else ""

            val displayDate = if (currentDate.isNotEmpty() && currentDate != lastDate) {
                lastDate = currentDate
                currentDate
            } else {
                ""
            }

            val chatMessage = message.copy(timestamp = formatTimestamp(rawTimestamp))
            ChatMessageUIExtended(
                message = chatMessage,
                date = displayDate,
                userName = sender?.name ?: "Unknown",
                isSentByMe = message.senderId == myId,
                isDelivered = false,
                isRead = false,
                attachmentPreviewData = attachmentPreviews
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatGroup: StateFlow<ChatGroup?> = conversationModel.getChatGroupById(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val usersInGroup: StateFlow<List<User>> = chatGroup.flatMapLatest { group ->
        getUsersInGroup(group)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        chatGroup
            .onEach { group ->
                if (group != null) {
                    chatModel.setActiveChatGroup(group)
                }
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(messageText: String) {
        viewModelScope.launch {
            chatModel.sendMessage(messageText, chatId)
        }
    }

    fun sendMessageWithAttachment(messageText: String, uris: List<Uri>) {
        viewModelScope.launch {
            chatModel.sendMessageWithAttachment(messageText, chatId, uris)
        }
    }

    fun getAttachedItems(uris: List<Uri>): List<AttachmentPreviewData> {
        return attachmentModel.getAttachmentPreviewData(uris)
    }

    fun getUsersInGroup(chatGroup: ChatGroup?): Flow<List<User>> {
        val userIds = chatGroup?.members?.map { member ->
            member.userId
        } ?: emptyList()
        if (userIds.isEmpty()) return flowOf(emptyList())
        return chatModel.getUsersById(userIds)
    }
    
    fun formatTimestamp(timestamp: String): String {
        return try {
            if (timestamp.length >= 16) {
                timestamp.substring(11, 16)
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}
