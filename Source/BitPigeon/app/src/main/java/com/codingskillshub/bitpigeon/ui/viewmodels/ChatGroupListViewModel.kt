package com.codingskillshub.bitpigeon.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.entities.ChatGroup
import com.codingskillshub.bitpigeon.domain.entities.ChatGroupType
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatGroupListViewModel @Inject constructor(
    private val conversationModel: ConversationModel
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val onlineChatGroupsIds: StateFlow<List<String>> = conversationModel.onlineGroups
        .map { chatGroups -> chatGroups.map { it.group.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {

    }

    /**
     * Raw data stream from the Model.
     * We convert the Flow from the Model into a StateFlow here so it stays active
     * while the ViewModel is alive.
     */

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ChatGroup>> = conversationModel.getAllConversations()
        .flatMapLatest { list: List<ChatGroup> ->
            if (list.isEmpty()) {
                flowOf(emptyList<ChatGroup>())
            } else {
                val flows: List<Flow<ChatGroup>> = list.map { chat ->
                    if (chat.group.type == ChatGroupType.DIRECT) {
                        // assumes userDao.getUserById returns Flow<User?>
                        conversationModel.getUserById(chat.group.name)
                            .map { user -> 
                                if (user != null) chat.copy(group = chat.group.copy(name = user.name)) else chat 
                            }
                    } else {
                        flowOf(chat)
                    }
                }
                combine(flows) { it.toList() }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * A filtered stream of conversations based on the search query.
     * Includes a special case: if query is "me", "you", or "myself",
     * it filters for groups where the current user is the only member.
     */
    val filteredConversations: StateFlow<List<ChatGroup>> = combine(
        conversations,
        _searchQuery
    ) { allConversations, query ->
        val trimmedQuery = query.trim().lowercase()

        if (trimmedQuery.isEmpty()) {
            allConversations
        } else if (trimmedQuery in listOf("me", "you", "myself")) {
            val personalChat = allConversations.find { chat ->
                chat.group.type == ChatGroupType.PERSONAL
            }

            if (personalChat == null) {
                Log.d("ChatGroupListViewModel", "Personal Chat not available")
                viewModelScope.launch {
                    // Create a "Personal" User object representing the self
                    conversationModel.createMyPersonalChat()
                }
                emptyList()
            } else {
                listOfNotNull(personalChat)
            }
        } else {
            // Standard Case: Filter by group name
            allConversations.filter {
                it.group.name.lowercase().contains(trimmedQuery)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
