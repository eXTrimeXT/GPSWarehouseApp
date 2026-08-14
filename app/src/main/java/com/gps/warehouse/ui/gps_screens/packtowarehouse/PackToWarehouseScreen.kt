package com.gps.warehouse.ui.gps_screens.packtowarehouse

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager

@Composable
fun PackToWarehouseScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Подписываемся на настройку
    val cameraScanEnabled by viewModel.cameraScanEnabled.collectAsState()
    // Флаг показа диалога камеры
    var showCameraDialog by remember { mutableStateOf(false) }

    // Состояния полей
    var materialArticle by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var uniqueCode by remember { mutableStateOf("") }

    val honeywellHelper = remember { ScannerManager(context) }

    // При входе на экран сбрасываем состояние
    LaunchedEffect(Unit) {
        viewModel.enterPackToWarehouseScreen()
    }

    // Инициализация сканера
    DisposableEffect(Unit) {
        honeywellHelper.init()
        onDispose {
            honeywellHelper.release()
        }
    }

    fun processScannedData(scannedData: String){
        if (scannedData.isNotEmpty()) {
            handleScanForPacking(
                scannedData = scannedData,
                onParsed = { mat, code ->
                    // Принудительно не читаем штрихкод
                    if (code.isEmpty()){
                        Toast.makeText(context, "Необходимо сканировать QR-код!", Toast.LENGTH_SHORT).show()
                    }
                    // Нам необходим код материала, поэтому проверяем оба значения
                    if (mat.isNotEmpty() && code.isNotEmpty()) {
                        materialArticle = mat
                        uniqueCode = code
                        quantity = "1"
                    }
                },
                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    // Обработка сканирования
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            processScannedData(scannedData)
        }
    }

    // Диалог сканирования камерой
    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                // Диалог закроется автоматически через onDismiss
                showCameraDialog = false
            }
        )
    }

    PackToWarehouseContent(
        materialArticle = materialArticle,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        onMaterialChange = { materialArticle = it },
        quantity = quantity,
        onQuantityChange = { quantity = it },
        uniqueCode = uniqueCode,
        onCodeChange = { uniqueCode = it },
        uiState = uiState,
        onPackClick = {
            if (materialArticle.isBlank()) {
                Toast.makeText(context, "Введите артикул", Toast.LENGTH_SHORT).show()
                return@PackToWarehouseContent
            }
            val qtyInt = quantity.toIntOrNull() ?: 1
            // Примечание: убедитесь, что в ViewModel есть метод packMaterialToWarehouse
            // Если нет, используйте существующий packMaterial
            viewModel.packMaterial(materialArticle, qtyInt, uniqueCode.ifBlank { "MANUAL_${System.currentTimeMillis()}" })

            // Очистка после успешной отправки (опционально, можно делать в onSuccess в VM)
            // Но так как мы не знаем об успехе здесь, лучше очищать поля вручную или по событию из VM
            // Для примера оставим как есть, или добавим сброс при успехе в UIState
        },
        onBackClick = { navController.popBackStack() },
        onClearClick = {
            materialArticle = ""
            quantity = ""
            uniqueCode = ""
        },
        onRetryClick = {
            materialArticle = ""
            quantity = ""
            uniqueCode = ""
            viewModel.resetStateToIdle()
        }
    )
}

private fun handleScanForPacking(
    scannedData: String,
    onParsed: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    val parsedData = BarcodeParser.parse(scannedData)

    if (parsedData != null) {
        // Передаем материал и код в коллбэк
        onParsed(parsedData.material, parsedData.code)
    } else {
        onError("Неверный формат штрихкода")
    }
}

@Composable
fun PackToWarehouseContent(
    materialArticle: String,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit,
    onMaterialChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    uniqueCode: String,
    onCodeChange: (String) -> Unit,
    uiState: MainViewModel.UiState,
    onPackClick: () -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    // Создаем FocusRequester для поля количества
    val quantityFocusRequester = remember { FocusRequester() }

    var qtyFieldValue by remember {
        mutableStateOf(TextFieldValue(text = quantity, selection = TextRange(quantity.length)))
    }

    // Следим за изменением артикула. Если он появился (не пустой), переводим фокус на количество
    LaunchedEffect(materialArticle) {
        if (materialArticle.isNotEmpty()) {
            quantityFocusRequester.requestFocus()
        }
    }

    // Следим за изменением входящего параметра qty
    LaunchedEffect(quantity) {
        qtyFieldValue = TextFieldValue(
            text = quantity,
            selection = TextRange(quantity.length)
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MyCustomActionBar(onBackClick = onBackClick, text = "Упаковка на склад")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                when (uiState) {
                    is MainViewModel.UiState.Packed -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Успешно!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    uiState.message,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        // Автоматическая очистка после успеха (опционально)
                        LaunchedEffect(uiState) {
                            // Можно добавить задержку и очистку
                            onClearClick()
                        }
                    }

                    is MainViewModel.UiState.Error -> {
                        ErrorStateView(
                            message = uiState.message,
                            modifier = Modifier,
                            onRetry = { onRetryClick() }
                        )

                    }

                    is MainViewModel.UiState.PackToWarehouseIdle -> {
                        OutlinedTextField(
                            value = materialArticle,
                            onValueChange = onMaterialChange,
                            label = { Text("Артикул материала") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Сканировать"
                                )
                            },
                            enabled = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = qtyFieldValue,
                            onValueChange = { newValue ->
                                // Разрешаем ввод только цифр и ограничиваем длину до 3 знаков
                                if (newValue.text.all { it.isDigit() } && newValue.text.length <= 3) {
                                    qtyFieldValue = newValue.copy(
                                        selection = TextRange(newValue.text.length)
                                    )
                                }
                            },
                            label = { Text("Количество") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(quantityFocusRequester),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uniqueCode,
                            onValueChange = onCodeChange,
                            label = { Text("Уникальный код упаковки") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false // Код обычно приходит со сканера или генерируется
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onPackClick,
                            enabled = uiState !is MainViewModel.UiState.Loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState is MainViewModel.UiState.Loading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Упаковать")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(onClick = onClearClick, modifier = Modifier.fillMaxWidth()) {
                            Text("Очистить")
                        }
                    }

                    else -> {}
                }
            }
        }

        // Плавающая кнопка сканирования (только если включено в настройках)
        CameraScanButton(
            onClick = onCameraScanClick,
            cameraScanEnabled = cameraScanEnabled,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}


// ====================== ПРЕВЬЮ ======================
@Preview(showBackground = true, name = "Pack To Warehouse - Success")
@Composable
fun PackToWarehousePreviewSuccess() {
    MaterialTheme {
        Surface {
            PackToWarehouseContent(
                materialArticle = "ART-12345-X",
                cameraScanEnabled = true,
                onCameraScanClick = {},
                onMaterialChange = {},
                quantity = "50",
                onQuantityChange = {},
                uniqueCode = "BOX-998877",
                onCodeChange = {},
                uiState = MainViewModel.UiState.Packed("Материал успешно упакован на склад"),
                onPackClick = {},
                onBackClick = {},
                onClearClick = {},
                onRetryClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Pack To Warehouse - Error")
@Composable
fun PackToWarehousePreviewError() {
    MaterialTheme {
        Surface {
            PackToWarehouseContent(
                materialArticle = "ART-12345-X",
                cameraScanEnabled = true,
                onCameraScanClick = {},
                onMaterialChange = {},
                quantity = "50",
                onQuantityChange = {},
                uniqueCode = "BOX-998877",
                onCodeChange = {},
                uiState = MainViewModel.UiState.Error("Error"),
                onPackClick = {},
                onBackClick = {},
                onClearClick = {},
                onRetryClick = {}
            )
        }
    }
}