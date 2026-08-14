package com.gps.warehouse.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.gps.warehouse.utils.CameraBarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun CameraScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Полноэкранный
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        CameraScannerContent(
            onBarcodeDetected = { code ->
                onBarcodeDetected(code)
                onDismiss() // Закрываем сразу после скана
            },
            onClose = onDismiss
        )
    }
}

@Composable
private fun CameraScannerContent(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            executor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            hasCameraPermission -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { it.setAnalyzer(executor, CameraBarcodeAnalyzer(onBarcodeDetected)) }

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            }
            else -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Для сканирования необходим доступ к камере",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Разрешить доступ")
                    }
                }
            }
        }

        // Кнопка закрытия
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Закрыть",
                tint = Color.White
            )
        }

        Text(
            "Наведите камеру на QR-код или штрихкод",
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}


// ====================== UI КОМПОНЕНТЫ ДЛЯ ПРЕВЬЮ ======================
@Composable
private fun CameraScannerPreviewContent(
    hasPermission: Boolean,
    permissionDeniedPermanently: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            hasPermission -> {
                // Имитация превью камеры
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                ) {
                    // Симуляция области сканирования
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(200.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }
            else -> {
                // UI отсутствия разрешения
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Для сканирования необходим доступ к камере",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = onRetry) {
                        Text("Разрешить доступ")
                    }

                    if (permissionDeniedPermanently) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Если разрешение отклонено навсегда, включите его в настройках приложения.",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Кнопка закрытия
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Закрыть",
                tint = Color.White
            )
        }

        // Подсказка (только если есть разрешение)
        if (hasPermission) {
            Text(
                "Наведите камеру на QR-код или штрихкод",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// ====================== ПРЕВЬЮ ======================
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Camera Scanner Dialog - Active", showSystemUi = true)
@Composable
private fun CameraScannerDialog_Active_Preview() {
    MaterialTheme {
        CameraScannerPreviewContent(
            hasPermission = true,
            permissionDeniedPermanently = false,
            onRetry = {},
            onClose = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Camera Scanner Dialog - Permission Denied", showSystemUi = true)
@Composable
private fun CameraScannerDialog_PermissionDenied_Preview() {
    MaterialTheme {
        CameraScannerPreviewContent(
            hasPermission = false,
            permissionDeniedPermanently = false,
            onRetry = {},
            onClose = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Camera Scanner Dialog - Permission Denied Permanently", showSystemUi = true)
@Composable
private fun CameraScannerDialog_PermissionDeniedPermanently_Preview() {
    MaterialTheme {
        CameraScannerPreviewContent(
            hasPermission = false,
            permissionDeniedPermanently = true,
            onRetry = {},
            onClose = {}
        )
    }
}