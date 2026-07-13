package com.codingskillshub.bitpigeon.ui.viewmodels

import android.util.Log
import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.Client
import com.codingskillshub.bitpigeon.domain.entities.User
import com.codingskillshub.bitpigeon.domain.models.ConversationModel
import com.codingskillshub.bitpigeon.domain.models.DiscoveryModel
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.QRCodeService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.domain.types.WifiDirectPeer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val wifiService: WifiCommunicationService,
    private val onlineChatService: OnlineChatService,
    private val configurationService: ConfigurationService,
    private val conversationModel: ConversationModel,
    private val discoveryModel: DiscoveryModel
) : ViewModel() {
    val nearbyPeers: StateFlow<List<WifiDirectPeer>> = discoveryModel.nearbyPeers
    val availableClients: StateFlow<List<Client>> = onlineChatService.availablePeerClients

    val groupOwnerName: StateFlow<String> = combine(
        availableClients,
        configurationService.userNameFlow
    ) { clients, userName ->
        if (clients.isNotEmpty()) {
            clients.find { it.isGroupOwner }?.user?.name ?: userName
        } else {
            ""
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val isGroupOwner: StateFlow<Boolean> = combine(
        availableClients,
        configurationService.userIdFlow
    ) { clients, userId ->
        clients.any { it.user.id == userId && it.isGroupOwner }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )


    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val isWifiEnabled: StateFlow<Boolean> = wifiService.isWifiEnabled

    val isWifiDirectServiceAdvertisingEnabled: StateFlow<Boolean> = wifiService.isWifiDirectServiceAdvertisingEnabled

    val isConnectedToGroup: StateFlow<Boolean> = wifiService.connectionInfo
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _showQrPopup = MutableStateFlow(false)
    val showQrPopup: StateFlow<Boolean> = _showQrPopup.asStateFlow()

    private val _showScanner = MutableStateFlow(false)
    val showScanner: StateFlow<Boolean> = _showScanner.asStateFlow()

    private val _qrPayloadText = MutableStateFlow("")
    val qrPayloadText: StateFlow<String> = _qrPayloadText.asStateFlow()

    private val _isQrValid = MutableStateFlow(false)
    val isQrValid: StateFlow<Boolean> = _isQrValid.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private var lastRefreshTime: Long = 0
    private val refreshDebounceMs = 5000L

    var onChatGroupInvoked: ((String) -> Unit)? = null

    fun connectToPeer(peer: WifiDirectPeer) {
        viewModelScope.launch {
            discoveryModel.connectToNearbyPeer(peer)
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

    fun showQrPopup() {
        viewModelScope.launch {
            _scanError.value = null

            val qrPayload = discoveryModel.prepareQrPayloadText()
            _qrPayloadText.value = qrPayload.first
            _isQrValid.value = qrPayload.second
            if (!_isQrValid.value) {
                _qrPayloadText.value = "Internal Error\nInvalid QR"
            }
            _showQrPopup.value = true
        }
    }

    fun hideQrPopup() {
        _showQrPopup.value = false
    }

    fun openScanner() {
        _scanError.value = null
        _showScanner.value = true
    }

    fun hideScanner() {
        _showScanner.value = false
    }

    fun handleScanResult(qrText: String) {
        viewModelScope.launch {
            _showScanner.value = false
            _scanError.value = null
            val connected = discoveryModel.connectToPeerFromPayloadText(qrText)
            if (!connected) {
                _scanError.value = "Unable to connect with scanned QR data. Please ensure the device is nearby and try again."
            }
        }
    }

    fun exitGroup() {
        if (isGroupOwner.value) {
            onlineChatService.destroyGroup()
        } else {
            onlineChatService.disconnectFromGroup()
        }
        discoveryModel.exitGroup()
    }

    fun switchAdvertising(enabled: Boolean) {
        wifiService.switchWifiDirectServiceAdvertising(enabled)
    }
}
