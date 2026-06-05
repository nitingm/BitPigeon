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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatGroupListViewModel
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupDb
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.ui.composables.ChatEntry
import com.codingskillshub.bitpigeon.ui.composables.ImageViewOverlay
import com.codingskillshub.bitpigeon.ui.composables.SearchBar
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatViewModel

@Composable
fun ChatGroupListView(
    navController: NavController,
    chatGroupListViewModel: ChatGroupListViewModel = viewModel()
) {
    val conversations by chatGroupListViewModel.conversations.collectAsStateWithLifecycle()

    var showProfilePictureFullscreen by remember { mutableStateOf<Boolean>(false) }
    var profilePictureUri by remember { mutableStateOf<String>("") }

    // Show the overlay if an attachment is selected
    if (showProfilePictureFullscreen) {
        ImageViewOverlay(
            fileName = "Profile picture",
            fileType = "image/jpg",
            fileUri = profilePictureUri,
            onDismiss = {
                profilePictureUri = ""
                showProfilePictureFullscreen = false
            }
        )
    }

    ChatGroupListViewContent(
        chatList = conversations,
        onlineChatGroups = chatGroupListViewModel.onlineChatGroupsIds.collectAsStateWithLifecycle().value,
        onChatClick = { chat ->
            // Handle navigation to ChatView
            navController.navigate("chatview/${chat.group.id}")
        },
        onProfilePictureClick = { profilePicture ->
            // Show the profile picture in fullscreen
            profilePictureUri = profilePicture
            showProfilePictureFullscreen = true
        },
        onSearchClick = {
            navController.navigate("search_group")
        }
    )
}

@Composable
fun ChatGroupListViewContent(
    chatList: List<ChatGroup>,
    onlineChatGroups: List<String>,
    onChatClick: (ChatGroup) -> Unit,
    onProfilePictureClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize()) {
        // Add the SearchBar at the top
        SearchBar(
            isSearchView = false,
            query = searchQuery,
            onQueryChange = {},
            onSearchClick = {
                onSearchClick()
            }
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
                key = { chat -> chat.group.id }
            ) { chat ->
                ChatEntry(
                    name = chat.group.name,
                    lastMessage = chat.lastMessage,
                    timestamp = chat.timestamp,
                    profilePictureUri = chat.group.profilePicture,
                    isOnline = (chat.group.id in onlineChatGroups),
                    onClick = { onChatClick(chat) },
                    onProfilePictureClick = { onProfilePictureClick(chat.group.profilePicture) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatGroupListViewPreview() {
    val sampleChats = listOf(
        ChatGroup(ChatGroupDb ( "1", "Aman Gupta", ChatGroupType.DIRECT ), emptyList(),"Got the files!", "27/12/2025"),
        ChatGroup(ChatGroupDb ("2", "John Doe", ChatGroupType.DIRECT), emptyList(),"Are you online?", "26/12/2025"),
        ChatGroup(ChatGroupDb ("3", "Project Group", ChatGroupType.DIRECT),emptyList(), "Meeting at 5 PM", "25/12/2025"),
        ChatGroup(ChatGroupDb ("4", "Mama", ChatGroupType.DIRECT),emptyList(), "Call me later", "24/12/2025"),
        ChatGroup(ChatGroupDb ("5", "BitPigeon Support", ChatGroupType.DIRECT),emptyList(), "Welcome to the app!", "20/12/2025")
    )

    val sampleOnlineChats = listOf("2","3")

    MaterialTheme {
        ChatGroupListViewContent(
            chatList = sampleChats,
            onlineChatGroups = sampleOnlineChats,
            onChatClick = { /* Handle navigation */ },
            onProfilePictureClick = { /* Handle profile picture click */ },
            onSearchClick = { /* Handle search */ }
        )
    }
}