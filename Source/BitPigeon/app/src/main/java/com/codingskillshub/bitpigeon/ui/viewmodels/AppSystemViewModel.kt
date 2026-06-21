package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.models.AppSystemModel
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    val appTheme: StateFlow<Pair<String, String>> = appSystemModel.getAppTheme()
        .map { theme ->
            when (theme) {
                "LIGHT" -> "LIGHT" to "Light"
                "DARK" -> "DARK" to "Dark"
                "SYSTEM_DEFAULT" -> "SYSTEM_DEFAULT" to "System Default"
                else -> "SYSTEM_DEFAULT" to "System Default"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SYSTEM_DEFAULT" to "System Default"
        )

    val availableAppThemes: List<Pair<String, String>> = listOf(
        "LIGHT" to "Light",
        "DARK" to "Dark",
        "SYSTEM_DEFAULT" to "System Default"
    )

    fun getAppVersionDetail(): String {
        return "Version ${appSystemModel.getAppVersion()}"
    }

    fun changeAppTheme(theme: Pair<String, String>) {
        viewModelScope.launch {
            appSystemModel.changeAppTheme(theme.first)
        }
    }
}
