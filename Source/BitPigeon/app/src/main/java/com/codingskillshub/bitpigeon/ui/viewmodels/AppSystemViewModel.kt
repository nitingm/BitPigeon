package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.domain.models.AppSystemModel
import com.codingskillshub.bitpigeon.domain.services.IntentService
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
    private val wifiService: WifiCommunicationService,
    private val intentService: IntentService
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

    val isOnboardingCompleted: StateFlow<Boolean?> = appSystemModel.isOnboardingCompleted()
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            appSystemModel.completeOnboarding()
        }
    }

    fun getAppVersionDetail(): String {
        return "Version ${appSystemModel.getAppVersion()}"
    }

    fun changeAppTheme(theme: Pair<String, String>) {
        viewModelScope.launch {
            appSystemModel.changeAppTheme(theme.first)
        }
    }

    fun openPrivacyPolicy() {
        val privacyPolicyUrl = "https://sites.google.com/view/bitpigeon/home"
        intentService.openUrlWithExternalApp(privacyPolicyUrl)
    }
    
    fun openTermsAndConditions() {
    	val tandcUrl = "https://sites.google.com/view/bitpigeon/terms-and-conditions"
    	intentService.openUrlWithExternalApp(tandcUrl)
    }
    
    fun openLicenseAndCertificates() {
    	val licenseUrl = "https://sites.google.com/view/bitpigeon/license-and-certificates"
    	intentService.openUrlWithExternalApp(licenseUrl)
    }
}
