package com.gps.warehouse.ui.assets_screens.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

    // TODO: Добавить сканирование с камеры и сканера!
    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }

    // Состояние выбранного актива для ввода количества
    var selectedAsset by remember { mutableStateOf<InventorizationItemDto?>(null) }
    // Состояние количества для ввода
    var inputQty by remember { mutableStateOf("") }
    // Состояние для диалога подтверждения завершения
    var showCompleteDialog by remember { mutableStateOf(false) }

    fun processScannedData(scannedData: String){
        // TODO: Логика сканирования и парсинга QR кода активов
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

    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData ->
            processScannedData(scannedData)
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.loadInventorizationItems(sessionId)
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
                inputQty = ""
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
                            contentPadding = PaddingValues(16.dp),
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
                                    onClick = { if (!isCompleted) onAssetSelect(item) }
                                )

                                // ActionPanel появляется ПОД выбранным элементом (внутри списка!)
                                if (!isCompleted && selectedAsset?.assetId == item.assetId) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    ActionPanel(
                                        selectedAsset = selectedAsset,
                                        inputQty = inputQty,
                                        onQtyChange = onQtyChange,
                                        onConfirmClick = onConfirmClick,
                                        isLoading = uiState is AssetViewModel.AssetUiState.Loading,
                                        modifier = Modifier.animateItem() // Плавное появление
                                    )
                                }
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
    onClick: () -> Unit
) {
    // Логика цветов
    val plan = item.quantity
    val fact = item.quantityFact

    // Цвет фона карточки
    val containerColor = when {
//        item.isChecked -> Color.Green.copy(alpha = 0.2f) // Зеленый при успехе
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.assetName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    // Галочка, если проверен
                    if (item.isChecked) {
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

                Text(
                    "Серийный номер: ${item.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("План: $plan", style = MaterialTheme.typography.bodySmall)
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

// ==================== PANEL ВВОДА КОЛИЧЕСТВА (компактный) ====================
@Composable
fun ActionPanel(
    selectedAsset: InventorizationItemDto?,
    inputQty: String,
    onQtyChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val quantityFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null) {
            quantityFocusRequester.requestFocus()
        }
    }

    // Компактный стиль: Row с background вместо Card
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
    ) {
        OutlinedTextField(
            value = inputQty,
            onValueChange = onQtyChange,
            placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .focusRequester(quantityFocusRequester),
            enabled = selectedAsset != null && !isLoading,
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
            enabled = selectedAsset != null && inputQty.toIntOrNull() != null && !isLoading,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "OK", modifier = Modifier.size(20.dp))
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
                selectedAsset = null,
                inputQty = "",
                showCompleteDialog = false,
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