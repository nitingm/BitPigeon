package com.codingskillshub.bitpigeon.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.codingskillshub.bitpigeon.infrastructure.CropBounds

@Composable
fun ProfilePictureEditOverlay(
    fileName: String,
    fileType: String,
    sourceFileUri: String,
    onDismiss: () -> Unit,
    // cropBounds are relative to the image size provided in displayWidth/Height
    onSaveCrop: (sourceUri: String, fileName: String, cropBounds: CropBounds, displayWidth: Int, displayHeight: Int) -> Unit = { _, _, _, _, _ -> }
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        ProfilePictureEditOverlayContent(
            fileName = fileName,
            fileType = fileType,
            fileUri = sourceFileUri,
            onDismiss = onDismiss,
            onSaveCrop = onSaveCrop
        )
    }
}

@Composable
fun ProfilePictureEditOverlayContent(
    fileName: String,
    fileType: String,
    fileUri: String,
    onDismiss: () -> Unit,
    onSaveCrop: (sourceUri: String, fileName: String, cropBounds: CropBounds, displayWidth: Int, displayHeight: Int) -> Unit = { _, _, _, _, _ -> }
) {
    // State for transformations
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageScale by remember { mutableFloatStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Image size tracking
    var intrinsicSize by remember { mutableStateOf(Size.Zero) }
    var painterState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    // Robust image check
    val isImage = remember(fileType, fileName) {
        fileType.startsWith("image/", ignoreCase = true) ||
                listOf("jpg", "jpeg", "png", "webp", "gif").any { fileName.lowercase().endsWith(it) } ||
                fileType.contains("octet-stream", ignoreCase = true) ||
                fileType.isBlank()
    }

    // Reactively calculate fit size and crop size
    val fitImageSize by remember {
        derivedStateOf {
            if (containerSize.width > 0 && intrinsicSize.width > 0) {
                val containerRatio = containerSize.width.toFloat() / containerSize.height
                val imageRatio = intrinsicSize.width / intrinsicSize.height
                if (imageRatio > containerRatio) {
                    Size(containerSize.width.toFloat(), containerSize.width.toFloat() / imageRatio)
                } else {
                    Size(containerSize.height.toFloat() * imageRatio, containerSize.height.toFloat())
                }
            } else Size.Zero
        }
    }

    val cropSize by remember {
        derivedStateOf {
            if (containerSize.width > 0) minOf(containerSize.width.toFloat(), containerSize.height.toFloat()) * 0.8f else 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
    ) {
        if (isImage) {
            // 1. Image Layer (Pannable and Zoomable)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            imageScale = (imageScale * zoom).coerceIn(0.5f, 10f)
                            imageOffset += pan
                        }
                    }
            ) {
                AsyncImage(
                    model = fileUri,
                    contentDescription = "Edit Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                            translationX = imageOffset.x
                            translationY = imageOffset.y
                        },
                    contentScale = ContentScale.Fit,
                    onState = { state ->
                        painterState = state
                        if (state is AsyncImagePainter.State.Success) {
                            intrinsicSize = state.painter.intrinsicSize
                        }
                    }
                )

                if (painterState is AsyncImagePainter.State.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                if (painterState is AsyncImagePainter.State.Error) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Could not load image", color = Color.White)
                        }
                    }
                }
            }

            // 2. Fixed Overlay Layer (Fixed Crop Area)
            if (cropSize > 0) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height
                    val rect = Rect(
                        left = (cw - cropSize) / 2,
                        top = (ch - cropSize) / 2,
                        right = (cw + cropSize) / 2,
                        bottom = (ch + cropSize) / 2
                    )

                    // Draw semi-transparent darkened background with a circular hole (WhatsApp style)
                    val path = Path().apply {
                        addRect(Rect(0f, 0f, cw, ch))
                        addOval(rect)
                        fillType = PathFillType.EvenOdd
                    }
                    drawPath(path, Color.Black.copy(alpha = 0.7f))

                    // Square Border (Light)
                    drawRect(
                        color = Color.White.copy(alpha = 0.3f),
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Circular Guide
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = cropSize / 2,
                        center = rect.center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        } else {
            // Fallback for non-image types
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "File Icon",
                        tint = Color.White,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Edit Profile Picture", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(fileType, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 3. UI Controls
        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(8.dp)
                .statusBarsPadding()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, enabled = !isSaving) {
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

        // Bottom Control Bar with Save Button
        if (isImage) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pan and zoom to adjust",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (fitImageSize.width > 0) {
                                isSaving = true
                                
                                // Image dimensions on screen after scale
                                val scaledW = fitImageSize.width * imageScale
                                val scaledH = fitImageSize.height * imageScale
                                
                                // Coordinates of the image top-left on screen
                                val imageLeftOnScreen = (containerSize.width - scaledW) / 2 + imageOffset.x
                                val imageTopOnScreen = (containerSize.height - scaledH) / 2 + imageOffset.y
                                
                                // Coordinates of the fixed crop box on screen
                                val cropLeftOnScreen = (containerSize.width - cropSize) / 2
                                val cropTopOnScreen = (containerSize.height - cropSize) / 2

                                // Map the crop box position relative to the image
                                val cropBounds = CropBounds(
                                    left = cropLeftOnScreen - imageLeftOnScreen,
                                    top = cropTopOnScreen - imageTopOnScreen,
                                    width = cropSize,
                                    height = cropSize
                                )
                                
                                onSaveCrop(fileUri, fileName, cropBounds, scaledW.toInt(), scaledH.toInt())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        contentPadding = ButtonDefaults.ContentPadding,
//                        enabled = !isSaving && fitImageSize.width > 0 && cropSize > 0,
                        modifier = Modifier.height(50.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePictureEditOverlayContentPreview() {
    MaterialTheme {
        ProfilePictureEditOverlayContent(
            fileName = "profile.jpg",
            fileType = "image/jpeg",
            fileUri = "",
            onDismiss = {}
        )
    }
}
