package com.codingskillshub.bitpigeon.infrastructure

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
    fun getOutputStream(fileName: String, isPrivateStorage: Boolean): Pair<OutputStream, Uri> {
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

    fun checkFileExist(fileName: String, uri: Uri): Boolean {
        // Prefer direct filesystem check for file:// URIs (works on all API levels)
        uri.scheme?.let { scheme ->
            if (scheme.equals("file", ignoreCase = true)) {
                val path = uri.path ?: return false
                val file = File(path)
                return file.exists() && file.name == fileName
            }
        }

        // For content:// URIs on Android Q+ use MediaStore query to validate display name
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Instead of querying the cursor (which requires persistent permissions),
                // we try to open the metadata descriptor or a stream.
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    // If we can open it, the file exists and we have access.
                    // Note: We can't easily verify the 'fileName' matches the display name
                    // without a Cursor, but if the URI is valid, the file is available.
                    true
                } ?: false
            } catch (e: Exception) {
                // If an exception is thrown (SecurityException or FileNotFound), the file doesn't exist or access is lost
                Log.e("FileStorageService", "File check failed for URI: $uri", e)
                false
            }
        } else {
            // Legacy: try filesystem path if available
            val file = File(uri.path ?: return false)
            file.exists() && file.name == fileName
        }
    }
    
    fun getFileUri(fileName: String, isPrivateStorage: Boolean = true): Uri? {
        return if (isPrivateStorage) {
            val directory = File(context.getExternalFilesDir(null), "BitPigeon")
            val file = File(directory, fileName)
            if (file.exists()) Uri.fromFile(file) else null
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

                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "$relativePath/BitPigeon%")

                context.contentResolver.query(
                    contentUri,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        Uri.withAppendedPath(contentUri, id.toString())
                    } else null
                }
            } else {
                val relativePath = when (extension) {
                    "jpg", "jpeg", "png", "gif", "webp" -> Environment.DIRECTORY_PICTURES
                    "mp4", "mkv", "mov" -> Environment.DIRECTORY_MOVIES
                    else -> Environment.DIRECTORY_DOWNLOADS
                }
                @Suppress("DEPRECATION")
                val baseDir = Environment.getExternalStoragePublicDirectory(relativePath)
                val directory = File(baseDir, "BitPigeon")
                val file = File(directory, fileName)
                if (file.exists()) Uri.fromFile(file) else null
            }
        }
    }
    
    fun getPrivateFileByName(fileName: String): Pair<String, Uri>? {
        val files = getAllPrivateFiles()
        Log.d("FileStorageService", "getPrivateFileByName: $files")
        return files.find { filePair -> filePair.first.startsWith(fileName) }
    }
    
    fun deletePrivateFileByUri(uri: Uri): Boolean {
        return try {
            val file = File(uri.path ?: return false)
            file.exists() && file.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAllPrivateFiles(): List<Pair<String, Uri>> {
        val directory = File(context.getExternalFilesDir(null), "BitPigeon")
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        
        return directory.listFiles()
            ?.filter { it.isFile }
            ?.map { file -> Pair(file.name, Uri.fromFile(file)) } 
            ?: emptyList()
    }

    fun getPrivateFileUri(fileName: String): String {
        val directory = File(context.getExternalFilesDir(null), "BitPigeon")
        if (!directory.exists()) if (!directory.exists()) directory.mkdirs()
        val file = File(directory, fileName)
        return Uri.fromFile(file).toString()
    }
}
