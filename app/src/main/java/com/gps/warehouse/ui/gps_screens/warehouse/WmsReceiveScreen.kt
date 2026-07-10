package com.gps.warehouse.ui.gps_screens.warehouse

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.WmsReceiveItem
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.decodeWmsScanData
import com.gps.warehouse.utils.isBase64EncodedJson
import java.text.SimpleDateFormat
import java.util.*

// ====================== 1. ЭКРАН ======================
@Composable
fun WmsReceiveScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    orderNumber: String = ""
) {
    var receiveItems by remember { mutableStateOf<List<WmsReceiveItem>>(emptyList()) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Состояния диалога редактирования
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var dialogOrderNumber by remember { mutableStateOf(orderNumber) }
    var dialogMaterial by remember { mutableStateOf("") }
    var dialogQty by remember { mutableStateOf("1") }
    var dialogQuality by remember { mutableStateOf(true) }
    var dialogExpi by remember { mutableStateOf("") }

    // НОВОЕ: Состояния для DatePicker в СПИСКЕ
    var showListDatePicker by remember { mutableStateOf(false) }
    var listDatePickerIndex by remember { mutableStateOf<Int?>(null) }
    var listDatePickerInitialDate by remember { mutableStateOf<Long?>(null) }

    // Состояния для DatePicker в ДИАЛОГЕ
    var showEditDatePicker by remember { mutableStateOf(false) }
    var editDatePickerInitialDate by remember { mutableStateOf<Long?>(null) }

    val honeywellHelper = remember { ScannerManager(context) }

    // Слушаем сканер
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val trimmedData = scannedData.trim()
                if (isBase64EncodedJson(trimmedData)) {
                    val scanResult = decodeWmsScanData(trimmedData)
                    if (scanResult != null) {
                        if (showDialog) {
                            dialogMaterial = scanResult.matNumScan
                        } else {
                            val orderNumberToUse =
                                if (scanResult.matNumOrder == orderNumber || orderNumber.isEmpty()) {
                                    scanResult.matNumOrder
                                } else {
                                    orderNumber
                                }
                            val newItem = WmsReceiveItem(
                                matNumScan = scanResult.matNumScan,
                                matNumOrder = orderNumberToUse,
                                matQtyScan = scanResult.matQtyScan,
                                checkQuality = true,
                                Expi = ""
                            )
                            receiveItems = receiveItems + newItem
                        }
                    } else {
                        Toast.makeText(context, "Ошибка распознавания данных скана!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Неизвестный формат QR кода!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Инициализация сканера
    DisposableEffect(Unit) {
        honeywellHelper.init(
//            onInitialized = { honeywellHelper.enableScanner(true) },
//            onError = { e ->
//                Toast.makeText(context, "Ошибка сканера: ${e.message}", Toast.LENGTH_LONG).show()
//            }
        )
        onDispose {
//            honeywellHelper.enableScanner(false)
            honeywellHelper.release()
            viewModel.resetReceiveState()
        }
    }

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ===
    if (showDialog) {
        EditReceiveItemDialog(
            material = dialogMaterial,
            qty = dialogQty,
            orderNumber = dialogOrderNumber,
            quality = dialogQuality,
            expi = dialogExpi,
            isEditing = editingIndex != null,
            onDismiss = { showDialog = false },
            onConfirm = { mat, qty, ord, qual, exp ->
                val qtyInt = qty.toIntOrNull() ?: 1
                val finalOrder = ord.ifBlank { orderNumber }
                if (qtyInt > 0 && mat.isNotBlank() && finalOrder.isNotBlank()) {
                    val newItem = WmsReceiveItem(
                        matNumScan = mat,
                        matNumOrder = finalOrder,
                        matQtyScan = qtyInt,
                        checkQuality = qual,
                        Expi = exp
                    )
                    receiveItems = if (editingIndex != null) {
                        receiveItems.toMutableList().apply { set(editingIndex!!, newItem) }
                    } else {
                        receiveItems + newItem
                    }
                    showDialog = false
                }
            },
            onDateClick = {
                editDatePickerInitialDate = parseDate(dialogExpi)?.time
                showEditDatePicker = true
            }
        )
    }

    // DatePicker для ДИАЛОГА редактирования
    if (showEditDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editDatePickerInitialDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dialogExpi = formatDate(millis)
                    }
                    showEditDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // DatePicker для СПИСКА (по клику на дату в карточке)
    if (showListDatePicker && listDatePickerIndex != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = listDatePickerInitialDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = {
                showListDatePicker = false
                listDatePickerIndex = null
            },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formattedDate = formatDate(millis)
                        receiveItems = receiveItems.toMutableList().apply {
                            listDatePickerIndex?.let { idx ->
                                set(idx, get(idx).copy(Expi = formattedDate))
                            }
                        }
                    }
                    showListDatePicker = false
                    listDatePickerIndex = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showListDatePicker = false
                    listDatePickerIndex = null
                }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // === ДИАЛОГ УСПЕХА ПРИЕМКИ ===
    if (uiState is MainViewModel.UiState.WmsReceiveSuccess) {
        val successState = uiState as MainViewModel.UiState.WmsReceiveSuccess
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Заказ завершен!") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(successState.message, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        receiveItems = emptyList()
                        viewModel.resetReceiveState()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("OK") }
            }
        )
    }

    WmsReceiveScreenContent(
        receiveItems = receiveItems,
        orderNumber = orderNumber,
        onEditClick = { index ->
            val item = receiveItems[index]
            dialogMaterial = item.matNumScan
            dialogQty = item.matQtyScan.toString()
            dialogOrderNumber = item.matNumOrder
            dialogQuality = item.checkQuality
            dialogExpi = item.Expi
            editingIndex = index
            showDialog = true
        },
        onRemoveItem = { index ->
            receiveItems = receiveItems.toMutableList().apply { removeAt(index) }
        },
        onToggleQuality = { index ->
            receiveItems = receiveItems.toMutableList().apply {
                set(index, get(index).copy(checkQuality = !get(index).checkQuality))
            }
        },
        // Колбэк для открытия DatePicker из списка
        onExpiDateClick = { index, currentExpi ->
            listDatePickerIndex = index
            listDatePickerInitialDate = parseDate(currentExpi)?.time
            showListDatePicker = true
        },
        onAddManualClick = {
            dialogMaterial = ""
            dialogQty = "1"
            dialogOrderNumber = orderNumber.ifEmpty { "" }
            dialogQuality = true
            dialogExpi = ""
            editingIndex = null
            showDialog = true
        },
        uiState = uiState,
        onReceiveClick = {
            if (receiveItems.isNotEmpty()) {
                viewModel.receiveWmsMaterials(orderNumber, receiveItems)
            } else {
                Toast.makeText(context, "Добавьте хотя бы один материал", Toast.LENGTH_SHORT).show()
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}

// Вспомогательные функции для работы с датой
private fun parseDate(dateString: String): Date? {
    return try {
        if (dateString.isBlank()) return null
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        formatter.parse(dateString)
    } catch (e: Exception) {
        null
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}

// ====================== 2. ЧИСТЫЙ UI КОМПОНЕНТ ======================
@Composable
fun WmsReceiveScreenContent(
    receiveItems: List<WmsReceiveItem>,
    orderNumber: String,
    onEditClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleQuality: (Int) -> Unit,
    onExpiDateClick: (Int, String) -> Unit, // Новый колбэк для даты
    onAddManualClick: () -> Unit,
    uiState: MainViewModel.UiState,
    onReceiveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Приемка",
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

        if (uiState !is MainViewModel.UiState.WmsReceiveSuccess &&
            uiState !is MainViewModel.UiState.Loading &&
            uiState !is MainViewModel.UiState.Error
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onReceiveClick,
                    enabled = receiveItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Завершить приемку (${receiveItems.sumOf { it.matQtyScan }} шт.)")
                }
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> CustomLoadingView()
            is MainViewModel.UiState.Error -> ErrorStateView(
                message = uiState.message,
                onRetry = { null },
                modifier = Modifier.weight(1f)
            )
            else -> {
                RenderReceiveList(
                    items = receiveItems,
                    onEditClick = onEditClick,
                    onRemoveItem = onRemoveItem,
                    onToggleQuality = onToggleQuality,
                    onExpiDateClick = onExpiDateClick // Передаем колбэк
                )
            }
        }
    }
}

// ====================== 3. СПИСОК МАТЕРИАЛОВ ======================
@Composable
fun RenderReceiveList(
    items: List<WmsReceiveItem>,
    onEditClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleQuality: (Int) -> Unit,
    onExpiDateClick: (Int, String) -> Unit // Колбэк для клика по дате
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
                key = { index, item -> "${item.matNumScan}_${item.matQtyScan}_$index" }
            ) { index, item ->
                Card(
                    Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        // Верхняя строка: заказ, материал, кнопки
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Заказ: ${item.matNumOrder}", style = MaterialTheme.typography.bodyLarge)
                                Text("Материал: ${item.matNumScan}", style = MaterialTheme.typography.bodyMedium)
                                Text("Кол-во: ${item.matQtyScan}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { onEditClick(index) }) {
                                Icon(Icons.Default.Edit, "Редактировать", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onRemoveItem(index) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        // Нижняя строка: чекбоксы + дата
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Чекбокс качества
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(checked = item.checkQuality, onCheckedChange = { onToggleQuality(index) })
                                Text("Качество", style = MaterialTheme.typography.bodySmall)
                            }

                            // Поле даты срока годности с иконкой календаря (кликабельное)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onExpiDateClick(index, item.Expi) }
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Выбрать дату",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (item.Expi.isNotBlank()) item.Expi else "Выбрать дату",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.Expi.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================== 4. ДИАЛОГ РЕДАКТИРОВАНИЯ ======================
@Composable
fun EditReceiveItemDialog(
    material: String,
    qty: String,
    orderNumber: String,
    quality: Boolean,
    expi: String,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, String) -> Unit,
    onDateClick: () -> Unit // Колбэк для открытия DatePicker
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var materialField by remember { mutableStateOf(TextFieldValue(material, TextRange(material.length))) }
    var qtyField by remember { mutableStateOf(TextFieldValue(qty, TextRange(qty.length))) }
    var orderField by remember { mutableStateOf(TextFieldValue(orderNumber, TextRange(orderNumber.length))) }
    var qualityChecked by remember { mutableStateOf(quality) }
    var expiField by remember { mutableStateOf(expi) }

    LaunchedEffect(material) { materialField = TextFieldValue(material, TextRange(material.length)) }
    LaunchedEffect(qty) { qtyField = TextFieldValue(qty, TextRange(qty.length)) }
    LaunchedEffect(orderNumber) { orderField = TextFieldValue(orderNumber, TextRange(orderNumber.length)) }
    LaunchedEffect(expi) { expiField = expi }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Редактирование" else "Новый материал", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier
                .verticalScroll(scrollState)
                .padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = materialField,
                    onValueChange = { materialField = it.copy(selection = TextRange(it.text.length)) },
                    label = { Text("Артикул") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qtyField,
                        onValueChange = { if (it.text.all { it.isDigit() } && it.text.length <= 5) qtyField = it.copy(selection = TextRange(it.text.length)) },
                        label = { Text("Кол-во") },
                        modifier = Modifier.weight(0.4f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = orderField,
                        onValueChange = { orderField = it.copy(selection = TextRange(it.text.length)) },
                        label = { Text("Заказ") },
                        modifier = Modifier
                            .weight(0.6f)
                            .clickable {
                                clipboard.setText(AnnotatedString(orderField.text))
                                Toast.makeText(
                                    context,
                                    "Номер заказа скопирован",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        singleLine = true,
                        readOnly = isEditing,
                        enabled = true
                    )
                }
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), shape = MaterialTheme.shapes.small) {
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = qualityChecked, onCheckedChange = { qualityChecked = it }, modifier = Modifier.size(20.dp))
                            Text("Качество", style = MaterialTheme.typography.bodySmall)
                        }
                        // Поле выбора даты с иконкой календаря
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Выбрать дату",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (expiField.isNotBlank()) expiField else "Выбрать",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (expiField.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { onDateClick() }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(materialField.text, qtyField.text, orderField.text, qualityChecked, expiField) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ====================== 5. ПРЕВЬЮ ======================
@Preview(showBackground = true, name = "WmsReceive - Empty")
@Composable
fun WmsReceivePreviewEmpty() {
    MaterialTheme {
        Surface {
            WmsReceiveScreenContent(
                receiveItems = emptyList(),
                orderNumber = "4200011646",
                onEditClick = {}, onRemoveItem = {}, onToggleQuality = {},
                onExpiDateClick = { _, _ -> },
                onAddManualClick = {}, uiState = MainViewModel.UiState.Idle,
                onReceiveClick = {}, onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "WmsReceive - With Items")
@Composable
fun WmsReceivePreviewItems() {
    MaterialTheme {
        Surface {
            WmsReceiveScreenContent(
                receiveItems = listOf(
                    WmsReceiveItem("LA0610402138", "4200011646", 5, true, "10.07.2026"),
                    WmsReceiveItem("LA0610501327", "4200011646", 3, false, "")
                ),
                orderNumber = "4200011646",
                onEditClick = {}, onRemoveItem = {}, onToggleQuality = {},
                onExpiDateClick = { _, _ -> },
                onAddManualClick = {}, uiState = MainViewModel.UiState.Idle,
                onReceiveClick = {}, onBackClick = {}
            )
        }
    }
}