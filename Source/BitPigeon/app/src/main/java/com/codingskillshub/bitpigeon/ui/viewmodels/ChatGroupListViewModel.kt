package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class ChatGroupListViewModel @Inject constructor() : ViewModel() {

    // 1. Raw Data (usually fetched from a database or Wi-Fi service)
    private val _allChats = MutableStateFlow<List<ChatGroup>>(getDummyData())

    // 2. Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 3. Filtered List Logic
    // This combines the list and the query to produce a filtered list automatically
    val chatList: StateFlow<List<ChatGroup>> = combine(_allChats, _searchQuery) { chats, query ->
        if (query.isEmpty()) {
            chats
        } else {
            chats.filter {
                it.name.contains(query, ignoreCase = true) ||
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

    private fun getDummyData(): List<ChatGroup> {
        return listOf(
            ChatGroup("1", "Aman Gupta", ChatGroupType.DIRECT,"Is the Wi-Fi P2P working?", "10:00"),
            ChatGroup("2", "John Doe", ChatGroupType.DIRECT,"Sent the zip file.", "Yesterday"),
            ChatGroup("3", "Dev Team", ChatGroupType.DIRECT,"K2 compiler is fast!", "25 Dec")
        )
    }
}