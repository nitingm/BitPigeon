package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.codingskillshub.bitpigeon.services.WifiCommunicationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class AppSystemViewModel @Inject constructor(
    private val wifiService: WifiCommunicationService
) : ViewModel() {
    // Example global states
    val isWifiEnabled = wifiService.isWifiEnabled

    val deviceName = wifiService.deviceName
}
