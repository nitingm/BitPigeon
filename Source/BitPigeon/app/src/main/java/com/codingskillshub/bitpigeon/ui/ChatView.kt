package com.codingskillshub.bitpigeon.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.ChatMessageUIExtended
import com.codingskillshub.bitpigeon.domain.entities.MessageData
import com.codingskillshub.bitpigeon.ui.composables.AttachmentPreviewBanner

import com.codingskillshub.bitpigeon.ui.composables.MessageInputBar
import com.codingskillshub.bitpigeon.ui.composables.MessageBubble
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatViewModel

@Composable
fun ChatView(
    navController: NavController,
    systemViewModel: AppSystemViewModel,
    chatViewModel: ChatViewModel
) {
    val messages by chatViewModel.messages.collectAsState()
    val chatGroup by chatViewModel.chatGroup.collectAsState()
    val isChatOnline by chatViewModel.isChatOnline.collectAsStateWithLifecycle()

    val attachmentUris = remember { mutableStateListOf<Uri>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        attachmentUris.addAll(uris)
    }

    ChatViewContent(
        chatPartnerName = chatGroup?.group?.name ?: "Unknown",
        chatProfilePictureUri = chatGroup?.group?.profilePicture?: "",
        isChatOnline = isChatOnline,
        messages = messages,
        attachedItems = chatViewModel.getAttachedItems(attachmentUris),
        onSendMessage = { messageText -> run {
                if (attachmentUris.isEmpty()) {
                    chatViewModel.sendMessage(messageText)
                } else {
                    // Handle attachment sending
                    chatViewModel.sendMessageWithAttachment(messageText, attachmentUris.toList())
                }
                attachmentUris.clear()
            }
        },
        onAttachClick = {
            filePickerLauncher.launch("*/*")
        },
        onBackClick = {
            navController.popBackStack()
        },
        onTitleClick = {
            navController.navigate("chat_group_detail/${chatGroup?.group?.id}")
        },
        onMediaClick = { attachment ->
            val isImage = attachment.fileType.startsWith("image/")
            val isVideo = attachment.fileType.startsWith("video/")
            if (isImage || isVideo) {
                navController.navigate("media_view/${chatGroup?.group?.id}?mediaId=${attachment.id}")
            } else {
                chatViewModel.openAttachmentWithExternalApp(attachment)
            }
        }
    )

}

@Composable
fun ChatViewContent(
    chatPartnerName: String,
    chatProfilePictureUri: String,
    isChatOnline: Boolean,
    messages: List<ChatMessageUIExtended>,
    attachedItems: List<AttachmentPreviewData>,
    onSendMessage: (String) -> Unit,
    onAttachClick: () -> Unit,
    onBackClick: () -> Unit,
    onTitleClick: () -> Unit,
    onMediaClick: (AttachmentPreviewData) -> Unit,
) {
    // List state to handle auto-scrolling or scroll position
    val listState = rememberLazyListState()

    // Auto-scroll to latest message when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ViewHeader(
                title = chatPartnerName,
                subtitle = if (isChatOnline) "Online" else "Offline",
                showLeadingImage = true,
                leadingImageUri  = chatProfilePictureUri,
                onNavigationClick = {
                    onBackClick()
                },
                onTitleRowClicked = {
                    onTitleClick()
                },
            )
        },
        bottomBar = {
            // Padding used to prevent keyboard overlap in modern Android
            Column(modifier = Modifier.imePadding()) {
                if (attachedItems.isNotEmpty()) {
                    AttachmentPreviewBanner(
                        attachedItems = attachedItems
                    )
                }
                MessageInputBar(
                    onSendMessage = { text -> onSendMessage(text) },
                    onAttachButtonClicked = { onAttachClick() }
                )
            }
        }
    ) { innerPadding ->
        // The core list of messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 8.dp),

            reverseLayout = false // Set to true if you want messages to grow from bottom
        ) {
            items(
                items = messages,
                key = { it.message.id }
            ) { message ->
                MessageBubble(
                    senderName = message.userName,
                    messageText = message.message.data.text,
                    timestamp = message.message.timestamp,
                    isSentByMe = message.isSentByMe,
                    // Only show header if it's the first message or sender changed
                    showHeader = !message.isSentByMe,
                    showDate = !message.date.isEmpty(),
                    date = message.date,
                    imageThumbnails = message.attachmentPreviewData,
                    onMediaClick = { attachment ->
                        // Handle media click, e.g., navigate to MediaView
                        onMediaClick(attachment)
                    }
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun ChatViewPreview() {
    val dummyAttachmentPreviewData = listOf(
        AttachmentPreviewData(
            id = "att1",
            fileName = "file.txt",
            fileType = "text/plain",
            fileUri = Uri.parse("https://example.com/file.txt")
        ),
        AttachmentPreviewData(
            id = "att2",
            fileName = "image.jpg",
            fileType = "image/jpeg",
            fileUri = Uri.parse("https://example.com/image.jpg")
        )
    )
    val dummyMessages = listOf(
        ChatMessageUIExtended(
            ChatMessage(
                "1",
                "2",
                "2",
                MessageData("Hey! Is the Wi-Fi P2P working?", emptyList()),
                "21:21"
                ),
            "",
            "John",
            true,
            true,
            true
        ),
        ChatMessageUIExtended(
            ChatMessage(
                "2",
                "2",
                "2",
                MessageData("Yes, just finished the ChatView implementation.", emptyList()),
                "21:21"
            ),
            "",
            "Murphy",
            false,
            true,
            false
        ),
        ChatMessageUIExtended(
            ChatMessage(
                "3",
                "2",
                "2",
                MessageData("Awesome, try sending an emoji! 🚀", emptyList()),
                "21:21"
            ),
            "",
            "John",
            true,
            false,
            false
        ),
        ChatMessageUIExtended(
            ChatMessage(
                "4",
                "2",
                "2",
                MessageData("Working perfectly fine. 👍", emptyList()),
                "21:21"
            ),
            "",
            "Murphy",
            false,
            true,
            false
        )
    )

    ChatViewContent(
        chatPartnerName = "Aman Gupta",
        "",
        true,
        messages = dummyMessages,
        dummyAttachmentPreviewData,
        {},
        {},
        {},
        {},
        {}
    )
}
