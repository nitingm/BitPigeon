package com.codingskillshub.bitpigeon.ui.viewmodels

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.AttachmentPreviewData
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
    private val imageCroppingService: ImageCroppingService,
    private val fileStorageService: FileStorageService,
    private val attachmentModel: AttachmentModel
) : ViewModel() {

    init {
        // Initialize profile picture URI from stored userId filename if available
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var userId = configurationService.userIdFlow.firstOrNull()

                userId?.let {
                    val filePair = fileStorageService.getPrivateFileByName(userId)
                    filePair?.let { found -> _profilePictureUri.value = found.second }
                }
            } catch (e: Exception) {
                // ignore initialization errors
            }
        }
    }

    val userName: StateFlow<String> = configurationService.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultUser")

    val phoneNumber: StateFlow<String> = configurationService.phoneNumberFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultPhone")

    val emailAddress: StateFlow<String> = configurationService.emailAddressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultEmail")

    val statusLabel: StateFlow<String> = configurationService.statusLabel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DefaultStatus")

    // Profile picture URI state
    private val _profilePictureUri = MutableStateFlow<Uri?>(null)
    val profilePictureUri: StateFlow<Uri?> = _profilePictureUri.asStateFlow()

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

    // Profile picture cropping states and methods
    private val _isSavingProfilePicture = MutableStateFlow(false)
    val isSavingProfilePicture: StateFlow<Boolean> = _isSavingProfilePicture.asStateFlow()

    private val _profilePictureError = MutableStateFlow<String?>(null)
    val profilePictureError: StateFlow<String?> = _profilePictureError.asStateFlow()

    /**
     * Save a cropped profile picture
     * @param sourceUri URI of the source image
     * @param fileName Name to save the file as
     * @param cropBounds The crop bounds in image pixel coordinates
     * @param isPrivateStorage Whether to save to private app storage (default true)
     * @param onSuccess Callback when save completes successfully
     */
    fun saveCroppedProfilePicture(
        sourceUri: String,
        fileName: String,
        // cropBounds are in display (composable) coordinates
        cropBounds: CropBounds,
        displayWidth: Int,
        displayHeight: Int,
        isPrivateStorage: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSavingProfilePicture.value = true
                _profilePictureError.value = null

                // Load the bitmap from URI
                val bitmap = imageCroppingService.loadBitmap(sourceUri.toUri())
                    ?: throw Exception("Failed to load image")
                // Convert display crop bounds to bitmap pixel coordinates
                val dw = if (displayWidth > 0) displayWidth else bitmap.width
                val dh = if (displayHeight > 0) displayHeight else bitmap.height

                val scaleX = bitmap.width.toFloat() / dw.toFloat()
                val scaleY = bitmap.height.toFloat() / dh.toFloat()

                val leftPx = (cropBounds.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                val topPx = (cropBounds.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                val widthPx = (cropBounds.width * scaleX).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - leftPx)
                val heightPx = (cropBounds.height * scaleY).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - topPx)

                val bitmapCropBounds = CropBounds(
                    left = leftPx.toFloat(),
                    top = topPx.toFloat(),
                    width = widthPx.toFloat(),
                    height = heightPx.toFloat()
                )

                // Determine filename based on current userId (override provided fileName)
                var userId = configurationService.userIdFlow.firstOrNull()
                if (userId.isNullOrBlank()) {
                    configurationService.generateAndSaveUserId()
                    userId = configurationService.userIdFlow.firstOrNull()
                }
                
                // Append timestamp to the filename to force reload (cache busting)
                val finalFileName = "${userId ?: "user"}_${System.currentTimeMillis()}.jpg"
                val oldUri = _profilePictureUri.value

                // Get output stream from FileStorageService (private storage requested)
                val (outputStream, fileUri) = fileStorageService.getOutputStream(finalFileName, isPrivateStorage)

                // Crop and save the image using bitmap pixel coordinates
                val success = imageCroppingService.cropAndSaveSquareImage(
                    bitmap,
                    bitmapCropBounds,
                    outputStream,
                    quality = 90
                )

                if (success) {
                    outputStream.close()
                    // Only finalize if using public storage (MediaStore); private storage doesn't need finalize
                    if (!isPrivateStorage) {
                        fileStorageService.finalizeFile(fileUri)
                    }
                    _isSavingProfilePicture.value = false
                    
                    // Delete old profile picture file if it exists to avoid accumulation
                    oldUri?.let { fileStorageService.deletePrivateFileByUri(it) }

                    // Update internal profile picture URI to the saved file
                    fileUri?.let { _profilePictureUri.value = it }
                    onSuccess()
                } else {
                    throw Exception("Failed to crop and save image")
                }
            } catch (e: Exception) {
                _isSavingProfilePicture.value = false
                _profilePictureError.value = e.message ?: "Unknown error occurred"
                e.printStackTrace()
            }
        }
    }

    fun toAttachmentData(uri: Uri): AttachmentPreviewData {
        return attachmentModel.getAttachmentPreviewDataForUri(uri)
    }
}
