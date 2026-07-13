package com.codingskillshub.bitpigeon.ui.composables

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.codingskillshub.bitpigeon.ui.theme.AppTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

@Composable
fun QRCodePopUp(
    payloadText: String,
    isQrValid: Boolean = true,
    title: String = "Show QR",
    subtitle: String = "Share this QR code to connect with a remote device.",
    onDismiss: () -> Unit
) {
    val imageBitmap = remember(payloadText, isQrValid) {
        if (isQrValid) generateQRCodeBitmap(payloadText, 360) else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isQrValid) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(280.dp)
                        )
                    } else {
                        Text(
                            text = "Unable to render QR code.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else {
                    Text(
                        text = payloadText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Close")
                }
            }
        }
    }
}

private fun generateQRCodeBitmap(text: String, size: Int): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (error: Exception) {
        null
    }
}

@Preview(showBackground = true, name = "Valid QR Light")
@Composable
fun QRCodePopUpPreview() {
    AppTheme(selectedTheme = "LIGHT") {
        QRCodePopUp(
            payloadText = "Valid Payload Text",
            isQrValid = true,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Invalid QR Light")
@Composable
fun QRCodePopUpInvalidPreview() {
    AppTheme(selectedTheme = "LIGHT") {
        QRCodePopUp(
            payloadText = "Unknown Device Address. Please ensure discovery is active.",
            isQrValid = false,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Valid QR Dark")
@Composable
fun QRCodePopUpDarkPreview() {
    AppTheme(selectedTheme = "DARK") {
        QRCodePopUp(
            payloadText = "Valid Payload Text",
            isQrValid = true,
            onDismiss = {}
        )
    }
}
