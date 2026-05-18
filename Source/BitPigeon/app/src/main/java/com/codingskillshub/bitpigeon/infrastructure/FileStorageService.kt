package com.codingskillshub.bitpigeon.infrastructure

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getOutputStream(fileName: String, isPrivateStorage: Boolean): Pair<OutputStream, Uri?> {
        return if (isPrivateStorage) {
            val directory = File(context.getExternalFilesDir(null), "BitPigeon")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            Pair(FileOutputStream(file), Uri.fromFile(file))
        } else {
            val extension = fileName.substringAfterLast(".").lowercase()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val (contentUri, relativePath) = when (extension) {
                    "jpg", "jpeg", "png", "gif", "webp" ->
                        Pair(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES)
                    "mp4", "mkv", "mov" ->
                        Pair(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES)
                    else ->
                        Pair(MediaStore.Downloads.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_DOWNLOADS)
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(extension))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$relativePath/BitPigeon")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(contentUri, contentValues)
                    ?: throw Exception("Failed to create MediaStore entry")

                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw Exception("Failed to open output stream")

                Pair(outputStream, uri)
            } else {
                // Legacy implementation for Android 9 and below
                val relativePath = when (extension) {
                    "jpg", "jpeg", "png", "gif", "webp" -> Environment.DIRECTORY_PICTURES
                    "mp4", "mkv", "mov" -> Environment.DIRECTORY_MOVIES
                    else -> Environment.DIRECTORY_DOWNLOADS
                }
                
                @Suppress("DEPRECATION")
                val baseDir = Environment.getExternalStoragePublicDirectory(relativePath)
                val directory = File(baseDir, "BitPigeon")
                if (!directory.exists()) {
                    if (!directory.mkdirs() && !directory.exists()) {
                         throw Exception("Failed to create directory: ${directory.absolutePath}. Ensure WRITE_EXTERNAL_STORAGE permission is granted.")
                    }
                }
                
                val file = File(directory, fileName)
                Pair(FileOutputStream(file), Uri.fromFile(file))
            }
        }
    }

    fun finalizeFile(uri: Uri?) {
        if (uri == null) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, contentValues, null, null)
        } else {
            // Scan the file so it appears in MediaStore (Gallery/Downloads)
            val path = uri.path ?: return
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                null,
                null
            )
        }
    }

    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "mp4" -> "video/mp4"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
