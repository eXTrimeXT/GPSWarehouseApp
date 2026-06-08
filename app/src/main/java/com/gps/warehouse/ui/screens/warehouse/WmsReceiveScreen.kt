package com.gps.warehouse.ui.screens.warehouse

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
import com.gps.warehouse.data.remote.dto.WmsReceiveItem
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.decodeWmsScanData
import com.gps.warehouse.utils.isBase64EncodedJson

// 1. Реальный экран
@Composable
fun WmsReceiveScreen(
    navController: NavHostController,
    viewModel: MainViewModel,
    orderNumber: String = "" // Опциональный, если скан содержит order
) {
    var receiveItems by remember { mutableStateOf<List<WmsReceiveItem>>(emptyList()) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Состояние для диалога редактирования/добавления
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var dialogOrderNumber by remember { mutableStateOf(orderNumber) }

    // Поля диалога
    var dialogMaterial by remember { mutableStateOf("") }
    var dialogQty by remember { mutableStateOf("1") }
    var dialogQuality by remember { mutableStateOf(true) }
    var dialogExpi by remember { mutableStateOf(true) }

    // Хелпер для сканера Honeywell
    val honeywellHelper = remember { ScannerManager(context) }

    // Слушаем поток сканера
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val trimmedData = scannedData.trim()

                // Проверяем, является ли данные Base64-JSON
                if (isBase64EncodedJson(trimmedData)) {
                    // Декодируем новый формат
                    val scanResult = decodeWmsScanData(trimmedData)

                    if (scanResult != null) {
                        if (showDialog) {
                            // Если диалог открыт — заполняем поле артикула
                            dialogMaterial = scanResult.matNumScan
                        } else {
                            // Создаем элемент с данными из скана
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
                                checkExpi = true
                            )
                            receiveItems = receiveItems + newItem
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Ошибка распознавания данных скана!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Старый формат: используем BarcodeParser
//                    val parsedData: ScannedData? = BarcodeParser.parse(trimmedData)
//                    if (parsedData != null) {
//                        if (showDialog) {
//                            dialogMaterial = parsedData.material
//                        } else {
//                            val newItem = WmsReceiveItem(
//                                matNumScan = parsedData.material,
//                                matNumOrder = orderNumber,
//                                matQtyScan = parsedData.qty,
//                                checkQuality = true,
//                                checkExpi = true
//                            )
//                            receiveItems = receiveItems + newItem
//                        }
//                    } else {
                        Toast.makeText(context, "Неизвестный формат QR кода!", Toast.LENGTH_SHORT).show()
//                    }
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

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ/ДОБАВЛЕНИЯ ===
    if (showDialog) {
        EditReceiveItemDialog(
            material = dialogMaterial,
            qty = dialogQty,
            orderNumber = dialogOrderNumber, // Передаем номер заказа в диалог
            quality = dialogQuality,
            expi = dialogExpi,
            isEditing = editingIndex != null,
            onDismiss = { showDialog = false },
            onConfirm = { mat, qty, ord, qual, exp ->
                val qtyInt = qty.toIntOrNull() ?: 1
                // Если в диалоге заказ не указан, берем номер текущего экрана
                val finalOrder = if (ord.isNotBlank()) ord else orderNumber

                if (qtyInt > 0 && mat.isNotBlank() && finalOrder.isNotBlank()) {
                    val newItem = WmsReceiveItem(
                        matNumScan = mat,
                        matNumOrder = finalOrder, // Применяем итоговый номер
                        matQtyScan = qtyInt,
                        checkQuality = qual,
                        checkExpi = exp
                    )
                    receiveItems = if (editingIndex != null) {
                        receiveItems.toMutableList().apply {
                            set(editingIndex!!, newItem)
                        }
                    } else {
                        receiveItems + newItem
                    }
                    showDialog = false
                }
            }
        )
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
            dialogOrderNumber = item.matNumOrder // Заполняем при редактировании
            dialogQuality = item.checkQuality
            dialogExpi = item.checkExpi
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
        onToggleExpi = { index ->
            receiveItems = receiveItems.toMutableList().apply {
                set(index, get(index).copy(checkExpi = !get(index).checkExpi))
            }
        },
        onAddManualClick = {
            dialogMaterial = ""
            dialogQty = "1"
            dialogOrderNumber = orderNumber.ifEmpty { "" } // Автозаполнение при добавлении
            dialogQuality = true
            dialogExpi = true
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

// 2. Чистый UI компонент
@Composable
fun WmsReceiveScreenContent(
    receiveItems: List<WmsReceiveItem>,
    orderNumber: String,
    onEditClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleQuality: (Int) -> Unit,
    onToggleExpi: (Int) -> Unit,
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

        // Кнопка "Завершить приемку" (видна только если не Loading/Error/Success)
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

        // Основной контент
        when (uiState) {
            is MainViewModel.UiState.Loading -> {
                CustomLoadingView()
            }
            is MainViewModel.UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = null,
                    modifier = Modifier.weight(1f)
                )
            }
            is MainViewModel.UiState.WmsReceiveSuccess -> {
                RenderReceiveList(
                    items = receiveItems,
                    onEditClick = onEditClick,
                    onRemoveItem = onRemoveItem,
                    onToggleQuality = onToggleQuality,
                    onToggleExpi = onToggleExpi
                )
            }
            else -> {
                RenderReceiveList(
                    items = receiveItems,
                    onEditClick = onEditClick,
                    onRemoveItem = onRemoveItem,
                    onToggleQuality = onToggleQuality,
                    onToggleExpi = onToggleExpi
                )
            }
        }
    }
}

// 📦 Список материалов (ИСПРАВЛЕНО: itemsIndexed вместо indexOfFirst)
@Composable
fun RenderReceiveList(
    items: List<WmsReceiveItem>,
    onEditClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onToggleQuality: (Int) -> Unit,
    onToggleExpi: (Int) -> Unit
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(checked = item.checkQuality, onCheckedChange = { onToggleQuality(index) })
                                Text("Качество", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(checked = item.checkExpi, onCheckedChange = { onToggleExpi(index) })
                                Text("Срок годности", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Диалог ввода (ИСПРАВЛЕНО: 5 параметров + копирование заказа)
@Composable
fun EditReceiveItemDialog(
    material: String,
    qty: String,
    orderNumber: String,
    quality: Boolean,
    expi: Boolean,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (material: String, qty: String, order: String, quality: Boolean, expi: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var materialField by remember { mutableStateOf(TextFieldValue(material, TextRange(material.length))) }
    var qtyField by remember { mutableStateOf(TextFieldValue(qty, TextRange(qty.length))) }
    var orderField by remember { mutableStateOf(TextFieldValue(orderNumber, TextRange(orderNumber.length))) }
    var qualityChecked by remember { mutableStateOf(quality) }
    var expiChecked by remember { mutableStateOf(expi) }

    LaunchedEffect(material) { materialField = TextFieldValue(material, TextRange(material.length)) }
    LaunchedEffect(qty) { qtyField = TextFieldValue(qty, TextRange(qty.length)) }
    LaunchedEffect(orderNumber) { orderField = TextFieldValue(orderNumber, TextRange(orderNumber.length)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Редактирование" else "Новый материал", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(scrollState).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        onValueChange = { if (it.text.all { c -> c.isDigit() } && it.text.length <= 5) qtyField = it.copy(selection = TextRange(it.text.length)) },
                        label = { Text("Кол-во") },
                        modifier = Modifier.weight(0.4f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = orderField,
                        onValueChange = { orderField = it.copy(selection = TextRange(it.text.length)) },
                        label = { Text("Заказ") },
                        modifier = Modifier.weight(0.6f).clickable {
                            clipboard.setText(AnnotatedString(orderField.text))
                            Toast.makeText(context, "Номер заказа скопирован", Toast.LENGTH_SHORT).show()
                        },
                        singleLine = true,
                        readOnly = isEditing,
                        enabled = true // Важно для работы clickable
                    )
                }
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), shape = MaterialTheme.shapes.small) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = qualityChecked, onCheckedChange = { qualityChecked = it }, modifier = Modifier.size(20.dp))
                            Text("Качество", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = expiChecked, onCheckedChange = { expiChecked = it }, modifier = Modifier.size(20.dp))
                            Text("Срок", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(materialField.text, qtyField.text, orderField.text, qualityChecked, expiChecked) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

//@Composable
//fun RenderReceiveList(
//    items: List<WmsReceiveItem>,
//    onEditClick: (Int) -> Unit,
//    onRemoveItem: (Int) -> Unit,
//    onToggleQuality: (Int) -> Unit,
//    onToggleExpi: (Int) -> Unit
//) {
//    if (items.isEmpty()) {
//        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                Text(
//                    text = "Список пуст.\nОтсканируйте QR или добавьте вручную.",
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    } else {
//        LazyColumn(
//            modifier = Modifier.fillMaxWidth(),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//            contentPadding = PaddingValues(16.dp)
//        ) {
//            items(items) { item ->
//                val index = items.indexOfFirst { it.matNumScan == item.matNumScan && it.matQtyScan == item.matQtyScan }
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Column(modifier = Modifier.padding(12.dp)) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text(item.matNumScan, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
//                                Text("Количество факт: ${item.matQtyScan}", style = MaterialTheme.typography.bodyMedium)
//                            }
//                            IconButton(onClick = { onEditClick(index) }) {
//                                Icon(Icons.Default.Edit, "Редактировать", tint = MaterialTheme.colorScheme.primary)
//                            }
//                            IconButton(onClick = { onRemoveItem(index) }) {
//                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
//                            }
//                        }
//                        Spacer(Modifier.height(8.dp))
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
//                                Checkbox(checked = item.checkQuality, onCheckedChange = { onToggleQuality(index) })
//                                Text("Качество", style = MaterialTheme.typography.bodySmall)
//                            }
//                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
//                                Checkbox(checked = item.checkExpi, onCheckedChange = { onToggleExpi(index) })
//                                Text("Срок годности", style = MaterialTheme.typography.bodySmall)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//@Composable
//fun EditReceiveItemDialog(
//    material: String,
//    qty: String,
//    orderNumber: String,
//    quality: Boolean,
//    expi: Boolean,
//    isEditing: Boolean,
//    onDismiss: () -> Unit,
//    onConfirm: (String, String, String, Boolean, Boolean) -> Unit
//) {
//    val context = LocalContext.current
//    val clipboardManager = LocalClipboardManager.current
//    val scrollState = rememberScrollState()
//
//    var materialFieldValue by remember {
//        mutableStateOf(TextFieldValue(text = material, selection = TextRange(material.length)))
//    }
//    var qtyFieldValue by remember {
//        mutableStateOf(TextFieldValue(text = qty, selection = TextRange(qty.length)))
//    }
//    var orderFieldValue by remember {
//        mutableStateOf(TextFieldValue(text = orderNumber, selection = TextRange(orderNumber.length)))
//    }
//    var qualityChecked by remember { mutableStateOf(quality) }
//    var expiChecked by remember { mutableStateOf(expi) }
//
//    LaunchedEffect(material) {
//        materialFieldValue = TextFieldValue(text = material, selection = TextRange(material.length))
//    }
//    LaunchedEffect(qty) {
//        qtyFieldValue = TextFieldValue(text = qty, selection = TextRange(qty.length))
//    }
//    LaunchedEffect(orderNumber) {
//        orderFieldValue = TextFieldValue(text = orderNumber, selection = TextRange(orderNumber.length))
//    }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = {
//            Text(
//                if (isEditing) "Редактирование" else "Новый материал",
//                fontWeight = FontWeight.SemiBold
//            )
//        },
//        text = {
//            Column(
//                modifier = Modifier
//                    .verticalScroll(scrollState)
//                    .padding(top = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                // Артикул
//                OutlinedTextField(
//                    value = materialFieldValue,
//                    onValueChange = { newValue ->
//                        materialFieldValue = newValue.copy(selection = TextRange(newValue.text.length))
//                    },
//                    label = { Text("Артикул") },
//                    modifier = Modifier.fillMaxWidth(),
//                    singleLine = true,
//                    textStyle = MaterialTheme.typography.bodyMedium,
//                    maxLines = 1
//                )
//
//                // Количество и Заказ в одну строку (компактно)
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    OutlinedTextField(
//                        value = qtyFieldValue,
//                        onValueChange = { newValue ->
//                            if (newValue.text.all { it.isDigit() } && newValue.text.length <= 5) {
//                                qtyFieldValue = newValue.copy(selection = TextRange(newValue.text.length))
//                            }
//                        },
//                        label = { Text("Кол-во") },
//                        modifier = Modifier.weight(0.4f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        singleLine = true,
//                        textStyle = MaterialTheme.typography.bodyMedium
//                    )
//
//                    // Поле номера заказа: ReadOnly + Clickable для копирования
//                    OutlinedTextField(
//                        value = orderFieldValue,
//                        onValueChange = { newValue ->
//                            if (newValue.text.all { it.isLetterOrDigit() || it == '-' || it == '_' } && newValue.text.length <= 20) {
//                                orderFieldValue = newValue.copy(selection = TextRange(newValue.text.length))
//                            }
//                        },
//                        label = { Text("Заказ") },
//                        modifier = Modifier
//                            .weight(0.6f)
//                            .clickable {
//                                clipboardManager.setText(AnnotatedString(orderFieldValue.text))
//                                Toast.makeText(context, "Номер заказа скопирован", Toast.LENGTH_SHORT).show()
//                            },
//                        singleLine = true,
//                        readOnly = isEditing,
//                        enabled = !isEditing,
//                        textStyle = MaterialTheme.typography.bodyMedium,
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
//                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    )
//                }
//
//                // Чекбоксы в одну компактную строку
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
//                    shape = MaterialTheme.shapes.small
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 12.dp, vertical = 8.dp),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Row(
//                            modifier = Modifier.weight(1f),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Checkbox(
//                                checked = qualityChecked,
//                                onCheckedChange = { qualityChecked = it },
//                                modifier = Modifier.size(20.dp)
//                            )
//                            Spacer(modifier = Modifier.padding(5.dp))
//                            Text("Качество", style = MaterialTheme.typography.bodySmall)
//                        }
//                        Row(
//                            modifier = Modifier.weight(1f),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Checkbox(
//                                checked = expiChecked,
//                                onCheckedChange = { expiChecked = it },
//                                modifier = Modifier.size(20.dp)
//                            )
//                            Spacer(modifier = Modifier.padding(5.dp))
//                            Text("Срок", style = MaterialTheme.typography.bodySmall)
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            Button(
//                onClick = {
//                    onConfirm(
//                        materialFieldValue.text,
//                        qtyFieldValue.text,
//                        orderFieldValue.text,
//                        qualityChecked,
//                        expiChecked
//                    )
//                },
//                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
//            ) {
//                Text("Сохранить")
//            }
//        },
//        dismissButton = {
//            TextButton(
//                onClick = onDismiss,
//                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
//            ) {
//                Text("Отмена")
//            }
//        }
//    )
//}



// --- ПРЕВЬЮ ---
@Preview(showBackground = true, name = "WmsReceive - Empty")
@Composable
fun WmsReceivePreviewEmpty() {
    MaterialTheme {
        Surface {
            WmsReceiveScreenContent(
                receiveItems = emptyList(),
                orderNumber = "4200011646",
                onEditClick = {}, onRemoveItem = {}, onToggleQuality = {}, onToggleExpi = {},
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
                    WmsReceiveItem("LA0610402138", "4200011646", 5, true, true),
                    WmsReceiveItem("LA0610501327", "4200011646", 3, false, true)
                ),
                orderNumber = "4200011646",
                onEditClick = {}, onRemoveItem = {}, onToggleQuality = {}, onToggleExpi = {},
                onAddManualClick = {}, uiState = MainViewModel.UiState.Idle,
                onReceiveClick = {}, onBackClick = {}
            )
        }
    }
}