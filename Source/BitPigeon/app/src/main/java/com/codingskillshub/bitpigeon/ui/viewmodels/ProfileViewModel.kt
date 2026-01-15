package com.codingskillshub.bitpigeon.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val configurationService: ConfigurationService
) : ViewModel() {

    val userName: StateFlow<String> = configurationService.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultUser")

    val phoneNumber: StateFlow<String> = configurationService.phoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultPhone")

    val emailAddress: StateFlow<String> = configurationService.emailAddressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultEmail")

    val statusLabel: StateFlow<String> = configurationService.statusLabel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultStatus")

    fun updateUserName(name: String) {
        viewModelScope.launch {
            configurationService.updateUserName(name)
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            configurationService.updatePhoneNumber(phoneNumber)
        }
    }

    fun updateEmailAddress(email: String) {
        viewModelScope.launch {
            configurationService.updateEmailAddress(email)
        }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch {
            configurationService.updateStatus(status)
        }
    }
}