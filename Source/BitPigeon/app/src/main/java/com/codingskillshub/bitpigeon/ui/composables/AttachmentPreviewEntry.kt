package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage

enum class ViewType {
    LIST, GRID
}

@Composable
fun AttachmentPreviewEntry(
    fileName: String,
    fileType: String,
    fileUri: String,
    viewType: ViewType = ViewType.LIST,
    showFileName: Boolean = true,
    isTransferring: Boolean = false,
    progress: Int = 0
) {
    if (viewType == ViewType.LIST) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttachmentPreviewBox(
                fileName = fileName,
                fileType = fileType,
                fileUri = fileUri,
                size = 48.dp,
                isTransferring = isTransferring,
                progress = progress
            )
            
            if (showFileName) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(if (showFileName) 8.dp else 0.dp)
                .width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AttachmentPreviewBox(
                fileName = fileName,
                fileType = fileType,
                fileUri = fileUri,
                size = 64.dp,
                isTransferring = isTransferring,
                progress = progress
            )
            
            if (showFileName) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AttachmentPreviewBox(
    fileName: String,
    fileType: String,
    fileUri: String,
    size: Dp,
    isTransferring: Boolean = false,
    progress: Int = 0
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val isImage = fileType.startsWith("image/", ignoreCase = true)

        if (isImage) {
            SubcomposeAsyncImage(
                model = fileUri,
                contentDescription = "Image Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Image Load Failed",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        } else {
            // Show file extension text as an "image"
            val extension = fileName.substringAfterLast('.', "").uppercase()
            Text(
                text = extension.ifEmpty { "FILE" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (size < 60.dp) 10.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                textAlign = TextAlign.Center
            )
        }

        if (isTransferring) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.size(size * 0.7f),
                    color = Color.White,
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttachmentPreviewEntryPreview() {
    MaterialTheme {
        Column {
            Text("List View:")
            AttachmentPreviewEntry(
                fileName = "vacation.png",
                fileType = "image/png",
                fileUri = "https://example.com/image.png",
                viewType = ViewType.LIST
            )
            Text("List View: (Transferring)")
            AttachmentPreviewEntry(
                fileName = "vacation.png",
                fileType = "image/png",
                fileUri = "https://example.com/image.png",
                viewType = ViewType.LIST,
                isTransferring = true,
                progress = 20
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Grid View:")
            Row {
                AttachmentPreviewEntry(
                    fileName = "report.pdf",
                    fileType = "application/pdf",
                    fileUri = "",
                    viewType = ViewType.GRID,
                    isTransferring = false,
                    progress = 45
                )
                AttachmentPreviewEntry(
                    fileName = "image.jpg",
                    fileType = "image/jpeg",
                    fileUri = "https://example.com/image.jpg",
                    viewType = ViewType.GRID,
                    isTransferring = false,
                    progress = 80
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Grid View (Transferring):")
            Row {
                AttachmentPreviewEntry(
                    fileName = "report.pdf",
                    fileType = "application/pdf",
                    fileUri = "",
                    viewType = ViewType.GRID,
                    isTransferring = true,
                    progress = 45
                )
                AttachmentPreviewEntry(
                    fileName = "image.jpg",
                    fileType = "image/jpeg",
                    fileUri = "https://example.com/image.jpg",
                    viewType = ViewType.GRID,
                    isTransferring = true,
                    progress = 80
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("No Filename (List):")
            Row {
                AttachmentPreviewEntry(
                    fileName = "vacation.png",
                    fileType = "image/png",
                    fileUri = "https://example.com/image.png",
                    showFileName = false
                )
                AttachmentPreviewEntry(
                    fileName = "vacation.png",
                    fileType = "image/png",
                    fileUri = "https://example.com/image.png",
                    showFileName = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("No Filename (List Transferring):")
            AttachmentPreviewEntry(
                fileName = "vacation.png",
                fileType = "image/png",
                fileUri = "https://example.com/image.png",
                showFileName = false,
                isTransferring = true,
                progress = 40
            )
        }
    }
}
