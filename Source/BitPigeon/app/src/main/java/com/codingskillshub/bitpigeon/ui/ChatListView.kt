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
import com.codingskillshub.bitpigeon.models.ChatData
import com.codingskillshub.bitpigeon.ui.composables.ChatEntry
import com.codingskillshub.bitpigeon.ui.composables.SearchBar

@Composable
fun ChatListView(
    chatList: List<ChatData>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onChatClick: (ChatData) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Add the SearchBar at the top
        SearchBar(
            query = searchQuery,
            onQueryChange = onQueryChange,
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
                    name = chat.senderName,
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
fun ChatListViewPreview() {
    var query by remember { mutableStateOf("") }
    val sampleChats = listOf(
        ChatData("1", "Aman Gupta", "Got the files!", "27/12/2025"),
        ChatData("2", "John Doe", "Are you online?", "26/12/2025"),
        ChatData("3", "Project Group", "Meeting at 5 PM", "25/12/2025"),
        ChatData("4", "Mama", "Call me later", "24/12/2025"),
        ChatData("5", "BitPigeon Support", "Welcome to the app!", "20/12/2025")
    )

    MaterialTheme {
        ChatListView(
            chatList = sampleChats,
            searchQuery = query,
            onQueryChange = { query = it },
            onChatClick = { /* Handle navigation */ }
        )
    }
}