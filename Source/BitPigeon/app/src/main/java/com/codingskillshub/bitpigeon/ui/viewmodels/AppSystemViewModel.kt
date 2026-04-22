package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.codingskillshub.bitpigeon.domain.models.AppSystemModel
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppSystemViewModel @Inject constructor(
    private val appSystemModel: AppSystemModel,
    private val wifiService: WifiCommunicationService
) : ViewModel() {
    // Example global states
    val isWifiEnabled = wifiService.isWifiEnabled

    val deviceName = wifiService.deviceName

    // Service discovery
    val discoveredUsers = wifiService.discoveredUsers
}
