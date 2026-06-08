package com.codingskillshub.bitpigeon.domain.models

import android.content.Context
import androidx.core.net.toUri
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.codingskillshub.bitpigeon.common.ConfigurationService
import com.codingskillshub.bitpigeon.domain.entities.AppFileRequest
import com.codingskillshub.bitpigeon.domain.entities.AppFileType
import com.codingskillshub.bitpigeon.domain.entities.AppFile
import com.codingskillshub.bitpigeon.domain.entities.Attachment
import com.codingskillshub.bitpigeon.domain.entities.StoreIn
import com.codingskillshub.bitpigeon.domain.entities.TransferStatus
import com.codingskillshub.bitpigeon.domain.interfaces.dao.UserDao
import com.codingskillshub.bitpigeon.domain.services.FileTransferService
import com.codingskillshub.bitpigeon.domain.services.OnlineChatService
import com.codingskillshub.bitpigeon.domain.services.WifiCommunicationService
import com.codingskillshub.bitpigeon.infrastructure.CropBounds
import com.codingskillshub.bitpigeon.infrastructure.FileStorageService
import com.codingskillshub.bitpigeon.infrastructure.ImageCroppingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.codingskillshub.bitpigeon.common.HashService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSystemModel @Inject constructor(
    private val userDao: UserDao,
    private val imageCroppingService: ImageCroppingService,
    private val fileStorageService: FileStorageService,
    private val fileTransferService: FileTransferService,
    private val onlineChatService: OnlineChatService,
    private val wifiService: WifiCommunicationService,
    private val hashService: HashService,
    private val configurationService: ConfigurationService,
    @ApplicationContext private val context: Context
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
        modelScope.launch {
            onlineChatService.incomingGetProfilePictureRequest.collect { appFileRequest ->
                if (appFileRequest.appFileType == AppFileType.PROFILE_PICTURE) {
                    sendProfilePictureToClient(appFileRequest)
                }
            }
        }
        modelScope.launch {
            fileTransferService.incomingAppFile.collect { appFile ->
                if (appFile.appFileType == AppFileType.PROFILE_PICTURE) {
                    handleReceivedProfilePicture(appFile)
                }
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
                    userDao.updateProfilePicture(userId, finalFileName)
                } else {
                    throw Exception("Failed to crop and save image")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun sendProfilePictureToClient(appFileRequest: AppFileRequest) {
        val user = userDao.getUserById(getMyUserId()).firstOrNull()
        if (user != null) {
            val myProfilePicture = user.profilePicture
            val requestedProfilePicture = appFileRequest.fileName

            if (myProfilePicture != requestedProfilePicture) {
                Log.d("AppSystemModel", "User requested for non-existing profile picture")
            }
            val client = onlineChatService.getPeerClientById(appFileRequest.senderId)
            if (client != null) {
                Log.d("AppSystemModel", "Sending profile picture to client: ${client.user.name}")
                val myPPFile = fileStorageService.getPrivateFileByName(myProfilePicture)

                if (myPPFile != null) {
                    val myPPUri: Uri = myPPFile.second
                    val appFile = createAppFileFromUri(myPPUri, appFileRequest.id, AppFileType.PROFILE_PICTURE)
                    appFile?.let {
                        fileTransferService.sendAppFileToClient(appFile, myPPUri.toString(), client)
                    }
                }
            }
        }
    }

    private fun createAppFileFromUri(uri: Uri, appFileId: String, appFileType: AppFileType): AppFile? {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                val name = if (nameIndex != -1) it.getString(nameIndex) else "file_${System.currentTimeMillis()}"
                val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
                val type = contentResolver.getType(uri) ?: "application/octet-stream"

                AppFile(
                    id = appFileId,
                    senderId = getMyUserId(),
                    fileName = name,
                    fileSize = size,
                    fileType = type,
                    filePath = uri.toString(),
                    appFileType = appFileType,
                    transferStatus = TransferStatus.PENDING,
                    storeIn = StoreIn.PRIVATE_STORAGE
                )
            } else null
        }
    }

    private suspend fun handleReceivedProfilePicture(appFile: AppFile) {
        val profilePicturePath = appFile.filePath
        val profilePicture = appFile.fileName
        val userId = appFile.senderId
        if (fileStorageService.checkFileExist(profilePicture, profilePicturePath.toUri())) {
            userDao.updateProfilePicture(userId, profilePicturePath)
        } else {
            Log.d("AppSystemModel", "${userId}'s profile picture does not exist at: $profilePicturePath")
        }
    }

    fun getProfilePicture(profilePicture: String, userId: String) {
        val appFileRequest = AppFileRequest(
            id = hashService.generateUniqueId(profilePicture),
            senderId = getMyUserId(),
            requestToUserId = userId,
            fileName = profilePicture,
            appFileType = AppFileType.PROFILE_PICTURE
        )

        Log.i("AppSystemModel", "Requested for Profile picture: $profilePicture from $userId")
        onlineChatService.sendGetProfilePictureRequest(appFileRequest)
    }
}