package com.codingskillshub.bitpigeon.domain.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class TransferStatus : Serializable {
    PENDING,
    TRANSFERRING,
    COMPLETED,
    FAILED
}

enum class StoreIn : Serializable {
    PRIVATE_STORAGE,
    PUBLIC_STORAGE
}

@Entity(tableName = "attachment")
data class Attachment (
    @PrimaryKey val id: String,
    val messageId: String,
    val chatGroupId: String,
    val senderId: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val timeStamp: String,
    val filePath: String = "",
    val transferStatus: TransferStatus = TransferStatus.PENDING,
    val storeIn: StoreIn = StoreIn.PUBLIC_STORAGE
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class AttachmentPreviewData(
    val id: String,
    val fileName: String,
    val fileType: String,
    val fileUri: Uri,
    var isTransferring: Boolean = false,
    var progress: Int = 0
)

enum class AppFileType: Serializable {
    PROFILE_PICTURE,
    UNKNOWN
}

data class AppFile (
    val id: String,
    val senderId: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val filePath: String = "",
    val appFileType: AppFileType = AppFileType.UNKNOWN,
    val transferStatus: TransferStatus = TransferStatus.PENDING,
    val storeIn: StoreIn = StoreIn.PRIVATE_STORAGE
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class AppFileRequest (
    val id: String,
    val senderId: String,
    val requestToUserId: String, //peer clientId
    val fileName: String,
    val appFileType: AppFileType = AppFileType.UNKNOWN,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}