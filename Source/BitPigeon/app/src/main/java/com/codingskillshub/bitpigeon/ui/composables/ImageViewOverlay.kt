package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage

@Composable
fun ImageViewOverlay(
    fileName: String,
    fileType: String,
    fileUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        ImageViewOverlayContent(
            fileName = fileName,
            fileType = fileType,
            fileUri = fileUri,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ImageViewOverlayContent(
    fileName: String,
    fileType: String,
    fileUri: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Image Content
        val isImage = fileType.startsWith("image/", ignoreCase = true)

        if (isImage) {
            SubcomposeAsyncImage(
                model = fileUri,
                contentDescription = "Fullscreen Image: $fileName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Error loading image",
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Could not load image", color = Color.White)
                        }
                    }
                }
            )
        } else {
            // Fallback for non-image types if accidentally opened
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "File Icon",
                        tint = Color.White,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(fileName, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(fileType, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .statusBarsPadding()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close overlay",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = fileName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImageViewOverlayPreview() {
    MaterialTheme {
        ImageViewOverlayContent(
            fileName = "sample_image.jpg",
            fileType = "image/jpeg",
            fileUri = "https://example.com/sample.jpg",
            onDismiss = {}
        )
    }
}
