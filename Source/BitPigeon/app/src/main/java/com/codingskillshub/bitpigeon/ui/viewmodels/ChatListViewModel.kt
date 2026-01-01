package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.codingskillshub.bitpigeon.models.ChatData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class ChatListViewModel : ViewModel() {

    // 1. Raw Data (usually fetched from a database or Wi-Fi service)
    private val _allChats = MutableStateFlow<List<ChatData>>(getDummyData())

    // 2. Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 3. Filtered List Logic
    // This combines the list and the query to produce a filtered list automatically
    val chatList: StateFlow<List<ChatData>> = combine(_allChats, _searchQuery) { chats, query ->
        if (query.isEmpty()) {
            chats
        } else {
            chats.filter {
                it.senderName.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }.let {
        // We use StateFlow to ensure the UI has an initial value
        MutableStateFlow(getDummyData())
    }
    /* Note: In a production app, you'd use .stateIn(viewModelScope) here */

    // Function to update search query from UI
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun getDummyData(): List<ChatData> {
        return listOf(
            ChatData("1", "Aman Gupta", "Is the Wi-Fi P2P working?", "10:00"),
            ChatData("2", "John Doe", "Sent the zip file.", "Yesterday"),
            ChatData("3", "Dev Team", "K2 compiler is fast!", "25 Dec")
        )
    }
}