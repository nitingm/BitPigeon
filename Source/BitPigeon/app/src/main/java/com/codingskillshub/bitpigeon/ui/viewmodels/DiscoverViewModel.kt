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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val isWifiEnabled: StateFlow<Boolean> = wifiService.isWifiEnabled

    val isWifiDirectServiceAdvertisingEnabled: StateFlow<Boolean> = wifiService.isWifiDirectServiceAdvertisingEnabled

    private var lastRefreshTime: Long = 0
    private val refreshDebounceMs = 5000L

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

    fun refresh() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime < refreshDebounceMs) {
            return
        }
        
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                lastRefreshTime = currentTime
                wifiService.refreshDiscovery()
            } catch (e: Exception) {
                // Handle error (e.g., log it)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun switchAdvertising(enabled: Boolean) {
        wifiService.switchWifiDirectServiceAdvertising(enabled)
    }
}
