package com.gps.warehouse.ui.scanner

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Состояния
    var hasCameraPermission by remember {
        mutableStateOf(hasCameraPermission(context))
    }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    // Лаунчер запроса разрешения
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        // Если пользователь отклонил и "больше не спрашивать",
        // granted=false, и мы покажем соответствующий UI
        if (!granted) {
            permissionDeniedPermanently = true
        }
    }

    // При первом запуске запрашиваем разрешение, если его нет
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Освобождение ресурсов камеры при выходе с экрана
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            }
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
                CameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    executor = executor,
                    onBarcodeDetected = { code ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("SCAN_RESULT_KEY", code)
                        navController.popBackStack()
                    }
                )
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
                        text = "Для сканирования необходим доступ к камере",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    ) {
                        Text("Разрешить доступ")
                    }

                    if (permissionDeniedPermanently) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Если разрешение отклонено навсегда, включите его в настройках приложения.",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Кнопка "Назад"
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White
            )
        }

        // Подсказка
        if (hasCameraPermission) {
            Text(
                text = "Наведите камеру на QR-код или штрихкод",
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

// ====================== Вспомогательные функции ======================

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

// ====================== Camera Preview ======================

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: java.util.concurrent.Executor,
    onBarcodeDetected: (String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
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
                        .also {
                            it.setAnalyzer(
                                executor,
                                BarcodeAnalyzer(onBarcodeDetected)
                            )
                        }

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

// ====================== Превью ======================

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Scanner - Permission Denied", showSystemUi = true)
@Composable
private fun CameraScannerScreen_PermissionDenied_Preview() {
    MaterialTheme {
        Surface(color = Color.Black) {
            PermissionDeniedContent(
                onRetry = {},
                permissionDeniedPermanently = false
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Scanner - Permission Denied Permanently", showSystemUi = true)
@Composable
private fun CameraScannerScreen_PermissionDeniedPermanently_Preview() {
    MaterialTheme {
        Surface(color = Color.Black) {
            PermissionDeniedContent(
                onRetry = {},
                permissionDeniedPermanently = true
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Scanner - Active Scanning", showSystemUi = true)
@Composable
private fun CameraScannerScreen_ActiveScanning_Preview() {
    MaterialTheme {
        Surface(color = Color.Black) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
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

                // Кнопка "Назад"
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }

                // Подсказка
                Text(
                    text = "Наведите камеру на QR-код или штрихкод",
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
}

// ====================== Вынесенные UI компоненты для Preview ======================

@Composable
private fun PermissionDeniedContent(
    onRetry: () -> Unit,
    permissionDeniedPermanently: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Для сканирования необходим доступ к камере",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onRetry
        ) {
            Text("Разрешить доступ")
        }

        if (permissionDeniedPermanently) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Если разрешение отклонено навсегда, включите его в настройках приложения.",
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}