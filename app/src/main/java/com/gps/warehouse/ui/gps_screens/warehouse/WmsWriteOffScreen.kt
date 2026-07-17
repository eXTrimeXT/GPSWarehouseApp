package com.gps.warehouse.ui.gps_screens.warehouse

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import  androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.WmsWriteOffItem
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.CustomVerticalScrollbar
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.decodeWmsWriteOffScreen
import com.gps.warehouse.utils.isBase64EncodedJson

// Enum для отслеживания фокуса в диалоге
enum class EditField {
    MATERIAL, QTY, STORAGE, COST_CENTER, GAL_ACCOUNT, POS_TEXT, INT_ORDER
}

// ====================== ЭКРАН ======================
@Composable
fun WmsWriteOffScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val TAG = "WmsWriteOffScreen"
    var writeOffItems by remember { mutableStateOf<List<WmsWriteOffItem>>(emptyList()) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Наблюдаем за доступными складами из ViewModel
    val availableWarehouses by viewModel.availableWarehouses.collectAsState()
    val availableStorages by remember(availableWarehouses) {
        mutableStateOf(availableWarehouses.filter { it.isVirtual == "0" }.map { it.name }.distinct().sorted())
    }

    // Состояния диалога редактирования
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Поля диалога
    var dialogMaterial by remember { mutableStateOf("") }
    var dialogQty by remember { mutableStateOf("1") }
    var dialogStorage by remember { mutableStateOf("") }
    var dialogCostCenter by remember { mutableStateOf("") }
    var dialogGalAccount by remember { mutableStateOf("") }
    var dialogPosText by remember { mutableStateOf("") }
    var dialogIntOrder by remember { mutableStateOf("") }

    // НОВОЕ: Состояние для отслеживания активного поля в диалоге (для сканирования)
    var focusedField by remember { mutableStateOf<EditField?>(null) }

    // Загружаем доступные склады при входе на экран
    LaunchedEffect(Unit) {
        viewModel.loadAvailableWarehouses()
    }

    // Хелпер для сканера Honeywell
    val honeywellHelper = remember { ScannerManager(context) }

    // Слушаем поток сканера
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val trimmedData = scannedData.trim()

                if (isBase64EncodedJson(trimmedData)) {
                    // JSON-скан: всегда в артикул, независимо от фокуса
                    val scanResult = decodeWmsWriteOffScreen(trimmedData)
                    Log.d(TAG, scanResult.toString())
                    if (scanResult != null) {
                        if (showDialog) {
                            dialogMaterial = scanResult.matNumScan
                        } else {
                            val newItem = WmsWriteOffItem(
                                material = scanResult.matNumScan,
                                qty = "1",
                                storage = availableStorages.firstOrNull() ?: "",
                                costcenter = null,
                                galaccount = null,
                                posText = null,
                                intOrder = null
                            )
                            writeOffItems = writeOffItems + newItem
                        }
                    } else {
                        Toast.makeText(context, "Ошибка распознавания данных скана!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Простой текст: сканируем в поле, которое сейчас в фокусе
                    if (showDialog) {
                        when (focusedField) {
                            EditField.COST_CENTER -> dialogCostCenter = trimmedData
                            EditField.GAL_ACCOUNT -> dialogGalAccount = trimmedData
                            EditField.POS_TEXT -> dialogPosText = trimmedData
                            EditField.INT_ORDER -> dialogIntOrder = trimmedData
                            EditField.MATERIAL -> dialogMaterial = trimmedData
                            // Если фокус на количестве или складе — по умолчанию в артикул
                            else -> dialogMaterial = trimmedData
                        }
                    } else {
                        // Диалог закрыт: создаем новый элемент
                        val newItem = WmsWriteOffItem(
                            material = trimmedData,
                            qty = "1",
                            storage = availableStorages.firstOrNull() ?: "",
                            costcenter = null,
                            galaccount = null,
                            posText = null,
                            intOrder = null
                        )
                        writeOffItems = writeOffItems + newItem
                    }
                }
            }
        }
    }

    // Инициализация сканера
    DisposableEffect(Unit) {
        honeywellHelper.init()
        onDispose {
            honeywellHelper.release()
            viewModel.resetWriteOffState()
        }
    }

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ/ДОБАВЛЕНИЯ ===
    if (showDialog) {
        EditWriteOffItemDialog(
            material = dialogMaterial,
            qty = dialogQty,
            storage = dialogStorage,
            costCenter = dialogCostCenter,
            galAccount = dialogGalAccount,
            posText = dialogPosText,
            intOrder = dialogIntOrder,
            availableStorages = availableStorages,
            isEditing = editingIndex != null,
            autoFocusCostCenter = editingIndex == null,
            // Передаем колбэк для отслеживания фокуса
            onFocusChanged = { field, isFocused ->
                if (isFocused) {
                    focusedField = field
                } else if (focusedField == field) {
                    focusedField = null
                }
            },
            onDismiss = {
                showDialog = false
                focusedField = null // Сбрасываем фокус при закрытии
            },
            onConfirm = { mat, qty, stor, cc, ga, pt, io ->
                val qtyInt = qty.toIntOrNull() ?: 1
                if (qtyInt > 0 && mat.isNotBlank() && stor.isNotBlank() && cc.isNotBlank()) {
                    val newItem = WmsWriteOffItem(
                        material = mat,
                        qty = qtyInt.toString(),
                        storage = stor,
                        costcenter = cc,
                        galaccount = ga.ifBlank { null },
                        posText = pt.ifBlank { null },
                        intOrder = io.ifBlank { null }
                    )
                    writeOffItems = if (editingIndex != null) {
                        writeOffItems.toMutableList().apply { set(editingIndex!!, newItem) }
                    } else {
                        writeOffItems + newItem
                    }
                    showDialog = false
                    focusedField = null
                } else if (cc.isBlank()) {
                    Toast.makeText(context, "Заполните поле МВЗ", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // === ДИАЛОГ УСПЕХА СПИСАНИЯ ===
    if (uiState is MainViewModel.UiState.WmsWriteOffSuccess) {
        val successState = uiState as MainViewModel.UiState.WmsWriteOffSuccess
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Списание завершено!") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(successState.message, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        writeOffItems = emptyList()
                        viewModel.resetWriteOffState()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("OK") }
            }
        )
    }

    WmsWriteOffScreenContent(
        writeOffItems = writeOffItems,
        availableStorages = availableStorages,
        onEditClick = { index ->
            val item = writeOffItems[index]
            dialogMaterial = item.material
            dialogQty = item.qty
            dialogStorage = item.storage
            dialogCostCenter = item.costcenter ?: ""
            dialogGalAccount = item.galaccount ?: ""
            dialogPosText = item.posText ?: ""
            dialogIntOrder = item.intOrder ?: ""
            editingIndex = index
            showDialog = true
        },
        onRemoveItem = { index ->
            writeOffItems = writeOffItems.toMutableList().apply { removeAt(index) }
        },
        onAddManualClick = {
            dialogMaterial = ""
            dialogQty = "1"
            dialogStorage = availableStorages.firstOrNull() ?: ""
            dialogCostCenter = ""
            dialogGalAccount = ""
            dialogPosText = ""
            dialogIntOrder = ""
            editingIndex = null
            showDialog = true
        },
        uiState = uiState,
        onWriteOffClick = {
            if (writeOffItems.isNotEmpty()) {
                viewModel.writeOffWmsMaterials(writeOffItems)
            } else {
                Toast.makeText(context, "Добавьте хотя бы один материал", Toast.LENGTH_SHORT).show()
            }
        },
        onRetryClick = { viewModel.loadWmsData() },
        onBackClick = { navController.popBackStack() }
    )
}

// ====================== ЧИСТЫЙ UI КОМПОНЕНТ ======================
@Composable
fun WmsWriteOffScreenContent(
    writeOffItems: List<WmsWriteOffItem>,
    availableStorages: List<String>,
    onEditClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddManualClick: () -> Unit,
    uiState: MainViewModel.UiState,
    onWriteOffClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Списание материалов",
            actionButton = {
                IconButton(onClick = onAddManualClick) {
                    Icon(
                        Icons.Default.AddBox,
                        contentDescription = "Добавить вручную",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        )

        if (uiState !is MainViewModel.UiState.WmsWriteOffSuccess &&
            uiState !is MainViewModel.UiState.Loading &&
            uiState !is MainViewModel.UiState.Error
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onWriteOffClick,
                    enabled = writeOffItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выполнить списание (${writeOffItems.sumOf { it.qty.toIntOrNull() ?: 0 }} шт.)")
                }
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> CustomLoadingView()
            is MainViewModel.UiState.Error -> ErrorStateView(
                message = uiState.message,
                onRetry = onRetryClick,
                modifier = Modifier.weight(1f)
            )
            else -> {
                RenderWriteOffList(
                    items = writeOffItems,
                    onEditClick = onEditClick,
                    onRemoveItem = onRemoveItem
                )
            }
        }
    }
}

// ====================== СПИСОК МАТЕРИАЛОВ ======================
@Composable
fun RenderWriteOffList(
    items: List<WmsWriteOffItem>,
    onEditClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Список пуст.\nОтсканируйте QR или добавьте вручную.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.material} ${item.qty} $index" }
            ) { index, item ->
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Артикул: ${item.material}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Количество: ${item.qty}", style = MaterialTheme.typography.bodyMedium)
                                Text("Склад: ${item.storage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                if (!item.costcenter.isNullOrBlank()) Text("МВЗ: ${item.costcenter}", style = MaterialTheme.typography.bodySmall)
                                if (!item.galaccount.isNullOrBlank()) Text("Счет гл.книги: ${item.galaccount}", style = MaterialTheme.typography.bodySmall)
                                if (!item.posText.isNullOrBlank()) Text("Текст позиции: ${item.posText}", style = MaterialTheme.typography.bodySmall)
                                if (!item.intOrder.isNullOrBlank()) Text("Заказ: ${item.intOrder}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onEditClick(index) }) {
                                Icon(Icons.Default.Edit, "Редактировать", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onRemoveItem(index) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================== ДИАЛОГ РЕДАКТИРОВАНИЯ ======================
@Composable
fun EditWriteOffItemDialog(
    material: String,
    qty: String,
    storage: String,
    costCenter: String,
    galAccount: String,
    posText: String,
    intOrder: String,
    availableStorages: List<String>,
    isEditing: Boolean,
    autoFocusCostCenter: Boolean = false,
    onFocusChanged: (EditField, Boolean) -> Unit, // НОВЫЙ ПАРАМЕТР
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    // FocusRequester для поля МВЗ
    val costCenterFocusRequester = remember { FocusRequester() }
    // ScrollState для scrollbar
    val scrollState = rememberScrollState()

    // Состояния полей
    var materialField by remember { mutableStateOf(TextFieldValue(material, TextRange(material.length))) }
    var qtyField by remember { mutableStateOf(TextFieldValue(qty, TextRange(qty.length))) }
    var storageField by remember { mutableStateOf(storage) }
    var costCenterField by remember { mutableStateOf(TextFieldValue(costCenter, TextRange(costCenter.length))) }
    var galAccountField by remember { mutableStateOf(TextFieldValue(galAccount, TextRange(galAccount.length))) }
    var posTextField by remember { mutableStateOf(TextFieldValue(posText, TextRange(posText.length))) }
    var intOrderField by remember { mutableStateOf(TextFieldValue(intOrder, TextRange(intOrder.length))) }

    // Обновление полей при изменении параметров
    LaunchedEffect(material) { materialField = TextFieldValue(material, TextRange(material.length)) }
    LaunchedEffect(qty) { qtyField = TextFieldValue(qty, TextRange(qty.length)) }
    LaunchedEffect(storage) { storageField = storage }
    LaunchedEffect(costCenter) { costCenterField = TextFieldValue(costCenter, TextRange(costCenter.length)) }
    LaunchedEffect(galAccount) { galAccountField = TextFieldValue(galAccount, TextRange(galAccount.length)) }
    LaunchedEffect(posText) { posTextField = TextFieldValue(posText, TextRange(posText.length)) }
    LaunchedEffect(intOrder) { intOrderField = TextFieldValue(intOrder, TextRange(intOrder.length)) }

    // АВТОФОКУС НА МВЗ при открытии диалога для нового материала
    LaunchedEffect(autoFocusCostCenter) {
        if (autoFocusCostCenter) {
            costCenterFocusRequester.requestFocus()
        }
    }

    // Высота диалога (ограничение 60% экрана)
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp * 0.59).dp

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.99f)
            .heightIn(max = maxDialogHeight),
        title = {
            Text(
                text = if (isEditing) "Редактирование" else "Новый материал",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Box позволяет наложить скроллбар поверх прокручиваемого контента
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp)
                        .padding(end = 16.dp), // Увеличенный отступ, чтобы поля не перекрывались скроллбаром
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Артикул
                    OutlinedTextField(
                        value = materialField,
                        onValueChange = {
                            materialField = it.copy(selection = TextRange(it.text.length))
                        },
                        label = {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("Артикул", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    " *",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                onFocusChanged(EditField.MATERIAL, focusState.isFocused)
                            },
                        singleLine = true,
                        readOnly = isEditing,
                        textStyle = MaterialTheme.typography.titleMedium,
                        isError = materialField.text.isBlank()
                    )

                    // Количество
                    OutlinedTextField(
                        value = qtyField,
                        onValueChange = {
                            if (it.text.all { it.isDigit() } && it.text.length <= 5)
                                qtyField = it.copy(selection = TextRange(it.text.length))
                        },
                        label = { Text("Количество", style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                onFocusChanged(EditField.QTY, focusState.isFocused)
                            },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium
                    )

                    // Склад (через чипсы)
                    Text("Склад", style = MaterialTheme.typography.bodyLarge)
                    if (availableStorages.isEmpty()) {
                        Text(
                            "Нет доступных складов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableStorages.forEach { stor ->
                                FilterChip(
                                    selected = storageField == stor,
                                    onClick = {
                                        storageField = stor
                                        onFocusChanged(EditField.STORAGE, true)
                                    },
                                    label = { Text(stor) }
                                )
                            }
                        }
                    }

                    // МВЗ
                    OutlinedTextField(
                        value = costCenterField,
                        onValueChange = {
                            costCenterField = it.copy(selection = TextRange(it.text.length))
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("МВЗ", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    " *",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(costCenterFocusRequester)
                            .onFocusChanged { focusState ->
                                onFocusChanged(EditField.COST_CENTER, focusState.isFocused)
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        isError = costCenterField.text.isBlank() && !isEditing
                    )
                    if (costCenterField.text.isBlank() && !isEditing) {
                        Text(
                            text = "Поле обязательно для заполнения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Счет ГЛ
                    OutlinedTextField(
                        value = galAccountField,
                        onValueChange = {
                            galAccountField = it.copy(selection = TextRange(it.text.length))
                        },
                        label = { Text("Счет гл.книги") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                onFocusChanged(EditField.GAL_ACCOUNT, focusState.isFocused)
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    // Текст позиции
                    OutlinedTextField(
                        value = posTextField,
                        onValueChange = {
                            posTextField = it.copy(selection = TextRange(it.text.length))
                        },
                        label = { Text("Текст позиции") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                onFocusChanged(EditField.POS_TEXT, focusState.isFocused)
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    // Внутренний заказ
                    OutlinedTextField(
                        value = intOrderField,
                        onValueChange = {
                            intOrderField = it.copy(selection = TextRange(it.text.length))
                        },
                        label = { Text("Внутренний заказ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                onFocusChanged(EditField.INT_ORDER, focusState.isFocused)
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                // Кастомный видимый ползунок прокрутки
                CustomVerticalScrollbar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (costCenterField.text.isBlank()) {
                        Toast.makeText(context, "Заполните поле МВЗ", Toast.LENGTH_SHORT).show()
                        costCenterFocusRequester.requestFocus()
                        return@Button
                    }
                    onConfirm(
                        materialField.text,
                        qtyField.text,
                        storageField,
                        costCenterField.text,
                        galAccountField.text,
                        posTextField.text,
                        intOrderField.text
                    )
                },
                enabled = costCenterField.text.isNotBlank() || isEditing
            ) {
                Text("Сохранить", style = MaterialTheme.typography.titleSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", style = MaterialTheme.typography.titleSmall)
            }
        }
    )
}

// ====================== ПРЕВЬЮ ======================
@Preview(showBackground = true, name = "WmsWriteOff - Empty")
@Composable
fun WmsWriteOffPreviewEmpty() {
    MaterialTheme {
        Surface {
            WmsWriteOffScreenContent(
                writeOffItems = emptyList(),
                availableStorages = listOf("3051", "3052", "4007"),
                onEditClick = {},
                onRemoveItem = {},
                onAddManualClick = {},
                uiState = MainViewModel.UiState.Idle,
                onWriteOffClick = {},
                onRetryClick = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "WmsWriteOff - With Items")
@Composable
fun WmsWriteOffPreviewItems() {
    MaterialTheme {
        Surface {
            WmsWriteOffScreenContent(
                writeOffItems = listOf(
                    WmsWriteOffItem("M00000031791", "1", "3051", "RU01050020", null, null, null),
                    WmsWriteOffItem("LA0610501327", "3", "3052", null, null, "Тест", "ORD-123")
                ),
                availableStorages = listOf("3051", "3052", "4007"),
                onEditClick = {},
                onRemoveItem = {},
                onAddManualClick = {},
                uiState = MainViewModel.UiState.Idle,
                onWriteOffClick = {},
                onRetryClick = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dialog Preview", device = "spec:width=400dp,height=1450dp")
@Composable
fun EditWriteOffItemDialogPreview() {
    EditWriteOffItemDialog(
        material = "",
        qty = "",
        storage = "",
        costCenter = "",
        galAccount = "",
        posText = "",
        intOrder = "",
        availableStorages = listOf("1", "2"),
        isEditing = true,
        onDismiss = {},
        onFocusChanged = { _, _ -> },
        onConfirm = { _, _, _, _, _, _, _ -> }
    )
}