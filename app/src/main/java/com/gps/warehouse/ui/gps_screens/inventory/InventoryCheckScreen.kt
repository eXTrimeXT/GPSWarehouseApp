package com.gps.warehouse.ui.gps_screens.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.InventoryMaterialDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager

@Composable
fun InventoryCheckScreen(
    orderNumber: String,
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }
    // Получаем FocusManager для управления клавиатурой
    val focusManager = LocalFocusManager.current
    // Создаём FocusRequester для инлайн-поля
    val quantityFocusRequester = remember { FocusRequester() }

    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }

    var selectedMaterial by remember { mutableStateOf<InventoryMaterialDto?>(null) }
    var inputQty by remember { mutableStateOf("") }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    val isOrderActive = mainViewModel.isInventoryActive

    fun processScannedData(scannedData: String){
        if (scannedData.isNotEmpty()) {
            if (!isOrderActive) {
                Toast.makeText(context, "Инвентаризация завершена. Изменения невозможны.", Toast.LENGTH_SHORT).show()
                return
            }
            handleInventoryScan(
                scannedData = scannedData,
                uiState = uiState,
                onMaterialFound = { material ->
                    selectedMaterial = material
                    inputQty = ""
                },
                onError = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    LaunchedEffect(orderNumber) {
        mainViewModel.loadInventoryMaterials(orderNumber)
    }

    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData ->
            processScannedData(scannedData)
        }
    }

    DisposableEffect(Unit) {
        scannerManager.init()
        onDispose { scannerManager.release() }
    }

    LaunchedEffect(selectedMaterial) {
        if (selectedMaterial != null) {
            kotlinx.coroutines.delay(50) // Небольшая задержка для надёжности
            quantityFocusRequester.requestFocus()
        }
    }

    // Диалог завершения
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            icon = { Icon(Icons.Default.DoneAll, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Завершить инвентаризацию?") },
            text = { Text("Вы уверены, что хотите завершить инвентаризацию заказа $orderNumber?") },
            confirmButton = {
                Button(onClick = { mainViewModel.finishInventoryOrder(orderNumber); showFinishConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Завершить")
                }
            },
            dismissButton = { TextButton(onClick = { showFinishConfirmDialog = false }) { Text("Отмена") } }
        )
    }

    // Диалог камеры
    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                showCameraDialog = false
            }
        )
    }

    InventoryCheckContent(
        orderNumber = orderNumber,
        uiState = uiState,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        isOrderActive = isOrderActive,
        selectedMaterial = selectedMaterial,
        inputQty = inputQty,
        onQtyChange = { inputQty = it },
        // ЛОГИКА Toggle: клик по выбранному → deselect + скрыть клавиатуру
        onMaterialSelect = { material ->
            if (selectedMaterial?.material == material.material) {
                // Уже выбран → снимаем выделение и скрываем клавиатуру
                selectedMaterial = null
                focusManager.clearFocus()
            } else {
                // Новый выбор
                selectedMaterial = material
                inputQty = ""
            }
        },
        onConfirmClick = {
            val mat = selectedMaterial
            if (mat != null) {
                val qty = inputQty.toIntOrNull() ?: 0
                if (qty >= 0) {
                    mainViewModel.checkInventoryMaterial(material = mat.material, order = orderNumber, qty = qty)
                    selectedMaterial = null // Сброс после отправки
                    focusManager.clearFocus() // Скрыть клавиатуру после успешной отправки
                } else {
                    Toast.makeText(context, "Количество не может быть меньше 0", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Выберите материал для сверки", Toast.LENGTH_SHORT).show()
            }
        },
        onBackClick = { navController.popBackStack() },
        onFinishClick = { showFinishConfirmDialog = true },
        onRetryClick = { mainViewModel.loadInventoryMaterials(orderNumber) },
        quantityFocusRequester = quantityFocusRequester
    )
}

private fun handleInventoryScan(
    scannedData: String,
    uiState: MainViewModel.UiState,
    onMaterialFound: (InventoryMaterialDto) -> Unit,
    onError: (String) -> Unit
) {
    // Попытка парсинга JSON из Base64 (если используется сложный QR)
    val materialArticle = BarcodeParser.parse(scannedData)?.material  ?: scannedData

    if (uiState is MainViewModel.UiState.InventoryMaterialsLoaded) {
        val materials = uiState.materials
        val foundMaterial = materials.find { it.material == materialArticle }

        if (foundMaterial != null) {
            onMaterialFound(foundMaterial)
        } else {
            onError("Материал $materialArticle не найден в списке")
        }
    } else {
        onError("Список материалов еще не загружен")
    }
}


@Composable
fun InventoryCheckContent(
    orderNumber: String,
    uiState: MainViewModel.UiState,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit,
    isOrderActive: Boolean,
    quantityFocusRequester: FocusRequester,
    selectedMaterial: InventoryMaterialDto?,
    inputQty: String,
    onQtyChange: (String) -> Unit,
    onMaterialSelect: (InventoryMaterialDto) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        var searchQuery by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // Автопрокрутка к выбранному материалу
        LaunchedEffect(selectedMaterial) {
            if (selectedMaterial != null && uiState is MainViewModel.UiState.InventoryMaterialsLoaded) {
                val index =
                    uiState.materials.indexOfFirst { it.material == selectedMaterial.material }
                if (index != -1) {
                    listState.animateScrollToItem(index)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ActionBar
            MyCustomActionBar(
                onBackClick = onBackClick,
                text = "Сверка: $orderNumber",
                actionButton = if (isOrderActive) {
                    {
                        IconButton(onClick = onFinishClick) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Завершить инвентаризацию",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null
            )

            // Поиск
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    singleLine = true
                )
            }

            // Статус заказа (если завершён)
            if (!isOrderActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Заказ завершен.\nРедактирование запрещено.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Список материалов
            when (uiState) {
                is MainViewModel.UiState.Loading -> CustomLoadingView()

                is MainViewModel.UiState.InventoryMaterialsLoaded -> {
                    val allMaterials = uiState.materials
                    val filteredMaterials = if (searchQuery.isBlank()) {
                        allMaterials
                    } else {
                        val query = searchQuery.lowercase()
                        allMaterials.filter {
                            it.material.lowercase().contains(query) ||
                                    (it.name?.lowercase()?.contains(query) ?: false)
                        }
                    }

                    if (filteredMaterials.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (allMaterials.isEmpty()) "Нет материалов" else "Ничего не найдено")
                                if (allMaterials.isNotEmpty() && searchQuery.isNotEmpty()) {
                                    TextButton(onClick = {
                                        searchQuery = ""
                                    }) { Text("Сбросить поиск") }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Найдено: ${filteredMaterials.size} из ${allMaterials.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }

                            items(filteredMaterials) { material ->
                                InventoryMaterialCard(
                                    material = material,
                                    isSelected = selectedMaterial?.material == material.material,
                                    onClick = { if (isOrderActive) onMaterialSelect(material) },
                                    inputQty = if (selectedMaterial?.material == material.material) inputQty else "",
                                    onQtyChange = if (selectedMaterial?.material == material.material) onQtyChange else { _ -> },
                                    onConfirmClick = onConfirmClick,
                                    focusRequester = if (selectedMaterial?.material == material.material) quantityFocusRequester else null,
                                )
                            }
                        }
                    }
                }

                is MainViewModel.UiState.InventoryFinished -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0, 150, 0, 170),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Инвентаризация завершена!",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0, 150, 0, 170)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onRetryClick) { Text("Обновить") }
                        }
                    }
                }

                is MainViewModel.UiState.Error -> {
                    ErrorStateView(
                        message = uiState.message,
                        onRetry = onRetryClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> CustomLoadingView()
            }
        }

        // Плавающая кнопка сканирования (если включено в настройках и инвентаризация активна)
        if (isOrderActive) {
            CameraScanButton(
                onClick = onCameraScanClick,
                cameraScanEnabled = cameraScanEnabled,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun InventoryMaterialCard(
    material: InventoryMaterialDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    inputQty: String = "",
    onQtyChange: (String) -> Unit = {},
    onConfirmClick: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    isLoading: Boolean = false
) {
    // Логика цветов
    val plan = material.count.toIntOrNull() ?: 0
    val fact = material.countFact.toIntOrNull() ?: 0
    val isComplete = plan >= 0 && plan == fact

    // Локальный TextFieldValue для управления курсором
    var textFieldValue by remember(inputQty) {
        mutableStateOf(
            TextFieldValue(
                text = inputQty,
                selection = TextRange(inputQty.length) // Курсор в конце
            )
        )
    }

    // Синхронизация: внешний inputQty → локальный textFieldValue
    LaunchedEffect(inputQty) {
        textFieldValue = TextFieldValue(
            text = inputQty,
            selection = TextRange(inputQty.length)
        )
    }

    // Цвет фона карточки
    val containerColor = when {
        material.isJustChecked -> Color.Green.copy(alpha = 0.2f)
        material.hasError -> Color.Red.copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок + статус
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Артикул: ${material.material}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                if (isComplete) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Сверено",
                        tint = Color(0, 150, 0, 255),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Не сверено",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (material.name != null) {
                Text(
                    "Наименование: ${material.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // План / Факт строка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("План: ${material.count}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(16.dp))

                // ✅ Факт — текст или инлайн-редактор
                if (isSelected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Факт:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                onQtyChange(newValue.text)
                            },
                            placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onConfirmClick() }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                            enabled = !isLoading,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = MaterialTheme.shapes.small
                        )

                        IconButton(
                            onClick = onConfirmClick,
                            enabled = textFieldValue.text.toIntOrNull() != null && !isLoading,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Подтвердить",
                                modifier = Modifier.size(18.dp),
                                tint = if (textFieldValue.text.toIntOrNull() != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Text(
                        "Факт: ${material.countFact}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Индикатор ошибки справа (если есть)
            if (material.hasError && !isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Ошибка",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// --- PREVIEWS ---
private val mockMaterials = listOf(
    InventoryMaterialDto(
        id = "1",
        material = "LA1203900151",
        name = "Принтер этикеток",
        count = "5",
        countFact = "2",
        EA = "шт",
        warehouse = "3051",
        numOrder = "GPS_INV_40"
    ),
    InventoryMaterialDto(
        id = "2",
        material = "LA0602600443",
        name = "СИГНАЛИЗАЦИОННАЯ ЛАМПА",
        count = "4",
        countFact = "0",
        EA = "шт",
        warehouse = "3051",
        numOrder = "GPS_INV_40"
    )
)

@Preview(showBackground = true, name = "Inventory Check - List Only")
@Composable
fun PreviewInventoryList() {
    MaterialTheme {
        Surface {
            InventoryCheckContent(
                orderNumber = "GPS_INV_40",
                uiState = MainViewModel.UiState.InventoryMaterialsLoaded(mockMaterials, "GPS_INV_40"),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                selectedMaterial = mockMaterials[0],
                inputQty = "1234567890",
                onQtyChange = {},
                onMaterialSelect = {},
                onConfirmClick = {},
                onBackClick = {},
                onRetryClick = {},
                isOrderActive = true,
                onFinishClick = {},
                quantityFocusRequester = FocusRequester.Default
            )
        }
    }
}