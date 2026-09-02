package com.gps.warehouse.ui.assets_screens.inventory

import android.widget.Toast
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
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.InventoryQrParser // Добавляем импорт

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
    val focusManager = LocalFocusManager.current
    val quantityFocusRequester = remember { FocusRequester() }

    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<InventorizationItemDto?>(null) }
    var inputQty by remember { mutableStateOf("") }
    var showCompleteDialog by remember { mutableStateOf(false) }

    // Используем вынесенный парсер
    fun processScannedData(scannedData: String) {
        if (scannedData.isEmpty()) return
        if (isCompleted) {
            Toast.makeText(context, "Инвентаризация завершена. Изменения невозможны.", Toast.LENGTH_SHORT).show()
            return
        }

        val parsedSerial = InventoryQrParser.parseSerialNumber(scannedData)
        if (parsedSerial != null) {
            val currentState = uiState
            if (currentState is AssetViewModel.AssetUiState.InventorizationItemsLoaded) {
                val foundAsset = currentState.items.find { it.serialNumber == parsedSerial }
                if (foundAsset != null) {
                    if (selectedAsset?.serialNumber == foundAsset.serialNumber) {
                        selectedAsset = null
                        focusManager.clearFocus()
                    } else {
                        selectedAsset = foundAsset
                        inputQty = ""
                    }
                } else {
                    Toast.makeText(context, "Серийный номер '$parsedSerial' не найден в сессии", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Список активов ещё не загружен", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Неверный формат QR-кода. Ожидается: ID&SerialNumber", Toast.LENGTH_SHORT).show()
        }
    }

    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                showCameraDialog = false
            }
        )
    }

    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null) {
            kotlinx.coroutines.delay(50)
            quantityFocusRequester.requestFocus()
        }
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
        quantityFocusRequester = quantityFocusRequester,
        onQtyChange = { inputQty = it },
        onAssetSelect = { asset ->
            if (selectedAsset?.assetId == asset.assetId) {
                selectedAsset = null
                focusManager.clearFocus()
            } else {
                selectedAsset = asset
                inputQty = asset.quantityFact?.toString() ?: ""
            }
        },
        onConfirmClick = {
            selectedAsset?.let { asset ->
                val qty = inputQty.toIntOrNull() ?: 0
                if (qty >= 0) {
                    viewModel.checkInventorizationItem(sessionId, asset.assetId, qty)
                    selectedAsset = null
                    focusManager.clearFocus()
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

// ==================== CONTENT: UI ====================
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
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedAsset) {
        if (selectedAsset != null && uiState is AssetViewModel.AssetUiState.InventorizationItemsLoaded) {
            val index = uiState.items.indexOfFirst { it.assetId == selectedAsset.assetId }
            if (index != -1) listState.animateScrollToItem(index)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            MyCustomActionBar(
                onBackClick = onBackClick,
                text = "Сверка: #$sessionId",
                actionButton = if (!isCompleted) {
                    {
                        IconButton(onClick = { onShowCompleteDialogChange(true) }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Завершить инвентаризацию", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else null
            )

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
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "Очистить") } }
                    } else null,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    singleLine = true
                )
            }

            if (isCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AssetViewModel.AssetUiState.InventorizationItemsLoaded -> {
                    val allItems = uiState.items
                    val filteredItems = if (searchQuery.isBlank()) allItems else {
                        val query = searchQuery.lowercase()
                        allItems.filter { it.assetId.toString().contains(query) || it.assetName.lowercase().contains(query) }
                    }

                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (allItems.isEmpty()) "Нет активов в сессии" else "Ничего не найдено")
                                if (allItems.isNotEmpty() && searchQuery.isNotEmpty()) {
                                    TextButton(onClick = { searchQuery = "" }) { Text("Сбросить поиск") }
                                }
                            }
                        }
                    } else {
                        val bottomPadding = if (cameraScanEnabled) 80.dp else 16.dp
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = bottomPadding
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
                                InventoryItemCard(
                                    item = item,
                                    isSelected = selectedAsset?.assetId == item.assetId,
                                    onClick = { if (!isCompleted) onAssetSelect(item) },
                                    inputQty = if (selectedAsset?.assetId == item.assetId) inputQty else "",
                                    onQtyChange = if (selectedAsset?.assetId == item.assetId) onQtyChange else { _ -> },
                                    onConfirmClick = onConfirmClick,
                                    focusRequester = if (selectedAsset?.assetId == item.assetId) quantityFocusRequester else null
                                )
                            }
                        }
                    }
                }
                is AssetViewModel.AssetUiState.Error -> {
                    ErrorStateView(message = uiState.message, onRetry = onRetry, modifier = Modifier.weight(1f))
                }
                else -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (!isCompleted) {
            CameraScanButton(
                onClick = onCameraScanClick,
                cameraScanEnabled = cameraScanEnabled,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }

    if (showCompleteDialog && !isCompleted) {
        AlertDialog(
            onDismissRequest = { onShowCompleteDialogChange(false) },
            icon = { Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Завершить инвентаризацию?") },
            text = { Text("Вы уверены, что хотите завершить инвентаризацию сессии #$sessionId? После этого редактирование будет невозможно.") },
            confirmButton = {
                Button(onClick = { onCompleteSession(); onShowCompleteDialogChange(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Завершить")
                }
            },
            dismissButton = { TextButton(onClick = { onShowCompleteDialogChange(false) }) { Text("Отмена") } }
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
    var textFieldValue by remember(inputQty) {
        mutableStateOf(TextFieldValue(text = inputQty, selection = TextRange(inputQty.length)))
    }

    LaunchedEffect(inputQty) {
        textFieldValue = TextFieldValue(text = inputQty, selection = TextRange(inputQty.length))
    }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.assetName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (item.isChecked) "Сверено" else "Не сверено",
                    tint = if (item.isChecked) Color(0, 150, 0, 255) else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text("Серийный номер: ${item.serialNumber}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("План: $plan", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(16.dp))

                if (isSelected) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Факт:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { newValue -> textFieldValue = newValue; onQtyChange(newValue.text) },
                            placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onConfirmClick() }),
                            modifier = Modifier.weight(1f).height(50.dp).then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                            enabled = !isLoading,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.small
                        )
                        IconButton(
                            onClick = onConfirmClick,
                            enabled = textFieldValue.text.toIntOrNull() != null && !isLoading,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Подтвердить", modifier = Modifier.size(18.dp),
                                tint = if (textFieldValue.text.toIntOrNull() != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    Text("Факт: ${fact ?: "–"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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
