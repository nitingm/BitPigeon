package com.codingskillshub.bitpigeon.ui.composables

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codingskillshub.bitpigeon.ui.theme.AppTheme
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings

@Composable
fun QRCodeScannerView(
    onScanResult: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    
    val cameraPermissionState = rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionState.value = granted
    }

    DisposableEffect(key1 = Unit) {
        if (!cameraPermissionState.value) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose {}
    }

    Dialog(onDismissRequest = onCancel) {
        QRCodeScannerContent(
            hasPermission = cameraPermissionState.value,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onCancel = onCancel,
            onScanResult = onScanResult
        )
    }
}

@Composable
fun QRCodeScannerContent(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
    onScanResult: (String) -> Unit
) {
    val onScanResultState = rememberUpdatedState(onScanResult)
    val hasScanned = rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Surface(
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasPermission) {
                Text(
                    text = "Camera permission is required to scan QR codes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Grant camera permission")
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Cancel")
                }
            } else {
                Text(
                    text = "Scan QR",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.Black, MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (LocalInspectionMode.current) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        )
                        Text(
                            text = "Camera Scanner Placeholder",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 220.dp)
                        )
                    } else {
                        var barcodeView by remember { mutableStateOf<DecoratedBarcodeView?>(null) }

                        // Synchronize scanner lifecycle with the UI lifecycle
                        DisposableEffect(lifecycleOwner, barcodeView) {
                            if (barcodeView == null) return@DisposableEffect onDispose {}

                            val observer = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_RESUME -> barcodeView?.resume()
                                    Lifecycle.Event.ON_PAUSE -> barcodeView?.pause()
                                    else -> {}
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                                barcodeView?.pause()
                            }
                        }

                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                DecoratedBarcodeView(ctx).apply {
                                    val settings = CameraSettings().apply {
                                        isContinuousFocusEnabled = true
                                        isAutoFocusEnabled = true
                                    }
                                    cameraSettings = settings
                                    decodeContinuous(object : BarcodeCallback {
                                        override fun barcodeResult(result: BarcodeResult?) {
                                            if (result == null || result.text.isNullOrBlank()) return
                                            if (!hasScanned.value) {
                                                hasScanned.value = true
                                                onScanResultState.value(result.text)
                                            }
                                        }
                                        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
                                    })
                                    resume()
                                    barcodeView = this
                                }
                            },
                            update = { view ->
                                barcodeView = view
                            }
                        )
                    }
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Close")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Content - Active")
@Composable
fun QRCodeScannerContentPreview() {
    AppTheme(selectedTheme = "SYSTEM") {
        QRCodeScannerContent(
            hasPermission = true,
            onRequestPermission = {},
            onCancel = {},
            onScanResult = {}
        )
    }
}

@Preview(showBackground = true, name = "Content - No Permission")
@Composable
fun QRCodeScannerPermissionPreview() {
    AppTheme(selectedTheme = "SYSTEM") {
        QRCodeScannerContent(
            hasPermission = false,
            onRequestPermission = {},
            onCancel = {},
            onScanResult = {}
        )
    }
}
