package com.codingskillshub.bitpigeon.infrastructure

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
// ...existing imports...
import android.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
// ...existing imports...
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing crop bounds in image coordinates
 */
data class CropBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * Service for handling image cropping operations
 */
@Singleton
class ImageCroppingService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Load a bitmap from URI, handling rotation from EXIF data
     */
    fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Handle EXIF rotation
            val rotatedBitmap = try {
                val exifStream = context.contentResolver.openInputStream(uri)
                if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    val rotation = getRotationDegrees(exif)
                    exifStream.close()
                    if (rotation != 0) {
                        rotateBitmap(bitmap, rotation)
                    } else {
                        bitmap
                    }
                } else {
                    bitmap
                }
            } catch (e: Exception) {
                bitmap
            }

            rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crop a bitmap to a square based on crop bounds and save to output stream
     * @param bitmap The source bitmap
     * @param cropBounds The crop area in bitmap coordinates
     * @param outputStream The stream to write the cropped image to
     * @param quality JPEG quality (0-100)
     * @return true if successful, false otherwise
     */
    fun cropAndSaveSquareImage(
        bitmap: Bitmap,
        cropBounds: CropBounds,
        outputStream: OutputStream,
        quality: Int = 85
    ): Boolean {
        return try {
            // Ensure crop bounds are within bitmap dimensions
            val left = cropBounds.left.toInt().coerceIn(0, bitmap.width)
            val top = cropBounds.top.toInt().coerceIn(0, bitmap.height)
            val width = cropBounds.width.toInt()
                .coerceIn(1, bitmap.width - left)
            val height = cropBounds.height.toInt()
                .coerceIn(1, bitmap.height - top)

            // Calculate square size (smaller of width/height)
            val squareSize = minOf(width, height)

            // Create cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, squareSize, squareSize)

            // Save to JPEG format
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()

            // Recycle if it's a different object
            if (croppedBitmap != bitmap) {
                croppedBitmap.recycle()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get rotation degrees from EXIF data
     */
    private fun getRotationDegrees(exif: ExifInterface): Int {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    /**
     * Rotate bitmap by given degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(degrees.toFloat())
        }

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        if (rotated != bitmap) {
            bitmap.recycle()
        }

        return rotated
    }

    /**
     * Scale bitmap to fit within max dimensions while maintaining aspect ratio
     */
    fun scaleBitmapToFit(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }

        val widthRatio = maxWidth.toFloat() / bitmap.width
        val heightRatio = maxHeight.toFloat() / bitmap.height
        val scale = minOf(widthRatio, heightRatio)

        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        if (scaled != bitmap) {
            bitmap.recycle()
        }

        return scaled
    }
}

