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

import com.codingskillshub.bitpigeon.ui.composables.MessageInputBar
import com.codingskillshub.bitpigeon.ui.composables.MessageBubble
import com.codingskillshub.bitpigeon.ui.composables.ViewHeader
import com.codingskillshub.bitpigeon.ui.viewmodels.AppSystemViewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatViewModel

// Simple data model for the messages
data class MessageData(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isMe: Boolean
)

@Composable
fun ChatView(
    navController: NavController,
    chatPartnerName: String,
    messages: List<MessageData>,
    systemViewModel: AppSystemViewModel,
    chatViewModel: ChatViewModel,
) {
    // List state to handle auto-scrolling or scroll position
    val listState = rememberLazyListState()

    // Collect states from ViewModel
//    val chatList by androidx.lifecycle.viewmodel.compose.viewModel.chatList.collectAsState()
//    val searchQuery by androidx.lifecycle.viewmodel.compose.viewModel.searchQuery.collectAsState()


    Scaffold(
        topBar = {
            ViewHeader(
                title = chatPartnerName,
                subtitle = "Active via Wi-Fi Direct",
                showLeadingImage = true,
                onNavigationClick = {
                    navController.popBackStack()
                }
            )
        },
        bottomBar = {
            // Padding used to prevent keyboard overlap in modern Android
            Column(modifier = Modifier.imePadding()) {
                MessageInputBar(onSendMessage = { text -> chatViewModel.sendMessage(text)})
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
                key = { it.id }
            ) { message ->
                MessageBubble(
                    senderName = message.sender,
                    messageText = message.text,
                    timestamp = message.time,
                    isSentByMe = message.isMe,
                    // Only show header if it's the first message or sender changed
                    showHeader = !message.isMe
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatViewPreview() {
    val dummyMessages = listOf(
        MessageData("1", "Aman Gupta", "Hey! Is the Wi-Fi P2P working?", "10:00", false),
        MessageData("2", "Me", "Yes, just finished the ChatView implementation.", "10:01", true),
        MessageData("3", "Aman Gupta", "Awesome, try sending an emoji! 🚀", "10:02", false),
        MessageData("4", "Me", "Working perfectly fine. 👍", "10:03", true),
    )

    ChatView(
        chatPartnerName = "Aman Gupta",
        messages = dummyMessages,
        navController = NavController(LocalContext.current),
        systemViewModel = viewModel(),
        chatViewModel = viewModel()
    )
}
