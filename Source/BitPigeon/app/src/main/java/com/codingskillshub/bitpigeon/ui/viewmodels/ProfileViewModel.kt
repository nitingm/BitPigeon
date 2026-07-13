package com.codingskillshub.bitpigeon.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
import com.codingskillshub.bitpigeon.domain.models.AppSystemModel
import com.codingskillshub.bitpigeon.domain.models.AttachmentModel
import com.codingskillshub.bitpigeon.infrastructure.CropBounds
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import com.codingskillshub.bitpigeon.infrastructure.ImageCroppingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val configurationService: ConfigurationService,
    private val appSystemModel: AppSystemModel,
    private val attachmentModel: AttachmentModel
) : ViewModel() {

    val userName: StateFlow<String> = configurationService.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultUser")

    val phoneNumber: StateFlow<String> = configurationService.phoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultPhone")

    val emailAddress: StateFlow<String> = configurationService.emailAddressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultEmail")

    val statusLabel: StateFlow<String> = configurationService.statusLabel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultStatus")

    val profilePictureUri: StateFlow<String?> = configurationService.profilePicture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUserName(name: String) {
        appSystemModel.updateUserName(name)
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

    // Profile picture cropping states and methods
    private val _isSavingProfilePicture = MutableStateFlow(false)
    val isSavingProfilePicture: StateFlow<Boolean> = _isSavingProfilePicture.asStateFlow()

    /**
     * Save a cropped profile picture
     * @param sourceUri URI of the source image
     * @param cropBounds The crop bounds in image pixel coordinates
     * @param onSuccess Callback when save completes successfully
     */
    fun saveCroppedProfilePicture(
        sourceUri: String,
        // cropBounds are in display (composable) coordinates
        cropBounds: CropBounds,
        displayWidth: Int,
        displayHeight: Int,
        onSuccess: () -> Unit = {}
    ) {
        _isSavingProfilePicture.value = true
        appSystemModel.saveCroppedProfilePicture(
            sourceUri,
            cropBounds,
            displayWidth,
            displayHeight,
            {
                onSuccess()
                _isSavingProfilePicture.value = false
            }
        )
    }

    fun toAttachmentData(uri: Uri): AttachmentPreviewData {
        return attachmentModel.getAttachmentPreviewDataForUri(uri)
    }
}
