package com.codingskillshub.bitpigeon.domain.services

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun openFileWithExternalApp(fileName: String, fileType: String, fileUri: String) {
        try {
            val uri = fileUri.toUri()
            val shareUri = if (uri.scheme == "file") {
                val filePath = uri.path
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else {
                        uri
                    }
                } else {
                    uri
                }
            } else {
                uri
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareUri, fileType.ifEmpty { "application/octet-stream" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open $fileName with")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
