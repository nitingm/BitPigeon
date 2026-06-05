package com.codingskillshub.bitpigeon.domain.models

import androidx.core.net.toUri
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.infrastructure.CropBounds
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import com.codingskillshub.bitpigeon.infrastructure.ImageCroppingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSystemModel @Inject constructor(
    private val userDao: UserDao,
    private val imageCroppingService: ImageCroppingService,
    private val fileStorageService: FileStorageService,
    private val onlineChatService: OnlineChatService,
    private val wifiService: WifiCommunicationService,
    private val configurationService: ConfigurationService
) {
    private val modelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var myUserId = "DefaultUser"

    init {
        modelScope.launch {
            initializeUserId()
            runBlocking {
                myUserId = configurationService.userIdFlow.first()?: "DefaultUser"
            }
        }
    }

    private suspend fun initializeUserId() {
        configurationService.generateAndSaveUserId()
    }

    fun getDeviceAddress(): StateFlow<String> {
        return wifiService.deviceAddress
    }

    fun getMyUserId(): String {
        return myUserId
    }

    fun updateUserName(name: String) {
        modelScope.launch {
            configurationService.updateUserName(name)
            userDao.updateUserName(getMyUserId(), name)
            onlineChatService.sendUserInfoUpdate(userDao.getUserById(getMyUserId()).firstOrNull()!!)
        }
    }

    fun saveCroppedProfilePicture(
        sourceUri: String,
        // cropBounds are in display (composable) coordinates
        cropBounds: CropBounds,
        displayWidth: Int,
        displayHeight: Int,
        onSuccess: (Uri) -> Unit = {}
    ) {
        modelScope.launch(Dispatchers.IO) {
            try {

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

                val userId = getMyUserId()

                // Append timestamp to the filename to force reload (cache busting)
                val finalFileName = "${userId}_${System.currentTimeMillis()}.jpg"

                // Get output stream from FileStorageService (private storage requested)
                val (outputStream, fileUri) = fileStorageService.getOutputStream(finalFileName, true)

                // Crop and save the image using bitmap pixel coordinates
                val success = imageCroppingService.cropAndSaveSquareImage(
                    bitmap,
                    bitmapCropBounds,
                    outputStream,
                    quality = 90
                )

                if (success) {
                    outputStream.close()
                    fileUri?.let { onSuccess(it) }
                    userDao.updateProfilePicture(userId, fileUri.toString())
                } else {
                    throw Exception("Failed to crop and save image")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}