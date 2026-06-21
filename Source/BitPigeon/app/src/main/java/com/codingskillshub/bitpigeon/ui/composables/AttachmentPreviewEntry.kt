package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage

enum class ViewType {
    LIST, GRID
}

enum class PreviewSize {
    STANDARD, MAX
}

@Composable
fun AttachmentPreviewEntry(
    fileName: String,
    fileType: String,
    fileUri: String,
    previewSize: PreviewSize = PreviewSize.STANDARD,
    viewType: ViewType = ViewType.LIST,
    showFileName: Boolean = true,
    isTransferring: Boolean = false,
    progress: Int = 0,
    onClick: () -> Unit = {}
) {
    val boxModifier = when {
        previewSize == PreviewSize.MAX -> Modifier
        viewType == ViewType.LIST -> Modifier.size(48.dp)
        else -> Modifier.size(64.dp)
    }

    if (viewType == ViewType.LIST) {
        if (!showFileName) {
            AttachmentPreviewBox(
                fileName = fileName,
                fileType = fileType,
                fileUri = fileUri,
                modifier = boxModifier,
                isTransferring = isTransferring,
                progress = progress,
                onClick = onClick
            )
        } else {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentPreviewBox(
                    fileName = fileName,
                    fileType = fileType,
                    fileUri = fileUri,
                    modifier = boxModifier,
                    isTransferring = isTransferring,
                    progress = progress,
                    onClick = onClick
                )

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
                modifier = boxModifier,
                isTransferring = isTransferring,
                progress = progress,
                onClick = onClick
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
    modifier: Modifier = Modifier,
    isTransferring: Boolean = false,
    progress: Int = 0,
    onClick: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // maxWidth and maxHeight are now equal due to the layout modifier above.
        val actualSize = maxWidth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            val isImage = fileType.startsWith("image/", ignoreCase = true)
            val isVideo = fileType.startsWith("video/", ignoreCase = true)

            if (isImage || isVideo) {
                Box(contentAlignment = Alignment.Center) {
                    SubcomposeAsyncImage(
                        model = fileUri,
                        contentDescription = if (isImage) "Image Preview" else "Video Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Icon(
                                imageVector = if (isImage) Icons.Default.Image else Icons.Default.PlayCircle,
                                contentDescription = "Preview Load Failed",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )
                    if (isVideo) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(actualSize / 2)
                        )
                    }
                }
            } else {
                // Show file extension text as an "image"
                val extension = fileName.substringAfterLast('.', "").uppercase()
                Text(
                    text = extension.ifEmpty { "FILE" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (actualSize < 60.dp) 10.sp else 12.sp,
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
                        modifier = Modifier.size(actualSize * 0.7f),
                        color = Color.White,
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
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
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AttachmentPreviewEntry(
                    fileName = "vacation.png",
                    fileType = "image/png",
                    fileUri = "https://example.com/image.png",
                    showFileName = false,
                    previewSize = PreviewSize.MAX
                )
                AttachmentPreviewEntry(
                    fileName = "vacation.png",
                    fileType = "image/png",
                    fileUri = "https://example.com/image.png",
                    showFileName = false,
                    previewSize = PreviewSize.MAX
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("No Filename (List Transferring):")
            Box(modifier = Modifier.width(120.dp).height(180.dp)) {
                AttachmentPreviewEntry(
                    fileName = "vacation.png",
                    fileType = "image/png",
                    fileUri = "https://example.com/image.png",
                    showFileName = false,
                    isTransferring = true,
                    progress = 40,
                    previewSize = PreviewSize.MAX
                )
            }
        }
    }
}
