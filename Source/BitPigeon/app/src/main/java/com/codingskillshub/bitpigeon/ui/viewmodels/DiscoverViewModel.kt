package com.codingskillshub.bitpigeon.ui.viewmodels

import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val wifiService: WifiCommunicationService,
    private val onlineChatService: OnlineChatService,
    private val conversationModel: ConversationModel
) : ViewModel() {
    val discoveredUsers: StateFlow<Map<String, Pair<User, WifiP2pDevice>>> = wifiService.discoveredUsers

    val availableClients: StateFlow<List<Client>> = onlineChatService.availablePeerClients

    var onChatGroupInvoked: ((String) -> Unit)? = null

    fun connectToPeer(peer: WifiP2pDevice) {
        viewModelScope.launch {
            wifiService.connectToPeer(peer)
        }
    }

    fun createAndOpenDirectChat(user: User) {
        viewModelScope.launch {
            val groupId = conversationModel.createDirectChat(user)
            onChatGroupInvoked?.invoke(groupId)
        }
    }
}