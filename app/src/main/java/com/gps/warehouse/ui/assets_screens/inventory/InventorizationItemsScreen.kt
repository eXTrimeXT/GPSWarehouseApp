package com.gps.warehouse.ui.assets_screens.inventory

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.ScannedData
import com.gps.warehouse.utils.ScannerManager

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationItemsScreen(
    sessionId: Int,
    isCompleted: Boolean,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }

    // Получаем FocusManager для управления клавиатурой
    val focusManager = LocalFocusManager.current
    // Создаём FocusRequester для инлайн-поля
    val quantityFocusRequester = remember { FocusRequester() }

    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }

    // Состояние выбранного актива для ввода количества
    var selectedAsset by remember { mutableStateOf<InventorizationItemDto?>(null) }
    // Состояние количества для ввода
    var inputQty by remember { mutableStateOf("") }
    // Состояние для диалога подтверждения завершения
    var showCompleteDialog by remember { mutableStateOf(false) }

    /**
     * Парсит QR-код инвентаризации формата: "ID&SerialNumber"
     * @param scannedData строка вида "123&SN-ABC-001"
     * @return SerialNumber (String?) или null если формат неверный
     */
    fun parseInventorizationQR(scannedData: String): String? {
        return try {
            // Ожидаем формат: "123&SN-ABC-001"
            val parts = scannedData.split("&", limit = 2)
            if (parts.size == 2) {
                // Возвращаем SerialNumber (вторая часть)
                parts[1].trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Функция обработки отсканированных данных
    fun processScannedData(scannedData: String) {
        if (scannedData.isEmpty()) return

        // Если сессия завершена — игнорируем сканирование
        if (isCompleted) {
            Toast.makeText(
                context,
                "Инвентаризация завершена. Изменения невозможны.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Парсим QR: "ID&SerialNumber"
        val scannedAssetId = parseInventorizationQR(scannedData)

        if (scannedAssetId != null) {
            // Ищем актив в текущем состоянии UI
            val currentState = uiState
            if (currentState is AssetViewModel.AssetUiState.InventorizationItemsLoaded) {
                val foundAsset = currentState.items.find { it.serialNumber == scannedAssetId }

                if (foundAsset != null) {
                    // Имитируем клик по элементу: toggle-логика как в UI
                    if (selectedAsset?.serialNumber == foundAsset.serialNumber) {
                        // Уже выбран → deselect + скрыть клавиатуру
                        selectedAsset = null
                        focusManager.clearFocus()
                    } else {
                        // Новый выбор → select + сброс количества
                        selectedAsset = foundAsset
                        inputQty = ""
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Актив #$scannedAssetId не найден в сессии",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Список активов ещё не загружен", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Неверный формат QR-кода. Ожидается: ID&SerialNumber",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    // При выборе актива — фокус на поле
    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null) {
            quantityFocusRequester.requestFocus()
        }
    }

    // Слушаем события от Honeywell-сканера
    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData ->
            processScannedData(scannedData)
        }
    }

    // Инициализация/очистка сканера
    DisposableEffect(Unit) {
        scannerManager.init()
        onDispose { scannerManager.release() }
    }

    LaunchedEffect(sessionId) {
        viewModel.loadInventorizationItems(sessionId)
    }

    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null) {
            kotlinx.coroutines.delay(50) // Небольшая задержка для надёжности
            quantityFocusRequester.requestFocus()
        }
    }

    InventorizationItemsContent(
        sessionId = sessionId,
        uiState = uiState,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        isCompleted = isCompleted,
        selectedAsset = selectedAsset,
        inputQty = inputQty,
        showCompleteDialog = showCompleteDialog,
        quantityFocusRequester = quantityFocusRequester,
        onQtyChange = { inputQty = it },
        // ЛОГИКА TOGGLE: клик по выбранному → deselect + скрыть клавиатуру
        onAssetSelect = { asset ->
            if (selectedAsset?.assetId == asset.assetId) {
                // Уже выбран → снимаем выделение и скрываем клавиатуру
                selectedAsset = null
                focusManager.clearFocus()
            } else {
                // Новый выбор
                selectedAsset = asset
                // заполняем quantityFact, если он есть
                inputQty = asset.quantityFact?.toString() ?: ""
            }
        },
        onConfirmClick = {
            selectedAsset?.let { asset ->
                val qty = inputQty.toIntOrNull() ?: 0
                if (qty >= 0) {
                    viewModel.checkInventorizationItem(sessionId, asset.assetId, qty)
                    selectedAsset = null // Сброс после отправки
                    focusManager.clearFocus() // Скрыть клавиатуру после успешной отправки
                }
            }
        },
        onShowCompleteDialogChange = { showCompleteDialog = it },
        onCompleteSession = {
            viewModel.completeInventorizationSession(sessionId)
            navController.popBackStack()
        },
        onDetailsClick = { assetId -> navController.navigate("asset_details/$assetId") },
        onRetry = { viewModel.loadInventorizationItems(sessionId) },
        onBackClick = { navController.popBackStack() }
    )
}

// ==================== CONTENT: UI + Preview ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationItemsContent(
    sessionId: Int,
    uiState: AssetViewModel.AssetUiState,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit,
    isCompleted: Boolean,
    selectedAsset: InventorizationItemDto?,
    inputQty: String,
    showCompleteDialog: Boolean,
    quantityFocusRequester: FocusRequester,
    onQtyChange: (String) -> Unit,
    onAssetSelect: (InventorizationItemDto) -> Unit,
    onConfirmClick: () -> Unit,
    onShowCompleteDialogChange: (Boolean) -> Unit,
    onCompleteSession: () -> Unit,
    onDetailsClick: (assetId: Int) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    // Состояние для поиска
    var searchQuery by remember { mutableStateOf("") }
    // Создаем состояние списка для управления прокруткой
    val listState = rememberLazyListState()

    // Автопрокрутка к выбранному активу
    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null && uiState is AssetViewModel.AssetUiState.InventorizationItemsLoaded) {
            val index = uiState.items.indexOfFirst { it.assetId == selectedAsset.assetId }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        }
    }

    // Root Box для правильного позиционирования плавающих элементов
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ActionBar с кнопкой завершения справа (только если сессия активна)
            MyCustomActionBar(
                onBackClick = onBackClick,
                text = "Сверка: #$sessionId",
                actionButton = if (!isCompleted) {
                    {
                        IconButton(onClick = { onShowCompleteDialogChange(true) }) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Завершить инвентаризацию",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null
            )

            // Панель поиска
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
                        { IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }}
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    singleLine = true
                )
            }

            // Индикатор статуса сессии (если завершена)
            if (isCompleted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Инвентаризация завершена.\nРедактирование запрещено.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            when (uiState) {
                is AssetViewModel.AssetUiState.Loading -> {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AssetViewModel.AssetUiState.InventorizationItemsLoaded -> {
                    val allItems = uiState.items
                    // Фильтрация списка
                    val filteredItems = if (searchQuery.isBlank()) {
                        allItems
                    } else {
                        val query = searchQuery.lowercase()
                        allItems.filter {
                            it.assetId.toString().contains(query) ||
                                    it.assetName.lowercase().contains(query)
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (allItems.isEmpty()) "Нет активов в сессии" else "Ничего не найдено")
                                if (allItems.isNotEmpty() && searchQuery.isNotEmpty()) {
                                    TextButton(onClick = { searchQuery = "" }) {
                                        Text("Сбросить поиск")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                // Динамический отступ снизу:
                                // - 80.dp если показан ActionPanel (чтобы не перекрывался FAB)
                                // - 72.dp если показана только кнопка камеры
                                // - 16.dp по умолчанию
                                bottom = if (selectedAsset != null && !isCompleted) {
                                    80.dp // ActionPanel + запас
                                } else if (cameraScanEnabled && !isCompleted) {
                                    72.dp // Только FAB камеры
                                } else {
                                    16.dp
                                }
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Найдено: ${filteredItems.size} из ${allItems.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }

                            items(filteredItems) { item ->
                                // Карточка актива
                                InventoryItemCard(
                                    item = item,
                                    isSelected = selectedAsset?.assetId == item.assetId,
                                    onClick = { if (!isCompleted) onAssetSelect(item) },
                                    // Передаём параметры для инлайн-редактирования
                                    inputQty = if (selectedAsset?.assetId == item.assetId) inputQty else "",
                                    onQtyChange = if (selectedAsset?.assetId == item.assetId) onQtyChange else {_ -> },
                                    onConfirmClick = onConfirmClick,
                                    focusRequester = if (selectedAsset?.assetId == item.assetId) quantityFocusRequester else null
                                )
                            }
                        }
                    }
                }
                is AssetViewModel.AssetUiState.Error -> {
                    ErrorStateView(
                        message = uiState.message,
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Плавающая кнопка сканирования
        if (!isCompleted) {
            CameraScanButton(
                onClick = onCameraScanClick,
                cameraScanEnabled = cameraScanEnabled,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    // Диалог подтверждения завершения
    if (showCompleteDialog && !isCompleted) {
        AlertDialog(
            onDismissRequest = { onShowCompleteDialogChange(false) },
            icon = {
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Завершить инвентаризацию?") },
            text = {
                Text("Вы уверены, что хотите завершить инвентаризацию сессии #$sessionId? После этого редактирование будет невозможно.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCompleteSession()
                        onShowCompleteDialogChange(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowCompleteDialogChange(false) }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// ==================== КАРТОЧКА АКТИВА ====================
@Composable
fun InventoryItemCard(
    item: InventorizationItemDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    inputQty: String = "",
    onQtyChange: (String) -> Unit = {},
    onConfirmClick: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    isLoading: Boolean = false
) {
    val plan = item.quantity
    val fact = item.quantityFact

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
            selection = TextRange(inputQty.length) // Всегда в конце
        )
    }

    val containerColor = when {
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
                    item.assetName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (item.isChecked) "Сверено" else "Не сверено",
                    tint = if (item.isChecked) Color(0, 150, 0, 255) else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                "Серийный номер: ${item.serialNumber}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // План / Факт строка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // План — всегда текст
                Text("План: $plan", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.width(16.dp))

                // Факт — текст или инлайн-редактор
                if (isSelected) {
                    // Инлайн-редактор "Факт"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Факт:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                onQtyChange(newValue.text) // Передаём строку наружу
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,

                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = MaterialTheme.shapes.small
                        )

                        // Кнопка подтверждения
                        IconButton(
                            onClick = onConfirmClick,
                            enabled = textFieldValue.text.toIntOrNull() != null && !isLoading,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Подтвердить",
                                modifier = Modifier.size(18.dp),
                                tint = if (inputQty.toIntOrNull() != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    // Обычный текст "Факт"
                    Text(
                        "Факт: ${fact ?: "–"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================
@PreviewLightDark
@Composable
private fun InventorizationItemsContentPreview() {
    MaterialTheme {
        Surface {
            InventorizationItemsContent(
                sessionId = 42,
                uiState = AssetViewModel.AssetUiState.InventorizationItemsLoaded(
                    sessionId = 42,
                    items = listOf(
                        InventorizationItemDto(1, 42, 101, "serial_number", "Компьютер Dell", true, 10, 8),
                        InventorizationItemDto(2, 42, 102, "serial_number","Монитор LG", false, 5, null),
                        InventorizationItemDto(3, 42, 103, "serial_number","Клавиатура", false, 20, null)
                    )
                ),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                isCompleted = false,
                selectedAsset = InventorizationItemDto(1, 42, 101, "serial_number", "Компьютер Dell", true, 10, 8),
                inputQty = "",
                showCompleteDialog = false,
                quantityFocusRequester = FocusRequester.Default,
                onQtyChange = {},
                onAssetSelect = {},
                onConfirmClick = {},
                onShowCompleteDialogChange = {},
                onCompleteSession = {},
                onDetailsClick = {},
                onRetry = {},
                onBackClick = {}
            )
        }
    }
}