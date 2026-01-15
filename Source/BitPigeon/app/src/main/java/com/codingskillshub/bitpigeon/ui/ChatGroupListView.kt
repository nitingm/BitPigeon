package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatGroupListViewModel
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.ui.composables.ChatEntry
import com.codingskillshub.bitpigeon.ui.composables.SearchBar

@Composable
fun ChatGroupListView(
    chatList: List<ChatGroup>,
    onChatClick: (ChatGroup) -> Unit,
    modifier: Modifier = Modifier,
    chatGroupListViewModel: ChatGroupListViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize()) {
        // Add the SearchBar at the top
        SearchBar(
            query = searchQuery,
            onQueryChange = {},
            onSearchClick = { /* Optional: handle focus or navigation */ }
        )
        LazyColumn(
            modifier = modifier.weight(1f), // Takes up remaining space,
            // Adds spacing at the top and bottom of the list
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 'items' handles the recycling and lazy loading automatically
            items(
                items = chatList,
                // Providing a 'key' helps Compose optimize list updates/reordering
                key = { chat -> chat.id }
            ) { chat ->
                ChatEntry(
                    name = chat.name,
                    lastMessage = chat.lastMessage,
                    timestamp = chat.timestamp,
                    onClick = { onChatClick(chat) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatGroupListViewPreview() {
    val sampleChats = listOf(
        ChatGroup("1", "Aman Gupta",  ChatGroupType.DIRECT, "Got the files!", "27/12/2025"),
        ChatGroup("2", "John Doe", ChatGroupType.DIRECT,"Are you online?", "26/12/2025"),
        ChatGroup("3", "Project Group", ChatGroupType.DIRECT, "Meeting at 5 PM", "25/12/2025"),
        ChatGroup("4", "Mama", ChatGroupType.DIRECT, "Call me later", "24/12/2025"),
        ChatGroup("5", "BitPigeon Support", ChatGroupType.DIRECT, "Welcome to the app!", "20/12/2025")
    )

    MaterialTheme {
        ChatGroupListView(
            chatList = sampleChats,
            onChatClick = { /* Handle navigation */ }
        )
    }
}