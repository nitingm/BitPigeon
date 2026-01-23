package com.codingskillshub.bitpigeon.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupDb
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.ui.composables.ChatEntry
import com.codingskillshub.bitpigeon.ui.viewmodels.ChatGroupListViewModel
import com.codingskillshub.bitpigeon.ui.composables.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchChatGroupView(
    navController: NavController,
    viewModel: ChatGroupListViewModel = viewModel()
) {
    // Collect the filtered conversations and the current query from the ViewModel
    val filteredConversations by viewModel.filteredConversations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()

    SearchChatGroupViewContent(
        filteredConversations,

        searchQuery = searchQuery,
        onChatClick = { chatId ->
            navController.navigate("chatview/$chatId")
        },
        onQueryChange = { it ->
            viewModel.onSearchQueryChanged(it)
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
}

@Composable
fun SearchChatGroupViewContent(
    filteredConversations: List<ChatGroup>,
    searchQuery: String,
    onChatClick: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            // Using the SearchBar we customized earlier
            SearchBar(
                isSearchView = true,
                query = searchQuery,
                onQueryChange = { onQueryChange(it) },
                onBackClick = {
                    onBackClick()
                },
                placeholderText = "Search people or 'me'..."
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(items = filteredConversations,
                key = { chat -> chat.group.id }) { chatGroup ->
                ChatEntry(
                    name = chatGroup.group.name,
                    lastMessage = "",
                    timestamp = "",
                    onClick = { onChatClick(chatGroup.group.id) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun SearchChatGroupViewPreview() {
    val sampleChats = listOf(
        ChatGroup(ChatGroupDb ( "1", "Aman Gupta", ChatGroupType.DIRECT ), emptyList(),"Got the files!", "27/12/2025"),
        ChatGroup(ChatGroupDb ("2", "John Doe", ChatGroupType.DIRECT), emptyList(),"Are you online?", "26/12/2025"),
        ChatGroup(ChatGroupDb ("3", "Project Group", ChatGroupType.DIRECT),emptyList(), "Meeting at 5 PM", "25/12/2025"),
        ChatGroup(ChatGroupDb ("4", "Mama", ChatGroupType.DIRECT),emptyList(), "Call me later", "24/12/2025"),
        ChatGroup(ChatGroupDb ("5", "BitPigeon Support", ChatGroupType.DIRECT),emptyList(), "Welcome to the app!", "20/12/2025")
    )
    SearchChatGroupViewContent(
        filteredConversations = sampleChats,
        searchQuery = "Aman",
        onChatClick = {},
        onQueryChange = {},
        onBackClick = {}
    )
}