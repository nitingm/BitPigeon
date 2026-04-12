package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.PrimaryKey
import com.codingskillshub.bitpigeon.domain.entities.ChatMessage
import com.codingskillshub.bitpigeon.domain.entities.ChatMessageUIExtented
import com.codingskillshub.bitpigeon.domain.entities.MessageData

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

    ChatViewContent(
        chatPartnerName = chatGroup?.group?.name?: "Unknown",
        messages = messages,
        onSendMessage = {
            messageText -> chatViewModel.sendMessage(messageText)
        },
        onBackClick = {
            navController.popBackStack()
        }
    )

}

@Composable
fun ChatViewContent(
    chatPartnerName: String,
    messages: List<ChatMessageUIExtented>,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit
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
                subtitle = "Active via Wi-Fi Direct",
                showLeadingImage = true,
                onNavigationClick = {
                    onBackClick()
                }
            )
        },
        bottomBar = {
            // Padding used to prevent keyboard overlap in modern Android
            Column(modifier = Modifier.imePadding()) {
                MessageInputBar(onSendMessage = { text -> onSendMessage(text)})
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
                key = { it.message.timestamp }
            ) { message ->
                MessageBubble(
                    senderName = message.userName,
                    messageText = message.message.data.text,
                    timestamp = message.message.timestamp,
                    isSentByMe = message.isSentByMe,
                    // Only show header if it's the first message or sender changed
                    showHeader = !message.isSentByMe
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatViewPreview() {
    val dummyMessages = listOf(
        ChatMessageUIExtented(
            ChatMessage(
                "1",
                "2",
                "2",
                MessageData("Hey! Is the Wi-Fi P2P working?", emptyList()),
                "21:21"
                ),
            "John",
            true,
            true,
            true
        ),
        ChatMessageUIExtented(
            ChatMessage(
                "1",
                "2",
                "2",
                MessageData("Yes, just finished the ChatView implementation.", emptyList()),
                "21:21"
            ),
            "Murphy",
            false,
            true,
            false
        ),
        ChatMessageUIExtented(
            ChatMessage(
                "1",
                "2",
                "2",
                MessageData("Awesome, try sending an emoji! 🚀", emptyList()),
                "21:21"
            ),
            "John",
            true,
            false,
            false
        ),
        ChatMessageUIExtented(
            ChatMessage(
                "1",
                "2",
                "2",
                MessageData("Working perfectly fine. 👍", emptyList()),
                "21:21"
            ),
            "Murphy",
            false,
            true,
            false
        )
    )

    ChatViewContent(
        chatPartnerName = "Aman Gupta",
        messages = dummyMessages,
        {},
        {}
    )
}
