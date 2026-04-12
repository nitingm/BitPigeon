package com.codingskillshub.bitpigeon.ui.viewmodels

import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val wifiService: WifiCommunicationService,
    private val conversationModel: ConversationModel
) : ViewModel() {
    val peersList = wifiService.peersList

    fun connectToPeer(peer: WifiP2pDevice) {
        viewModelScope.launch {
            wifiService.connectToPeer(peer)
        }
    }
}