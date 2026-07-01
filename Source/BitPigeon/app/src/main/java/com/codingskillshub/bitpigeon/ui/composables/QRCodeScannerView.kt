package com.codingskillshub.bitpigeon.ui.composables

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

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

    val onScanResultState = rememberUpdatedState(onScanResult)
    var hasScanned = rememberSaveable { mutableStateOf(false) }

    DisposableEffect(key1 = Unit) {
        if (!cameraPermissionState.value) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose {}
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!cameraPermissionState.value) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera permission is required to scan QR codes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(text = "Grant camera permission")
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(text = "Cancel")
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        DecoratedBarcodeView(ctx).apply {
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult?) {
                                    if (result == null || result.text.isNullOrBlank()) {
                                        return
                                    }
                                    if (!hasScanned.value) {
                                        hasScanned.value = true
                                        onScanResultState.value(result.text)
                                    }
                                }

                                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                                    // no-op
                                }
                            })
                        }
                    },
                    update = { view ->
                        if (!view.isActivated) {
                            view.resume()
                        }
                    }
                )
            }
        }
    }
}
