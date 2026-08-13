package com.gps.warehouse.ui.gps_screens.warehouse

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.WmsReceiveItem
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.Constants.ROUTE_CAMERA_SCAN
import com.gps.warehouse.utils.HandleScanResult
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.decodeWmsReceiveScreen
import com.gps.warehouse.utils.isBase64EncodedJson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ====================== ЭКРАН ======================
@Composable
fun WmsReceiveScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    orderNumber: String = ""
) {
    val TAG = "WmsReceiveScreen"
    val scope = rememberCoroutineScope()
    var receiveItems by remember { mutableStateOf<List<WmsReceiveItem>>(emptyList()) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Подписываемся на настройку
    val cameraScanEnabled by viewModel.cameraScanEnabled.collectAsState()

    // Состояния диалога редактирования
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var dialogOrderNumber by remember { mutableStateOf(orderNumber) }
    var dialogMaterial by remember { mutableStateOf("") }
    var dialogQty by remember { mutableStateOf("1") }
    var dialogQuality by remember { mutableStateOf(true) }
    var dialogExpi by remember { mutableStateOf("") }
    var dialogPosition by remember { mutableStateOf("") }
    var isPositionReadOnly by remember { mutableStateOf(false) }
    var dialogMatName by remember { mutableStateOf("") }
    var dialogQtyOrder by remember { mutableStateOf("") }
    var isUpdatingName by remember { mutableStateOf(false) } // Индикатор загрузки имени

    // Состояния для DatePicker в СПИСКЕ
    var showListDatePicker by remember { mutableStateOf(false) }
    var listDatePickerIndex by remember { mutableStateOf<Int?>(null) }
    var listDatePickerInitialDate by remember { mutableStateOf<Long?>(null) }

    // Состояния для DatePicker в ДИАЛОГЕ
    var showEditDatePicker by remember { mutableStateOf(false) }
    var editDatePickerInitialDate by remember { mutableStateOf<Long?>(null) }

    // Для диалога подтверждения удаления
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }

    val honeywellHelper = remember { ScannerManager(context) }

    // Слушаем сканер
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val trimmedData = scannedData.trim()
                if (isBase64EncodedJson(trimmedData)) {
                    val scanResult = decodeWmsReceiveScreen(trimmedData)
                    Log.d(TAG, "scanResult = $scanResult")
                    if (scanResult != null) {
                        if (showDialog) {
                            // Если диалог открыт — заполняем ТОЛЬКО артикул
                            dialogMaterial = scanResult.matNumScan
                        } else {
                            // Сначала создаём элемент ВРЕМЕННО без имени
                            val orderNumberToUse =
                                if (scanResult.matNumOrder == orderNumber || orderNumber.isEmpty()) {
                                    scanResult.matNumOrder
                                } else {
                                    orderNumber
                                }

                            val tempItem = WmsReceiveItem(
                                matNumScan = scanResult.matNumScan,
                                matNumOrder = orderNumberToUse,
                                matQtyOrder = scanResult.matQtyScan,
                                checkQuality = true,
                                Expi = "",
                                matPositionSap = scanResult.matPosition,
                                isPositionFromScan = true,
                                matName = "", // Временно пусто,
                                qtyOrder = scanResult.matQtyScan
                            )

                            // Сразу добавляем элемент в список (чтобы он отобразился)
                            receiveItems = receiveItems + tempItem

                            // Асинхронно запрашиваем имя и обновляем элемент в списке
                            scope.launch {
                                val nameMaterial = viewModel.getNameMaterial(scanResult.matNumScan)
                                Log.d(TAG, "MATERIAL NAME: $nameMaterial")

                                // Находим индекс элемента по артикулу и обновляем с именем
                                val index = receiveItems.indexOfFirst {
                                    it.matNumScan == scanResult.matNumScan && it.matName.isEmpty()
                                }
                                if (index != -1) {
                                    receiveItems = receiveItems.toMutableList().apply {
                                        set(index, get(index).copy(matName = nameMaterial))
                                    }
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Ошибка распознавания данных скана!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // Ловим результат сканирования
    HandleScanResult(navController) { scannedCode ->
        val materialCode = scannedCode // Автоматически вставляем в поле
        // Можно сразу отправить запрос на сервер, если нужно
    }

    // Единая кнопка. Если cameraScanEnabled = false, она НЕ отрисуется
    CameraScanButton(
        cameraScanEnabled = cameraScanEnabled,
        onClick = { navController.navigate(ROUTE_CAMERA_SCAN) },
        modifier = Modifier.fillMaxWidth()
    )

    // Инициализация сканера
    DisposableEffect(Unit) {
        honeywellHelper.init()
        onDispose {
            honeywellHelper.release()
            viewModel.resetReceiveState()
        }
    }

    // === Диалог редактирования ===
    if (showDialog) {
        EditReceiveItemDialog(
            material = dialogMaterial,
            qty = dialogQty,
            qtyOrder = dialogQtyOrder,
            matName = dialogMatName,
            isUpdatingName = isUpdatingName,
            orderNumber = dialogOrderNumber,
            quality = dialogQuality,
            expi = dialogExpi,
            position = dialogPosition,
            isEditing = editingIndex != null,
            isPositionReadOnly = isPositionReadOnly,
            onDismiss = { showDialog = false },
            onConfirm = { mat, qty, qtyOrd, name, ord, qual, exp, pos ->
                val qtyInt = qty.toIntOrNull() ?: 1
                val qtyOrderInt = qtyOrd.toIntOrNull() ?: qtyInt
                val finalOrder = ord.ifBlank { orderNumber }

                if (qtyInt >= 0 && mat.isNotBlank() && finalOrder.isNotBlank()) {
                    // логика внутри scope.launch для ожидания результата
                    scope.launch {
                        // 1. Получаем имя: если пусто — запрашиваем, иначе используем существующее
                        val finalName = if (name.isBlank()) {
                            viewModel.getNameMaterial(mat)
                        } else {
                            name
                        }

                        Log.d(TAG, "Final name for $mat: $finalName")

                        // 2. Создаём элемент с актуальным именем
                        val newItem = WmsReceiveItem(
                            matNumScan = mat,
                            matNumOrder = finalOrder,
                            matQtyOrder = qtyInt,
                            checkQuality = qual,
                            Expi = exp,
                            matPositionSap = pos,
                            isPositionFromScan = isPositionReadOnly,
                            matName = finalName, // ← Актуальное имя
                            qtyOrder = qtyOrderInt
                        )

                        // 3. Обновляем список
                        receiveItems = if (editingIndex != null) {
                            receiveItems.toMutableList().apply { set(editingIndex!!, newItem) }
                        } else {
                            receiveItems + newItem
                        }

                        // 4. Закрываем диалог
                        showDialog = false
                    }
                }
            },
            onDateClick = {
                editDatePickerInitialDate = parseDate(dialogExpi)?.time
                showEditDatePicker = true
            },
            // Колбэк для обновления имени материала
            onUpdateMaterialName = { materialCode ->
                scope.launch {
                    isUpdatingName = true
                    val name = viewModel.getNameMaterial(materialCode)
                    dialogMatName = name
                    isUpdatingName = false
                }
            }
        )
    }

    // Диалог подтверждения удаления
    if (deleteConfirmIndex != null) {
        val itemToDelete = receiveItems.getOrNull(deleteConfirmIndex!!)
        if (itemToDelete != null) {
            DeleteConfirmationDialog(
                materialName = itemToDelete.matName,
                materialNumber = itemToDelete.matNumScan,
                onConfirm = {
                    receiveItems = receiveItems.toMutableList().apply {
                        removeAt(deleteConfirmIndex!!)
                    }
                    deleteConfirmIndex = null
                },
                onDismiss = { deleteConfirmIndex = null }
            )
        }
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Заказ завершен!") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
            dialogQty = item.matQtyOrder.toString()
            dialogQtyOrder = item.qtyOrder.toString()
            dialogMatName = item.matName
            dialogOrderNumber = item.matNumOrder
            dialogQuality = item.checkQuality
            dialogExpi = item.Expi
            dialogPosition = item.matPositionSap
            isPositionReadOnly = item.isPositionFromScan
            editingIndex = index
            showDialog = true
        },
        onRequestDelete = { index ->
            deleteConfirmIndex = index
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
            dialogQtyOrder = "1"
            dialogMatName = ""
            dialogOrderNumber = orderNumber.ifEmpty { "" }
            dialogQuality = true
            dialogExpi = ""
            dialogPosition = ""
            isPositionReadOnly = false
            editingIndex = null
            showDialog = true
        },
        uiState = uiState,
        onReceiveClick = {
            if (receiveItems.isNotEmpty()) {
                // === Автоматически проставляем дату для элементов без Expi ===
                val itemsWithDefaultDate = receiveItems.map { item ->
                    if (item.Expi.isBlank()) {
                        item.copy(Expi = "01.01.2222") // Дата по умолчанию
                    } else {
                        item
                    }
                }
                // Отправляем на сервер список с заполненными датами
                viewModel.receiveWmsMaterials(itemsWithDefaultDate)
            } else {
                Toast.makeText(context, "Добавьте хотя бы один материал", Toast.LENGTH_SHORT).show()
            }
        },
        onRetryClick = { viewModel.loadWmsData() },
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

// ====================== ЧИСТЫЙ UI КОМПОНЕНТ ======================
@Composable
fun WmsReceiveScreenContent(
    receiveItems: List<WmsReceiveItem>,
    orderNumber: String,
    onEditClick: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
    onToggleQuality: (Int) -> Unit,
    onExpiDateClick: (Int, String) -> Unit,
    onAddManualClick: () -> Unit,
    uiState: MainViewModel.UiState,
    onReceiveClick: () -> Unit,
    onRetryClick: () -> Unit,
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
            Column(modifier = Modifier.padding(8.dp)) {

                // === ЛОГИКА ДЛЯ КНОПКИ ===
                val isReadyToComplete = receiveItems.isNotEmpty() &&
                        receiveItems.all { it.matPositionSap.isNotBlank() }

                Button(
                    onClick = onReceiveClick,
                    enabled = isReadyToComplete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Завершить приемку (${receiveItems.sumOf { it.matQtyOrder }} шт.)")
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
                RenderReceiveList(
                    items = receiveItems,
                    onEditClick = onEditClick,
                    onRequestDelete = onRequestDelete,
                    onToggleQuality = onToggleQuality,
                    onExpiDateClick = onExpiDateClick
                )
            }
        }
    }
}

// ====================== СПИСОК МАТЕРИАЛОВ ======================
@Composable
fun RenderReceiveList(
    items: List<WmsReceiveItem>,
    onEditClick: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
    onToggleQuality: (Int) -> Unit,
    onExpiDateClick: (Int, String) -> Unit
) {
    val lazyListState = rememberLazyListState()
    // Сохраняем позицию при изменении количества элементов
    var previousSize by remember { mutableIntStateOf(items.size) }

    LaunchedEffect(items.size) {
        // Скроллим до последнего элемента
        // Т.к. список перевернут, будем всегда видеть отсканированный элемент вверху списка
        previousSize = items.size
        lazyListState.scrollToItem(previousSize)
    }

    if (items.isEmpty()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Список пуст.\nОтсканируйте QR или добавьте вручную.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true,
            state = lazyListState
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.matNumScan} ${item.matQtyOrder} $index" },
            ) { index, item ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onEditClick(index) }),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    // Box позволяет позиционировать кнопки по углам
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // === ЛЕВАЯ ЧАСТЬ: Информация о материале ===
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f) // % ширины для контента
                                .padding(12.dp)         // Отступ для красоты текста
                        ) {
                            Text(
                                "Заказ: ${item.matNumOrder}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Материал: ${item.matNumScan}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (item.matName.isNotBlank()) {
                                Text(
                                    "Название: ${item.matName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "Кол-во: ${item.matQtyOrder}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Кол-во в накладной: ${item.qtyOrder}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!item.matPositionSap.isNullOrBlank()) {
                                Text(
                                    "Позиция SAP: ${item.matPositionSap}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            // Нижняя строка: качество + дата
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = item.checkQuality,
                                        onCheckedChange = { onToggleQuality(index) }
                                    )
                                    Text("Качество", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onExpiDateClick(index, item.Expi) }
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Срок годности!",
                                        tint = if (item.Expi.isNotBlank())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (item.Expi.isNotBlank()) item.Expi else "Срок годности!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.Expi.isNotBlank())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                        fontWeight = if (item.Expi.isNotBlank()) FontWeight.Normal else FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // ПРАВАЯ ЧАСТЬ: Кнопки (вертикально: сверху редактировать, снизу удалить)
                        Column(
                            modifier = Modifier
                                .fillMaxHeight(0.7f) // Занимаем всю высоту для распределения кнопок
                                .align(Alignment.TopEnd), // Прижимаем к правому краю
                            verticalArrangement = Arrangement.SpaceBetween, // Кнопки по краям
                        ) {
                            // Кнопка удаления — СВЕРХУ справа
                            IconButton(
                                onClick = { onRequestDelete(index) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Удалить",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditReceiveItemDialog(
    material: String,
    qty: String,
    qtyOrder: String,
    matName: String,
    isUpdatingName: Boolean,
    orderNumber: String,
    quality: Boolean,
    expi: String,
    position: String,
    isEditing: Boolean,
    isPositionReadOnly: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Boolean, String, String) -> Unit,
    onDateClick: () -> Unit,
    onUpdateMaterialName: (String) -> Unit // Колбэк для обновления имени
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Состояния полей
    var materialField by remember { mutableStateOf(TextFieldValue(material, TextRange(material.length))) }
    var qtyField by remember { mutableStateOf(TextFieldValue(qty, TextRange(qty.length))) }
    var qtyOrderField by remember { mutableStateOf(TextFieldValue(qtyOrder, TextRange(qtyOrder.length))) }
    var matNameField by remember { mutableStateOf(TextFieldValue(matName, TextRange(matName.length))) }
    var orderField by remember { mutableStateOf(TextFieldValue(orderNumber, TextRange(orderNumber.length))) }
    var qualityChecked by remember { mutableStateOf(quality) }
    var expiField by remember { mutableStateOf(expi) }
    var positionField by remember { mutableStateOf(TextFieldValue(position, TextRange(position.length))) }

    // Обновление полей при изменении параметров
    LaunchedEffect(material) { materialField = TextFieldValue(material, TextRange(material.length)) }
    LaunchedEffect(qty) {
        qtyField = TextFieldValue(qty, TextRange(qty.length))
        // Если qtyOrder пустой или равен старому qty — обновляем его
        if (qtyOrder.isEmpty() || qtyOrder == qty) {
            qtyOrderField = TextFieldValue(qty, TextRange(qty.length))
        }
    }
    LaunchedEffect(qtyOrder) { qtyOrderField = TextFieldValue(qtyOrder, TextRange(qtyOrder.length)) }
    LaunchedEffect(matName) { matNameField = TextFieldValue(matName, TextRange(matName.length)) }
    LaunchedEffect(orderNumber) { orderField = TextFieldValue(orderNumber, TextRange(orderNumber.length)) }
    LaunchedEffect(expi) { expiField = expi }
    LaunchedEffect(position) { positionField = TextFieldValue(position, TextRange(position.length)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.99f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Редактирование" else "Новый материал",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Артикул
                OutlinedTextField(
                    value = materialField,
                    onValueChange = {
                        materialField = it.copy(selection = TextRange(it.text.length))
                    },
                    label = {
                        Text(
                            "Артикул", style = MaterialTheme.typography.bodySmall,
                            color = if (materialField.text.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            clipboard.setText(AnnotatedString(materialField.text))
                            Toast.makeText(context, "Артикул скопирован", Toast.LENGTH_SHORT).show()
                        },
                    singleLine = true,
                    readOnly = isEditing,
                    enabled = !isEditing,
                    textStyle = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = matNameField,
                    onValueChange = { matNameField = it.copy(selection = TextRange(it.text.length)) },
                    label = { Text("Название", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .clickable {
                            clipboard.setText(AnnotatedString(matNameField.text))
                            Toast.makeText(context, "Название скопировано", Toast.LENGTH_SHORT)
                                .show()
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    enabled = false
                )

                // Заказ
                OutlinedTextField(
                    value = orderField,
                    onValueChange = { orderField = it.copy(selection = TextRange(it.text.length)) },
                    label = {
                        Text(
                            "Заказ", style = MaterialTheme.typography.bodySmall,
                            color = if (orderField.text.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            clipboard.setText(AnnotatedString(orderField.text))
                            Toast.makeText(context, "Номер заказа скопирован", Toast.LENGTH_SHORT).show()
                        },
                    singleLine = true,
                    readOnly = isEditing,
                    enabled = !isEditing,
                    textStyle = MaterialTheme.typography.titleMedium
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),

                ) {
                    // Количество (сканирование)
                    OutlinedTextField(
                        value = qtyField,
                        onValueChange = {
                            if (it.text.all { it.isDigit() } && it.text.length <= 5) {
                                qtyField = it.copy(selection = TextRange(it.text.length))
                                // Если qtyOrder не редактировался пользователем — синхронизируем
                                // (опционально, можно убрать если нужна полная независимость)
                            }
                        },
//                        label = { Text("Кол-во", style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        supportingText = { Text("Кол-во факт") }
                    )
                    OutlinedTextField(
                        value = qtyOrderField,
                        onValueChange = {
                            if (it.text.all { it.isDigit() } && it.text.length <= 5) {
                                qtyOrderField = it.copy(selection = TextRange(it.text.length))
                            }
                        },
//                        label = { Text("В накладной", style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        supportingText = { Text("Кол-во накладная") },
                    )
                }

                // Позиция SAP
                OutlinedTextField(
                    value = positionField,
                    onValueChange = {
                        if (it.text.all { it.isDigit() } && it.text.length <= 5) positionField =
                            it.copy(selection = TextRange(it.text.length))
                    },
                    label = {
                        Text(
                            "Позиция SAP",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (positionField.text.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    readOnly = isPositionReadOnly,
                    enabled = !isPositionReadOnly,
                    textStyle = MaterialTheme.typography.titleMedium
                )

                // Блок качества и даты
                Surface(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = qualityChecked,
                                onCheckedChange = { qualityChecked = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Качество", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Срок годности",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Срок годности:\n${expiField.ifBlank { "Не выбрано" }}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (expiField.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { onDateClick() }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    materialField.text,
                    qtyField.text,
                    qtyOrderField.text,
                    matNameField.text,
                    orderField.text,
                    qualityChecked,
                    expiField,
                    positionField.text
                )
            }) {
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


// ====================== ДИАЛОГ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ ======================
@Composable
fun DeleteConfirmationDialog(
    materialName: String,
    materialNumber: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                "Подтвердите удаление",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Вы действительно хотите удалить из списка:\n \"$materialName\"\n($materialNumber) ?\nЭто действие нельзя отменить.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить", style = MaterialTheme.typography.titleSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", style = MaterialTheme.typography.titleSmall)
            }
        },
        // Блокируем закрытие по клику вне диалога
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
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
                onEditClick = {}, onRequestDelete = {}, onToggleQuality = {},
                onExpiDateClick = { _, _ -> },
                onAddManualClick = {}, uiState = MainViewModel.UiState.Idle,
                onReceiveClick = {},
                onRetryClick = {},
                onBackClick = {}
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
                    WmsReceiveItem(
                        "LA0610402138",
                        "4200011646",
                        5,
                        true,
                        "10.07.2026",
                        "3051",
                        isPositionFromScan = true,
                        matName = "Название 1",
                        qtyOrder = 5
                    ),
                    WmsReceiveItem(
                        "LA0610501327",
                        "4200011646",
                        3,
                        false,
                        "",
                        "",
                        isPositionFromScan = false,
                        matName = "Название 2",
                        qtyOrder = 3
                    )
                ),
                orderNumber = "4200011646",
                onEditClick = {}, onRequestDelete = {}, onToggleQuality = {},
                onExpiDateClick = { _, _ -> },
                onAddManualClick = {}, uiState = MainViewModel.UiState.Idle,
                onReceiveClick = {},
                onRetryClick = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Edit Dialog - New Item (позиция редактируемая)")
@Composable
fun EditReceiveItemDialogPreview_New() {
    MaterialTheme {
        Surface {
            EditReceiveItemDialog(
                material = "",
                qty = "5",
                qtyOrder = "3",
                matName = "",
                isUpdatingName = false,
                orderNumber = "",
                quality = true,
                expi = "",
                position = "",
                isEditing = false,
                isPositionReadOnly = false,
                onDismiss = {},
                onConfirm = { _, _, _, _, _, _, _, _ -> },
                onDateClick = {},
                onUpdateMaterialName = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Edit Dialog - Editing Item (позиция readOnly)")
@Composable
fun EditReceiveItemDialogPreview_Editing() {
    MaterialTheme {
        Surface {
            EditReceiveItemDialog(
                material = "LA0610402138",
                qty = "5",
                qtyOrder = "3",
                matName = "Сигнализационная лампа",
                isUpdatingName = false,
                orderNumber = "4200011646",
                quality = true,
                expi = "10.07.2026",
                position = "3051",
                isEditing = true,
                isPositionReadOnly = true,
                onDismiss = {},
                onConfirm = { _, _, _, _, _, _, _, _ -> },
                onDateClick = {},
                onUpdateMaterialName = {}
            )
        }
    }
}

@Preview()
@Composable
fun DeleteConfirmationDialogPreview() {
    MaterialTheme {
        Surface {
            DeleteConfirmationDialog(
                materialName = "Имя",
                materialNumber = "Номер42325",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}