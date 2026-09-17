//package com.gps.warehouse.ui.gps_screens.warehouse
//
//import android.annotation.SuppressLint
//import android.util.Log
//import android.widget.Toast
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.Assignment
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Clear
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material.icons.filled.ErrorOutline
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.SwapHoriz
//import androidx.compose.material.icons.filled.Tune
//import androidx.compose.material.icons.outlined.LocationOn
//import androidx.compose.material.icons.outlined.Warehouse
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalFocusManager
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
//import androidx.navigation.NavHostController
//import com.gps.warehouse.data.remote.gps_dto.TopologyDto
//import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
//import com.gps.warehouse.ui.MainViewModel
//import com.gps.warehouse.ui.components.ErrorStateView
//import com.gps.warehouse.ui.components.MyCustomActionBar
//import com.gps.warehouse.ui.components.SapBadge
//import com.gps.warehouse.utils.ScannerManager
//import kotlin.collections.forEach
//
//data class WmsEditState(
//    val material: String,
//    val position: String,
//    val positionId: String,
//    val qty: String,
//    val min: String,
//    val max: String
//) {
//    companion object {
//        fun fromItem(item: WmsItemDto) = WmsEditState(
//            material = item.material,
//            position = item.position,
//            positionId = item.positionId.toString(),
//            qty = item.qty.toInt().toString(),
//            min = item.min.toString(),
//            max = item.max.toString()
//        )
//    }
//}
//
//@Composable
//fun WmsItemDetailsScreen(
//    material: String,
//    storageId: String,
//    navController: NavHostController,
//    mainViewModel: MainViewModel
//) {
//    val uiState by mainViewModel.uiState.collectAsState()
//    val topologies by mainViewModel.topologies.collectAsState()
//
//    // Находим item по композитному ключу: material + storageId
//    val item: WmsItemDto? = remember(uiState, material, storageId) {
//        (uiState as? MainViewModel.UiState.WmsLoaded)?.items?.find {
//            it.material == material && it.storageId.toString() == storageId
//        }
//    }
//
//    // Локальные состояния для диалогов
//    var showMoveDialog by remember { mutableStateOf(false) }
//    var showEditDialog by remember { mutableStateOf(false) }
//    var moveQty by remember { mutableStateOf("1") }
//    var targetStorage by remember { mutableStateOf("") }
//    var showDialogSuccess by remember { mutableStateOf(false) }
//    var dialogError by remember { mutableStateOf<String?>(null) }
//
//    // Состояние для редактирования
//    var editState by remember { mutableStateOf<WmsEditState?>(null) }
//
//    // Сканер для топологии
//    val context = LocalContext.current
//    val honeywellHelper = remember { ScannerManager(context) }
//    val currentShowMoveDialog by rememberUpdatedState(showMoveDialog)
//    val currentTopologies by rememberUpdatedState(topologies)
//
//    // Загрузка топологий при открытии экрана
//    LaunchedEffect(item?.storageId) {
//        item?.let {
//            mainViewModel.loadTopologies(it.storageId.toString())
//        }
//    }
//
//    // Обработка сканирования: если открыт диалог перемещения — ищем топологию
//    LaunchedEffect(Unit) {
//        honeywellHelper.barcodeFlow.collect { scannedData ->
//            if (scannedData.isEmpty()) return@collect
//
//            if (currentShowMoveDialog) {
//                val topologyCode = scannedData.trim()
//                val matchedTopology = currentTopologies.find {
//                    it.positionScan.equals(topologyCode, ignoreCase = true)
//                }
//                if (matchedTopology != null) {
//                    targetStorage = matchedTopology.position
//                    dialogError = null
//                    Toast.makeText(
//                        context,
//                        "Позиция: ${matchedTopology.position}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
//                    Toast.makeText(
//                        context,
//                        "Позиция '$topologyCode' не найдена",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//    }
//
//    DisposableEffect(Unit) {
//        honeywellHelper.init()
//        onDispose { honeywellHelper.release() }
//    }
//
//    // Реакция на результат перемещения
//    LaunchedEffect(uiState) {
//        when (uiState) {
//            is MainViewModel.UiState.WmsMoveSuccess -> {
//                if (showMoveDialog) showDialogSuccess = true
//            }
//            is MainViewModel.UiState.Error -> {
//                if (showMoveDialog) dialogError = (uiState as MainViewModel.UiState.Error).message
//            }
//            else -> {}
//        }
//    }
//
//    // Если item не найден — ошибка
//    if (item == null) {
//        ErrorStateView(
//            message = "Материал не найден",
//            onRetry = { navController.popBackStack() },
//            modifier = Modifier.fillMaxSize()
//        )
//        return
//    }
//
//    // Инициализация editState при первом показе
//    LaunchedEffect(item) {
//        if (editState == null) {
//            editState = WmsEditState.fromItem(item)
//        }
//    }
//
//    WmsItemDetailsContent(
//        item = item,
//        onBackClick = { navController.popBackStack() },
//        onMoveClick = {
//            moveQty = ""
//            targetStorage = ""
//            dialogError = null
//            showDialogSuccess = false
//            showMoveDialog = true
//        },
//        onChangeTopologyClick = { showEditDialog = true }
//    )
//
//    // === ДИАЛОГ ПЕРЕМЕЩЕНИЯ ===
//    if (showMoveDialog) {
//        MoveMaterialDialog(
//            itemToMove = item,
//            moveQty = moveQty,
//            targetStorage = targetStorage,
//            isLoading = uiState is MainViewModel.UiState.Loading && !showDialogSuccess,
//            isSuccess = showDialogSuccess,
//            errorMessage = dialogError,
//            onEditClick = {
//                // С диалога перемещения уже не идём в редактирование
//                // (в новой архитектуре редактирование — отдельная кнопка в ActionBar)
//            },
//            onDismissRequest = {
//                if (!showDialogSuccess && uiState !is MainViewModel.UiState.Loading) {
//                    showMoveDialog = false
//                    dialogError = null
//                }
//            },
//            onQtyChange = { moveQty = it },
//            onTargetStorageChange = { targetStorage = it },
//            onClearError = { dialogError = null },
//            onConfirmMove = { it, qtyStr, toStorage ->
//                val qty = qtyStr.toIntOrNull() ?: 0
//                if (qty > 0 && toStorage.isNotEmpty()) {
//                    dialogError = null
//                    showDialogSuccess = false
//                    mainViewModel.moveWmsMaterial(it.material, it.storage, toStorage, qty)
//                }
//            },
//            onSuccessAcknowledge = {
//                showMoveDialog = false
//                showDialogSuccess = false
//                Toast.makeText(context, "Перемещение успешно", Toast.LENGTH_SHORT).show()
//                mainViewModel.loadWmsData()
//            }
//        )
//    }
//
//    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ТОПОЛОГИИ ===
//    if (showEditDialog && editState != null) {
//        EditWmsItemDialog(
//            item = item,
//            currentStorageId = item.storageId.toString(),
//            topologies = topologies,
//            isLoadingTopologies = false,
//            initialPosition = editState!!.position,
//            initialMax = editState!!.max,
//            initialMin = editState!!.min,
//            initialMaterial = editState!!.material,
//            initialQty = editState!!.qty,
//            onPositionSelected = { position, positionId ->
//                editState = editState!!.copy(position = position, positionId = positionId)
//            },
//            onMaxChange = { editState = editState!!.copy(max = it) },
//            onMinChange = { editState = editState!!.copy(min = it) },
//            onMaterialChange = { editState = editState!!.copy(material = it) },
//            onQtyChange = { editState = editState!!.copy(qty = it) },
//            onDismiss = { showEditDialog = false },
//            onSave = {
//                // Валидация
//                val minInt = editState!!.min.toIntOrNull() ?: item.min
//                val maxInt = editState!!.max.toIntOrNull() ?: item.max
//                val qtyInt = editState!!.qty.toIntOrNull() ?: item.qty.toInt()
//
//                if (editState!!.position.isBlank() || editState!!.positionId.isBlank()) {
//                    Toast.makeText(context, "Выберите позицию", Toast.LENGTH_SHORT).show()
//                    return@EditWmsItemDialog
//                }
//                if (minInt < 0 || maxInt < 0 || minInt > maxInt) {
//                    Toast.makeText(context, "Проверьте значения остатков", Toast.LENGTH_SHORT).show()
//                    return@EditWmsItemDialog
//                }
//
//                mainViewModel.updateWmsItem(
//                    item = item,
//                    newPosition = editState!!.position,
//                    newPositionId = editState!!.positionId,
//                    newMin = minInt,
//                    newMax = maxInt,
//                    newMaterial = if (item.sapA == 0) editState!!.material else null,
//                    newQty = if (item.sapA == 0) qtyInt else null,
//                    onSuccess = {
//                        showEditDialog = false
//                        mainViewModel.loadWmsData()
//                    },
//                    onError = { error ->
//                        Toast.makeText(context, "Ошибка: $error", Toast.LENGTH_SHORT).show()
//                    }
//                )
//            },
//            onLoadTopologies = { storageId ->
//                mainViewModel.loadTopologies(storageId)
//            }
//        )
//    }
//}
//
//// ====================== ДИАЛОГ РЕДАКТИРОВАНИЯ ======================
//@SuppressLint("ConfigurationScreenWidthHeight")
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun EditWmsItemDialog(
//    item: WmsItemDto,
//    currentStorageId: String,
//    topologies: List<TopologyDto>,
//    isLoadingTopologies: Boolean,
//    initialPosition: String,
//    initialMax: String,
//    initialMin: String,
//    initialMaterial: String,
//    initialQty: String,
//    onPositionSelected: (String, String) -> Unit,
//    onMaxChange: (String) -> Unit,
//    onMinChange: (String) -> Unit,
//    onMaterialChange: (String) -> Unit,
//    onQtyChange: (String) -> Unit,
//    onDismiss: () -> Unit,
//    onSave: () -> Unit,
//    onLoadTopologies: (String) -> Unit
//) {
//    val scrollState = rememberScrollState()
//    val focusManager = LocalFocusManager.current
//
//    // Локальные состояния
//    var materialField by remember { mutableStateOf(initialMaterial) }
//    var positionField by remember { mutableStateOf(initialPosition) }
//    var qtyField by remember { mutableStateOf(initialQty) }
//    var maxField by remember { mutableStateOf(initialMax) }
//    var minField by remember { mutableStateOf(initialMin) }
//
//    var expanded by remember { mutableStateOf(false) }
//
//    // Загрузка топологий
//    LaunchedEffect(Unit) {
//        if (topologies.isEmpty() && currentStorageId.isNotBlank()) {
//            onLoadTopologies(currentStorageId)
//        }
//    }
//
//    // Синхронизация с внешними изменениями
//    LaunchedEffect(initialMaterial) { materialField = initialMaterial }
//    LaunchedEffect(initialPosition) { positionField = initialPosition }
//    LaunchedEffect(initialQty) { qtyField = initialQty }
//    LaunchedEffect(initialMax) { maxField = initialMax }
//    LaunchedEffect(initialMin) { minField = initialMin }
//
//    // Ключевое: imePadding + fixed height предотвращают "прыжки"
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        properties = androidx.compose.ui.window.DialogProperties(
//            usePlatformDefaultWidth = false
//        ),
//        modifier = Modifier
//            .fillMaxWidth()
//            .imePadding(), // Учитывает клавиатуру
//        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
//        shape = RoundedCornerShape(20.dp),
//        title = {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Surface(
//                    shape = CircleShape,
//                    color = MaterialTheme.colorScheme.primaryContainer,
//                    modifier = Modifier.size(40.dp)
//                ) {
//                    Icon(
//                        Icons.Default.Edit,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
//                        modifier = Modifier.padding(8.dp)
//                    )
//                }
//                Column {
//                    Text(
//                        "Редактирование",
//                        style = MaterialTheme.typography.bodyLarge,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Text(
//                        item.material,
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Text(
//                        item.name,
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//        },
//        text = {
//            Column(
//                modifier = Modifier
//                    .verticalScroll(scrollState)
//                    .padding(vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                // === Группа 1: Основные поля ===
//                if (item.sapA == 0) {
//                    Surface(
//                        shape = RoundedCornerShape(16.dp),
//                        color = MaterialTheme.colorScheme.surface,
//                        shadowElevation = 2.dp,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Column(modifier = Modifier.padding(16.dp)) {
//                            // Артикул - показываем только если это виртуальный материал
//                            OutlinedTextField(
//                                value = materialField,
//                                onValueChange = { newValue ->
//                                    materialField = newValue
//                                    onMaterialChange(newValue)
//                                },
//                                label = { Text("Артикул") },
//                                modifier = Modifier.fillMaxWidth(),
//                                readOnly = (item.sapA == 1),
//                                enabled = (item.sapA == 0),
//                                singleLine = true,
//                                textStyle = MaterialTheme.typography.bodyMedium,
//                                isError = item.sapA == 0 && materialField.isBlank(),
//                                supportingText = {
//                                    if (item.sapA == 0 && materialField.isBlank()) {
//                                        Text(
//                                            "Обязательно", color = MaterialTheme.colorScheme.error
//                                        )
//                                    } else if (item.sapA == 1) {
//                                        Text(
//                                            "Не редактируется",
//                                            style = MaterialTheme.typography.labelSmall
//                                        )
//                                    }
//                                }
//                            )
//
//                            // Количество (только для non-SAP)
//                            Spacer(Modifier.height(8.dp))
//                            OutlinedTextField(
//                                value = qtyField,
//                                onValueChange = { newValue ->
//                                    if (newValue.all { c -> c.isDigit() }) {
//                                        qtyField = newValue
//                                        onQtyChange(newValue)
//                                    }
//                                },
//                                label = { Text("Количество") },
//                                modifier = Modifier.fillMaxWidth(),
//                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                                singleLine = true,
//                                textStyle = MaterialTheme.typography.bodyMedium
//                            )
//                        }
//                    }
//                }
//
//                // === Группа 2: Позиция (топология) ===
//                Surface(
//                    shape = RoundedCornerShape(16.dp),
//                    color = MaterialTheme.colorScheme.surface,
//                    shadowElevation = 2.dp,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text("Позиция (топология)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
//                        Spacer(Modifier.height(8.dp))
//
//                        ExposedDropdownMenuBox(
//                            expanded = expanded,
//                            onExpandedChange = { expanded = !expanded }
//                        ) {
//                            OutlinedTextField(
//                                value = positionField,
//                                onValueChange = {},
//                                label = { Text("Выберите позицию") },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .menuAnchor(),
//                                readOnly = true,
//                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                                singleLine = true
//                            )
//                            ExposedDropdownMenu(
//                                expanded = expanded,
//                                onDismissRequest = { expanded = false },
//                                modifier = Modifier.fillMaxWidth(0.95f)
//                            ) {
//                                if (isLoadingTopologies) {
//                                    DropdownMenuItem(text = { Text("Загрузка...") }, onClick = {})
//                                } else if (topologies.isEmpty()) {
//                                    DropdownMenuItem(text = { Text("Нет позиций") }, onClick = {})
//                                } else {
//                                    topologies.forEach { topology ->
//                                        Log.d("TOPOLOGY", "positionField=${positionField} | position=${topology.position} id=${topology.id}")
//                                        DropdownMenuItem(
//                                            text = { Text(topology.position) },
//                                            onClick = {
//                                                onPositionSelected(topology.position, topology.id)
//                                                positionField = topology.position
//                                                expanded = false
//                                            },
//                                            leadingIcon = {
//                                                if (topology.position == positionField) {
//                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
//                                                }
//                                            }
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                // === Группа 3: Остатки ===
//                Surface(
//                    shape = RoundedCornerShape(16.dp),
//                    color = MaterialTheme.colorScheme.surface,
//                    shadowElevation = 2.dp,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text("Лимиты остатков", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
//                        Spacer(Modifier.height(8.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            // Мин. остаток
//                            OutlinedTextField(
//                                value = minField,
//                                onValueChange = { newValue ->
//                                    if (newValue.all { c -> c.isDigit() }) {
//                                        minField = newValue
//                                        onMinChange(newValue)
//                                    }
//                                },
//                                label = { Text("Мин.") },
//                                modifier = Modifier.weight(1f),
//                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                                singleLine = true,
//                                textStyle = MaterialTheme.typography.bodyMedium
//                            )
//                            // Макс. остаток
//                            OutlinedTextField(
//                                value = maxField,
//                                onValueChange = { newValue ->
//                                    if (newValue.all { c -> c.isDigit() }) {
//                                        maxField = newValue
//                                        onMaxChange(newValue)
//                                    }
//                                },
//                                label = { Text("Макс.") },
//                                modifier = Modifier.weight(1f),
//                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                                singleLine = true,
//                                textStyle = MaterialTheme.typography.bodyMedium,
//                                isError = maxField.toIntOrNull() != null &&
//                                        minField.toIntOrNull() != null &&
//                                        maxField.toInt() < minField.toInt()
//                            )
//                        }
//                        if (maxField.toIntOrNull() != null && minField.toIntOrNull() != null && maxField.toInt() < minField.toInt()) {
//                            Text("Макс. не может быть меньше мин.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            Button(
//                onClick = {
//                    focusManager.clearFocus() // Скрываем клавиатуру перед сохранением
//                    onSave()
//                },
//                modifier = Modifier
//                    .height(48.dp),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Text("Сохранить изменения", fontWeight = FontWeight.Medium)
//            }
//        },
//        dismissButton = {
//            TextButton(
//                onClick = {
//                    focusManager.clearFocus()
//                    onDismiss()
//                },
//                modifier = Modifier
//                    .height(48.dp)
//            ) {
//                Text("Отмена", fontWeight = FontWeight.Medium)
//            }
//        }
//    )
//}
//
//// ====================== ДИАЛОГ ПЕРЕМЕЩЕНИЯ ======================
//@Composable
//fun MoveMaterialDialog(
//    itemToMove: WmsItemDto,
//    moveQty: String,
//    targetStorage: String,
//    isLoading: Boolean,
//    isSuccess: Boolean,
//    errorMessage: String?,
//    onEditClick: (WmsItemDto) -> Unit,
//    onDismissRequest: () -> Unit,
//    onQtyChange: (String) -> Unit,
//    onTargetStorageChange: (String) -> Unit,
//    onClearError: () -> Unit,
//    onConfirmMove: (WmsItemDto, String, String) -> Unit,
//    onSuccessAcknowledge: () -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = if (isLoading || isSuccess) { {} } else { onDismissRequest },
//        properties = androidx.compose.ui.window.DialogProperties(
//            usePlatformDefaultWidth = false
//        ),
//        icon = {
//            if (isSuccess) {
//                Icon(
//                    Icons.Default.CheckCircle,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(48.dp)
//                )
//            } else {
//                Icon(
//                    Icons.Default.SwapHoriz,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//        },
//        title = {
//            Text(
//                if (isSuccess) "Успешно!" else "Перемещение материала",
//                style = MaterialTheme.typography.headlineSmall
//            )
//        },
//        text = {
//            if (isSuccess) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    modifier = Modifier.padding(vertical = 16.dp)
//                ) {
//                    Text("Материал перемещен", style = MaterialTheme.typography.bodyLarge)
//                    Spacer(Modifier.height(8.dp))
//                    Card(
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.surfaceVariant
//                        ),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Column(modifier = Modifier.padding(12.dp)) {
//                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
//                                Text("Со склада:", style = MaterialTheme.typography.labelSmall)
//                                Text(itemToMove.storage, fontWeight = FontWeight.Bold)
//                            }
//                            Spacer(Modifier.height(4.dp))
//                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
//                                Text("На склад:", style = MaterialTheme.typography.labelSmall)
//                                Text(targetStorage, fontWeight = FontWeight.Bold)
//                            }
//                            Spacer(Modifier.height(4.dp))
//                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
//                                Text("Количество:", style = MaterialTheme.typography.labelSmall)
//                                Text(moveQty, fontWeight = FontWeight.Bold)
//                            }
//                        }
//                    }
//                }
//            } else {
//                Column(
//                    modifier = Modifier
//                        .verticalScroll(rememberScrollState())
//                        .padding(bottom = 16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    Card(
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.surfaceVariant
//                        ),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Column(modifier = Modifier.padding(10.dp)) {
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                Text(
//                                    text = itemToMove.material,
//                                    style = MaterialTheme.typography.titleMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis,
//                                    modifier = Modifier.weight(1f)
//                                )
//                                Surface(
//                                    shape = MaterialTheme.shapes.small,
//                                    color = MaterialTheme.colorScheme.secondaryContainer
//                                ) {
//                                    Text(
//                                        text = itemToMove.position,
//                                        style = MaterialTheme.typography.labelMedium,
//                                        fontWeight = FontWeight.Medium,
//                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
//                                    )
//                                }
//                                if (itemToMove.sapA == 1) {
//                                    Surface(
//                                        shape = RoundedCornerShape(12.dp),
//                                        color = Color(0xFF667eea), // Фиолетовый градиент-стиль
//                                        modifier = Modifier.padding(start = 4.dp)
//                                    ) {
//                                        Row(
//                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
//                                            verticalAlignment = Alignment.CenterVertically,
//                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
//                                        ) {
//                                            Icon(
//                                                Icons.AutoMirrored.Filled.Assignment,
//                                                contentDescription = "SAP",
//                                                tint = Color.White,
//                                                modifier = Modifier.size(14.dp)
//                                            )
//                                            Text(
//                                                text = "SAP",
//                                                style = MaterialTheme.typography.labelSmall,
//                                                fontWeight = FontWeight.SemiBold,
//                                                color = Color.White
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                            Spacer(modifier = Modifier.height(2.dp))
//                            Text(
//                                text = itemToMove.name,
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis
//                            )
//                            Spacer(modifier = Modifier.height(6.dp))
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                Text(
//                                    text = "Склад: ${itemToMove.storage}",
//                                    style = MaterialTheme.typography.labelMedium,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(
//                                    text = "${itemToMove.qty.toInt()} шт.",
//                                    style = MaterialTheme.typography.labelLarge,
//                                    fontWeight = FontWeight.SemiBold,
//                                    color = MaterialTheme.colorScheme.primary
//                                )
//                            }
//                        }
//                    }
//
//                    OutlinedTextField(
//                        value = targetStorage,
//                        onValueChange = onTargetStorageChange,
//                        label = { Text("Целевой склад") },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        enabled = !isLoading,
//                        isError = targetStorage.isEmpty() && !isLoading
//                    )
//
//                    OutlinedTextField(
//                        value = moveQty,
//                        onValueChange = { newValue ->
//                            if (newValue.all { it.isDigit() }) {
//                                val inputQty = newValue.toIntOrNull() ?: 0
//                                val maxQty = itemToMove.qty.toInt()
//                                if (inputQty <= maxQty || newValue.isEmpty()) onQtyChange(newValue)
//                            }
//                        },
//                        label = { Text("Количество") },
//                        supportingText = { Text("Макс: ${itemToMove.qty.toInt()}") },
//                        modifier = Modifier.fillMaxWidth(),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        singleLine = true,
//                        enabled = !isLoading,
//                        isError = (moveQty.toIntOrNull() ?: 0) > itemToMove.qty.toInt() && moveQty.isNotEmpty()
//                    )
//
//                    if (errorMessage != null) {
//                        Card(
//                            colors = CardDefaults.cardColors(
//                                containerColor = MaterialTheme.colorScheme.errorContainer
//                            ),
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Row(
//                                modifier = Modifier.padding(12.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Icon(
//                                    Icons.Default.ErrorOutline,
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colorScheme.onErrorContainer,
//                                    modifier = Modifier.size(24.dp)
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(
//                                    text = errorMessage,
//                                    color = MaterialTheme.colorScheme.onErrorContainer,
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    modifier = Modifier.weight(1f)
//                                )
//                                IconButton(onClick = onClearError) {
//                                    Icon(
//                                        Icons.Default.Clear,
//                                        contentDescription = "Закрыть",
//                                        tint = MaterialTheme.colorScheme.onErrorContainer,
//                                        modifier = Modifier.size(16.dp)
//                                    )
//                                }
//                            }
//                        }
//                    }
//
//                    if (isLoading) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.Center,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Выполняется перемещение...")
//                        }
//                    }
//
//                    // Кнопка "Редактировать"
//                    if (!isLoading) {
//                        TextButton(
//                            onClick = { onEditClick(itemToMove) },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 8.dp)
//                        ) {
//                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
//                            Spacer(Modifier.width(4.dp))
//                            Text("Редактировать материал")
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            if (isSuccess) {
//                Button(
//                    onClick = onSuccessAcknowledge,
//                    modifier = Modifier.height(48.dp)
//                ) {
//                    Text("ОК")
//                }
//            } else {
//                Button(
//                    onClick = { onConfirmMove(itemToMove, moveQty, targetStorage) },
//                    enabled = !isLoading &&
//                            targetStorage.isNotEmpty() &&
//                            moveQty.isNotEmpty() &&
//                            (moveQty.toIntOrNull() ?: 0) > 0 &&
//                            (moveQty.toIntOrNull() ?: 0) <= itemToMove.qty.toInt(),
//                    modifier = Modifier.height(48.dp)
//                ) {
//                    Text("Переместить")
//                }
//            }
//        },
//        dismissButton = {
//            if (!isSuccess) {
//                TextButton(
//                    onClick = onDismissRequest,
//                    enabled = !isLoading,
//                    modifier = Modifier.height(48.dp)
//                ) {
//                    Text("Отмена")
//                }
//            }
//        }
//    )
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun WmsItemDetailsContent(
//    item: WmsItemDto,
//    onBackClick: () -> Unit,
//    onMoveClick: () -> Unit,
//    onChangeTopologyClick: () -> Unit
//) {
//    Column(modifier = Modifier.fillMaxSize()) {
//        MyCustomActionBar(
//            text = item.material,
//            onBackClick = onBackClick,
//            actionButton = {
//                // Кнопка "Редактировать" в ActionBar — открывает тот же диалог изменения топологии
//                IconButton(onClick = onChangeTopologyClick) {
//                    Icon(
//                        Icons.Default.Edit,
//                        contentDescription = "Редактировать",
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                }
//            }
//        )
//
//        LazyColumn(
//            modifier = Modifier.weight(1f),
//            contentPadding = PaddingValues(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            // === Карточка статуса/остатка ===
//            item {
//                WmsQuantityCard(item = item)
//            }
//
//            // === Основная информация ===
//            item {
//                WmsInfoSection(item = item)
//            }
//
//            // === Склад и позиция ===
//            item {
//                WmsLocationSection(item = item)
//            }
//
//            // === Остатки (мин/макс) ===
//            item {
//                WmsLimitsSection(item = item)
//            }
//
//            // === Мета-информация ===
//            item {
//                WmsMetaSection(item = item)
//            }
//        }
//
//        // === Кнопки действий внизу ===
//        Surface {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                OutlinedButton(
//                    onClick = onChangeTopologyClick,
//                    modifier = Modifier.weight(1f),
//                    shape = RoundedCornerShape(12.dp)
//                ) {
//                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
//                    Spacer(Modifier.width(6.dp))
//                    Text("Изменить топологию")
//                }
//                Button(
//                    onClick = onMoveClick,
//                    modifier = Modifier.weight(1f),
//                    shape = RoundedCornerShape(12.dp)
//                ) {
//                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
//                    Spacer(Modifier.width(6.dp))
//                    Text("Переместить", softWrap = false)
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun WmsQuantityCard(item: WmsItemDto) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
//        )
//    ) {
//        Row(
//            modifier = Modifier.padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Surface(
//                shape = RoundedCornerShape(8.dp),
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(48.dp)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Icon(
//                        Icons.Outlined.Warehouse,
//                        contentDescription = null,
//                        tint = Color.White,
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//            Spacer(Modifier.width(16.dp))
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    "Остаток",
//                    style = MaterialTheme.typography.labelMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Text(
//                    "${item.qty.toInt()} шт.",
//                    style = MaterialTheme.typography.headlineSmall,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//            if (item.sapA == 1) SapBadge()
//        }
//    }
//}
//
//@Composable
//private fun WmsInfoSection(item: WmsItemDto) {
//    WmsSectionCard(icon = Icons.Default.Info, title = "Основная информация") {
//        WmsInfoRow("Артикул", item.material, copyable = true)
//        WmsInfoRow("Название", item.name)
//        WmsInfoRow("Цена", item.price.toString())
//    }
//}
//
//@Composable
//private fun WmsLocationSection(item: WmsItemDto) {
//    WmsSectionCard(icon = Icons.Outlined.LocationOn, title = "Местоположение") {
//        WmsInfoRow("Склад", item.storage)
//        WmsInfoRow("Позиция", item.position)
//        WmsInfoRow("ID позиции", item.positionId.toString())
//    }
//}
//
//@Composable
//private fun WmsLimitsSection(item: WmsItemDto) {
//    WmsSectionCard(icon = Icons.Default.Tune, title = "Лимиты остатков") {
//        WmsInfoRow("Минимум", item.min.toString())
//        WmsInfoRow("Максимум", item.max.toString())
//    }
//}
//
//@Composable
//private fun WmsMetaSection(item: WmsItemDto) {
//    WmsSectionCard(icon = Icons.Default.Info, title = "Мета-информация") {
//        WmsInfoRow("ID записи", item.id.toString())
//        WmsInfoRow("SAP", if (item.sapA == 1) "Да" else "Нет")
//    }
//}
//
//@Composable
//private fun WmsSectionCard(
//    icon: ImageVector,
//    title: String,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(icon, null, modifier = Modifier.size(28.dp))
//                Spacer(Modifier.width(8.dp))
//                Text(title, style = MaterialTheme.typography.titleMedium)
//            }
//            Spacer(Modifier.height(12.dp))
//            content()
//        }
//    }
//}
//
//@Composable
//private fun WmsInfoRow(label: String, value: String?, copyable: Boolean = false) {
//    if (value.isNullOrBlank()) return
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            label,
//            style = MaterialTheme.typography.labelMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        Text(
//            value,
//            style = MaterialTheme.typography.bodyMedium,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun WmsItemDetailsContentPreview(){
//    WmsItemDetailsContent(
//        item = WmsItemDto(
//            id = 1,
//            name = "название",
//            material = "материал",
//            max = 111,
//            min = 10,
//            positionId = 1,
//            position = "Позиция",
//            price = 0.1,
//            qty = 1.1,
//            sapA = 1,
//            storage = "3051",
//            storageId = 1
//        ),
//        onBackClick = {},
//        onMoveClick = {},
//        onChangeTopologyClick = {}
//    )
//}
//
//@Preview(showBackground = true, name = "WMS Screen - MoveDialog")
//@Composable
//fun MoveDialogPreview() {
//    MoveMaterialDialog(
//        itemToMove = WmsItemDto(
//            id = 1,
//            material = "material",
//            max = 100,
//            min = 1,
//            positionId = 0,
//            position = "position",
//            price = 211.0,
//            qty = 222.0,
//            sapA = 1,
//            storage = "storage",
//            storageId = 3,
//            name = "name"
//        ),
//        moveQty = "10",
//        targetStorage = "3051",
//        isLoading = false,
//        isSuccess = false,
//        errorMessage = "",
//        onEditClick = {},
//        onDismissRequest = {},
//        onQtyChange = {},
//        onTargetStorageChange = {},
//        onClearError = {},
//        onConfirmMove = {_, _, _ -> {}},
//        onSuccessAcknowledge = {}
//    )
//}
//
//@Preview(showBackground = true, name = "WMS Screen - EditDialog")
//@Composable
//fun EditDialogPreview() {
//    EditWmsItemDialog(
//        item = WmsItemDto(
//            id = 1,
//            material = "material",
//            max = 100,
//            min = 1,
//            positionId = 0,
//            position = "BUFF",
//            price = 211.0,
//            qty = 222.0,
//            sapA = 0,
//            storage = "storage",
//            storageId = 3,
//            name = "Test Name"
//        ),
//        currentStorageId = "1",
//        topologies = listOf(
//            TopologyDto(id = "1", position = "BUFF", positionScan = "BUFF"),
//            TopologyDto(id = "2", position = "A-01", positionScan = "A1")
//        ),
//        isLoadingTopologies = false,
//        initialPosition = "BUFF",
//        initialMax = "100",
//        initialMin = "0",
//        initialMaterial = "LA0602600443",
//        initialQty = "42",
//        onPositionSelected = {_, _ -> },
//        onMaxChange = {},
//        onMinChange = {},
//        onMaterialChange = {},
//        onQtyChange = {},
//        onDismiss = {} ,
//        onSave = {} ,
//        onLoadTopologies = {}
//    )
//}


package com.gps.warehouse.ui.gps_screens.warehouse

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.TopologyDto
import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SapBadge
import com.gps.warehouse.utils.ScannerManager

private const val TAG = "WmsItemDetails"

// ====================== Внутреннее состояние редактирования ======================
data class WmsEditState(
    val material: String,
    val position: String,
    val positionId: String,
    val qty: String,
    val min: String,
    val max: String
) {
    companion object {
        fun fromItem(item: WmsItemDto) = WmsEditState(
            material = item.material,
            position = item.position,
            positionId = item.positionId.toString(),
            qty = item.qty.toInt().toString(),
            min = item.min.toString(),
            max = item.max.toString()
        )
    }
}

// ====================== SCREEN ======================
@Composable
fun WmsItemDetailsScreen(
    material: String,
    storageId: String,
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by mainViewModel.uiState.collectAsState()
    val topologies by mainViewModel.topologies.collectAsState()

    // Ищем элемент в ТЕКУЩЕМ состоянии
    val currentItem: WmsItemDto? = remember(uiState, material, storageId) {
        (uiState as? MainViewModel.UiState.WmsLoaded)?.items?.find {
            it.material == material && it.storageId.toString() == storageId
        }
    }

    // Кэш последнего найденного элемента.
    // Ключи (material, storageId) — чтобы кэш сбрасывался при переходе к ДРУГОМУ материалу,
    // но НЕ сбрасывался при перезагрузке того же самого.
    var lastFoundItem by remember(material, storageId) { mutableStateOf<WmsItemDto?>(null) }

    // Обновляем кэш только когда элемент реально найден
    LaunchedEffect(currentItem) {
        if (currentItem != null) {
            lastFoundItem = currentItem
        }
    }

    // Итоговый элемент: свежий ИЛИ последний известный
    val item: WmsItemDto? = currentItem ?: lastFoundItem

    // Экран ошибки показываем ТОЛЬКО если данные загружены, а элемента реально нет.
    // Во время Loading показываем индикатор, а не ошибку.
    if (item == null) {
        when (uiState) {
            is MainViewModel.UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            else -> ErrorStateView(
                message = "Материал не найден",
                onRetry = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }


    // === Состояние диалогов ===
    var showMoveDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var moveQty by remember { mutableStateOf("") }
    var targetStorage by remember { mutableStateOf("") }
    var showMoveSuccess by remember { mutableStateOf(false) }
    var moveError by remember { mutableStateOf<String?>(null) }

    // === Состояние редактирования ===
    var editState by remember { mutableStateOf<WmsEditState?>(null) }

    // === Сканер (для топологии) ===
    val honeywellHelper = remember { ScannerManager(context) }
    val currentShowMoveDialog by rememberUpdatedState(showMoveDialog)
    val currentTopologies by rememberUpdatedState(topologies)

    // Загрузка топологий при открытии экрана
    LaunchedEffect(storageId) {
        if (storageId.isNotBlank()) {
            mainViewModel.loadTopologies(storageId)
        }
    }

    // Сканирование: если открыт диалог перемещения — ищем топологию
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isEmpty()) return@collect
            if (!currentShowMoveDialog) return@collect

            val topologyCode = scannedData.trim()
            val matched = currentTopologies.find {
                it.positionScan.equals(topologyCode, ignoreCase = true)
            }
            if (matched != null) {
                targetStorage = matched.position
                moveError = null
                Toast.makeText(context, "Позиция: ${matched.position}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Позиция '$topologyCode' не найдена", Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(Unit) {
        honeywellHelper.init()
        onDispose { honeywellHelper.release() }
    }

    // Инициализация editState при первом показе item
//    LaunchedEffect(item) {
//        if (item != null && editState == null) {
//            editState = WmsEditState.fromItem(item)
//        }
//    }

    // Реакция на результат перемещения
    LaunchedEffect(uiState) {
        if (!showMoveDialog) return@LaunchedEffect
        when (val s = uiState) {
            is MainViewModel.UiState.WmsMoveSuccess -> showMoveSuccess = true
            is MainViewModel.UiState.Error -> moveError = s.message
            else -> Unit
        }
    }

    // === Контент ===
    WmsItemDetailsContent(
        item = item,
        onBackClick = { navController.popBackStack() },
        onMoveClick = {
            // Сброс состояния диалога перемещения
            moveQty = if (item.qty > 0) item.qty.toInt().toString() else ""
            targetStorage = ""
            moveError = null
            showMoveSuccess = false
            showMoveDialog = true
        },
        onChangeTopologyClick = {
            // Сброс состояния редактирования
            editState = WmsEditState.fromItem(item)
            showEditDialog = true
        }
    )

    // === ДИАЛОГ ПЕРЕМЕЩЕНИЯ ===
    if (showMoveDialog) {
        MoveMaterialDialog(
            itemToMove = item,
            moveQty = moveQty,
            targetStorage = targetStorage,
            isLoading = uiState is MainViewModel.UiState.Loading && !showMoveSuccess,
            isSuccess = showMoveSuccess,
            errorMessage = moveError,
            onDismissRequest = {
                if (!showMoveSuccess && uiState !is MainViewModel.UiState.Loading) {
                    showMoveDialog = false
                    moveError = null
                }
            },
            onQtyChange = { moveQty = it },
            onTargetStorageChange = { targetStorage = it },
            onClearError = { moveError = null },
            onConfirmMove = { toStorage ->
                val qty = moveQty.toIntOrNull() ?: 0
                if (qty > 0 && toStorage.isNotEmpty()) {
                    moveError = null
                    showMoveSuccess = false
                    mainViewModel.moveWmsMaterial(item.material, item.storage, toStorage, qty)
                }
            },
            onSuccessAcknowledge = {
                showMoveDialog = false
                showMoveSuccess = false
                Toast.makeText(context, "Перемещение успешно", Toast.LENGTH_SHORT).show()
                mainViewModel.loadWmsData()
            }
        )
    }

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ТОПОЛОГИИ ===
    if (showEditDialog && editState != null) {
        EditWmsItemDialog(
            item = item,
            currentStorageId = item.storageId.toString(),
            topologies = topologies,
            isLoadingTopologies = false,
            initialPosition = editState!!.position,
            initialMax = editState!!.max,
            initialMin = editState!!.min,
            initialMaterial = editState!!.material,
            initialQty = editState!!.qty,
            onPositionSelected = { position, positionId ->
                editState = editState!!.copy(position = position, positionId = positionId)
            },
            onMaxChange = { editState = editState!!.copy(max = it) },
            onMinChange = { editState = editState!!.copy(min = it) },
            onMaterialChange = { editState = editState!!.copy(material = it) },
            onQtyChange = { editState = editState!!.copy(qty = it) },
            onDismiss = { showEditDialog = false },
            onSave = {
                val state = editState ?: return@EditWmsItemDialog
                val minInt = state.min.toIntOrNull() ?: item.min
                val maxInt = state.max.toIntOrNull() ?: item.max
                val qtyInt = state.qty.toIntOrNull() ?: item.qty.toInt()

                // Валидация
                if (state.position.isBlank() || state.positionId.isBlank()) {
                    Toast.makeText(context, "Выберите позицию", Toast.LENGTH_SHORT).show()
                    return@EditWmsItemDialog
                }
                if (minInt < 0 || maxInt < 0 || minInt > maxInt) {
                    Toast.makeText(context, "Проверьте значения остатков", Toast.LENGTH_SHORT).show()
                    return@EditWmsItemDialog
                }
                if (item.sapA == 0 && (qtyInt < 0 || state.qty.isBlank())) {
                    Toast.makeText(context, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                    return@EditWmsItemDialog
                }

                mainViewModel.updateWmsItem(
                    item = item,
                    newPosition = state.position,
                    newPositionId = state.positionId,
                    newMin = minInt,
                    newMax = maxInt,
                    newMaterial = if (item.sapA == 0) state.material else null,
                    newQty = if (item.sapA == 0) qtyInt else null,
                    onSuccess = {
                        showEditDialog = false
                        mainViewModel.loadWmsData()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Ошибка: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onLoadTopologies = { sid -> mainViewModel.loadTopologies(sid) }
        )
    }
}

// ====================== CONTENT ======================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmsItemDetailsContent(
    item: WmsItemDto,
    onBackClick: () -> Unit,
    onMoveClick: () -> Unit,
    onChangeTopologyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            text = item.material,
            onBackClick = onBackClick,
            actionButton = {
                IconButton(onClick = onChangeTopologyClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { WmsQuantityCard(item) }
            item { WmsInfoSection(item) }
            item { WmsLocationSection(item) }
            item { WmsLimitsSection(item) }
            item { WmsMetaSection(item) }
        }

        // Кнопки действий внизу
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeTopologyClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Изменить топологию", softWrap = false)
                }
                Button(
                    onClick = onMoveClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Переместить")
                }
            }
        }
    }
}

// ====================== КАРТОЧКИ ======================
@Composable
private fun WmsQuantityCard(item: WmsItemDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Warehouse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Остаток",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${item.qty.toInt()} шт.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (item.sapA == 1) SapBadge()
        }
    }
}

@Composable
private fun WmsInfoSection(item: WmsItemDto) {
    WmsSectionCard(icon = Icons.Default.Info, title = "Основная информация") {
        WmsInfoRow("Артикул", item.material)
        WmsInfoRow("Название", item.name)
        WmsInfoRow("Цена", item.price.toString())
    }
}

@Composable
private fun WmsLocationSection(item: WmsItemDto) {
    WmsSectionCard(icon = Icons.Outlined.LocationOn, title = "Местоположение") {
        WmsInfoRow("Склад", item.storage)
        WmsInfoRow("Топология", item.position)
        WmsInfoRow("ID позиции", item.positionId.toString())
    }
}

@Composable
private fun WmsLimitsSection(item: WmsItemDto) {
    WmsSectionCard(icon = Icons.Default.Tune, title = "Лимиты остатков") {
        WmsInfoRow("Минимум", item.min.toString())
        WmsInfoRow("Максимум", item.max.toString())
    }
}

@Composable
private fun WmsMetaSection(item: WmsItemDto) {
    WmsSectionCard(icon = Icons.Default.Info, title = "Мета-информация") {
        WmsInfoRow("ID записи", item.id.toString())
        WmsInfoRow("SAP", if (item.sapA == 1) "Да" else "Нет")
    }
}

@Composable
private fun WmsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun WmsInfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ====================== ДИАЛОГ РЕДАКТИРОВАНИЯ ======================
@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWmsItemDialog(
    item: WmsItemDto,
    currentStorageId: String,
    topologies: List<TopologyDto>,
    isLoadingTopologies: Boolean,
    initialPosition: String,
    initialMax: String,
    initialMin: String,
    initialMaterial: String,
    initialQty: String,
    onPositionSelected: (String, String) -> Unit,
    onMaxChange: (String) -> Unit,
    onMinChange: (String) -> Unit,
    onMaterialChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onLoadTopologies: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var materialField by remember { mutableStateOf(initialMaterial) }
    var positionField by remember { mutableStateOf(initialPosition) }
    var qtyField by remember { mutableStateOf(initialQty) }
    var maxField by remember { mutableStateOf(initialMax) }
    var minField by remember { mutableStateOf(initialMin) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (topologies.isEmpty() && currentStorageId.isNotBlank()) {
            onLoadTopologies(currentStorageId)
        }
    }

    LaunchedEffect(initialMaterial) { materialField = initialMaterial }
    LaunchedEffect(initialPosition) { positionField = initialPosition }
    LaunchedEffect(initialQty) { qtyField = initialQty }
    LaunchedEffect(initialMax) { maxField = initialMax }
    LaunchedEffect(initialMin) { minField = initialMin }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column {
                    Text(
                        "Редактирование",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.material,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Группа 1: Основные поля (только для не-SAP)
                if (item.sapA == 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = materialField,
                                onValueChange = {
                                    materialField = it
                                    onMaterialChange(it)
                                },
                                label = { Text("Артикул") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = materialField.isBlank(),
                                supportingText = {
                                    if (materialField.isBlank()) {
                                        Text("Обязательно", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = qtyField,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        qtyField = it
                                        onQtyChange(it)
                                    }
                                },
                                label = { Text("Количество") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }

                // Группа 2: Позиция
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Позиция (топология)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = positionField,
                                onValueChange = {},
                                label = { Text("Выберите позицию") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.95f)
                            ) {
                                if (isLoadingTopologies) {
                                    DropdownMenuItem(text = { Text("Загрузка...") }, onClick = {})
                                } else if (topologies.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Нет позиций") }, onClick = {})
                                } else {
                                    topologies.forEach { topology ->
                                        DropdownMenuItem(
                                            text = { Text(topology.position) },
                                            onClick = {
                                                onPositionSelected(topology.position, topology.id)
                                                positionField = topology.position
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                if (topology.position == positionField) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Группа 3: Остатки
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Лимиты остатков",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = minField,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        minField = it
                                        onMinChange(it)
                                    }
                                },
                                label = { Text("Мин.") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = maxField,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        maxField = it
                                        onMaxChange(it)
                                    }
                                },
                                label = { Text("Макс.") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = maxField.toIntOrNull() != null &&
                                        minField.toIntOrNull() != null &&
                                        maxField.toInt() < minField.toInt()
                            )
                        }
                        if (maxField.toIntOrNull() != null && minField.toIntOrNull() != null &&
                            maxField.toInt() < minField.toInt()
                        ) {
                            Text(
                                "Макс. не может быть меньше мин.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить изменения", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Отмена", fontWeight = FontWeight.Medium)
            }
        }
    )
}

// ====================== ДИАЛОГ ПЕРЕМЕЩЕНИЯ ======================
@Composable
fun MoveMaterialDialog(
    itemToMove: WmsItemDto,
    moveQty: String,
    targetStorage: String,
    isLoading: Boolean,
    isSuccess: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onQtyChange: (String) -> Unit,
    onTargetStorageChange: (String) -> Unit,
    onClearError: () -> Unit,
    onConfirmMove: (String) -> Unit,
    onSuccessAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = if (isLoading || isSuccess) { {} } else { onDismissRequest },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        icon = {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isSuccess) 48.dp else 32.dp)
            )
        },
        title = {
            Text(
                if (isSuccess) "Успешно!" else "Перемещение материала",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            if (isSuccess) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text("Материал перемещен", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            SuccessRow("Со склада:", itemToMove.storage)
                            Spacer(Modifier.height(4.dp))
                            SuccessRow("На склад:", targetStorage)
                            Spacer(Modifier.height(4.dp))
                            SuccessRow("Количество:", moveQty)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Карточка материала
                    ItemInfoCard(itemToMove)

                    // Сканер топологии / целевой склад
                    OutlinedTextField(
                        value = targetStorage,
                        onValueChange = onTargetStorageChange,
                        label = { Text("Целевой склад / позиция") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = targetStorage.isEmpty() && !isLoading,
                        supportingText = { Text("Отсканируйте QR-код позиции") }
                    )

                    // Количество
                    OutlinedTextField(
                        value = moveQty,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                val inputQty = newValue.toIntOrNull() ?: 0
                                if (inputQty <= itemToMove.qty.toInt() || newValue.isEmpty()) {
                                    onQtyChange(newValue)
                                }
                            }
                        },
                        label = { Text("Количество") },
                        supportingText = { Text("Макс: ${itemToMove.qty.toInt()}") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = (moveQty.toIntOrNull() ?: 0) > itemToMove.qty.toInt() && moveQty.isNotEmpty()
                    )

                    // Ошибка
                    if (errorMessage != null) {
                        ErrorCard(errorMessage, onClearError)
                    }

                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Выполняется перемещение...")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(onClick = onSuccessAcknowledge, modifier = Modifier.height(48.dp)) {
                    Text("ОК")
                }
            } else {
                Button(
                    onClick = { onConfirmMove(targetStorage) },
                    enabled = !isLoading &&
                            targetStorage.isNotEmpty() &&
                            moveQty.isNotEmpty() &&
                            (moveQty.toIntOrNull() ?: 0) > 0 &&
                            (moveQty.toIntOrNull() ?: 0) <= itemToMove.qty.toInt(),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Переместить")
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(
                    onClick = onDismissRequest,
                    enabled = !isLoading,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Отмена")
                }
            }
        }
    )
}

@Composable
private fun SuccessRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemInfoCard(item: WmsItemDto) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.material,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = item.position,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (item.sapA == 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF667eea),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = "SAP",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SAP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Склад: ${item.storage}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.qty.toInt()} шт.",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onClear: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Закрыть",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ====================== PREVIEWS ======================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WmsItemDetailsContentPreview() {
    MaterialTheme {
        Surface {
            WmsItemDetailsContent(
                item = WmsItemDto(
                    id = 1,
                    name = "название",
                    material = "материал",
                    max = 111,
                    min = 10,
                    positionId = 1,
                    position = "BUFF",
                    price = 0.1,
                    qty = 1.1,
                    sapA = 1,
                    storage = "3051",
                    storageId = 1
                ),
                onBackClick = {},
                onMoveClick = {},
                onChangeTopologyClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "MoveDialog")
@Composable
fun MoveDialogPreview() {
    MaterialTheme {
        Surface {
            MoveMaterialDialog(
                itemToMove = WmsItemDto(
                    id = 1, material = "material", max = 100, min = 1,
                    positionId = 0, position = "position", price = 211.0,
                    qty = 222.0, sapA = 1, storage = "storage",
                    storageId = 3, name = "name"
                ),
                moveQty = "10",
                targetStorage = "3051",
                isLoading = false,
                isSuccess = false,
                errorMessage = null,
                onDismissRequest = {},
                onQtyChange = {},
                onTargetStorageChange = {},
                onClearError = {},
                onConfirmMove = {},
                onSuccessAcknowledge = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "EditDialog")
@Composable
fun EditDialogPreview() {
    MaterialTheme {
        Surface {
            EditWmsItemDialog(
                item = WmsItemDto(
                    id = 1, material = "material", max = 100, min = 1,
                    positionId = 0, position = "BUFF", price = 211.0,
                    qty = 222.0, sapA = 0, storage = "storage",
                    storageId = 3, name = "Test Name"
                ),
                currentStorageId = "1",
                topologies = listOf(
                    TopologyDto(id = "1", position = "BUFF", positionScan = "BUFF"),
                    TopologyDto(id = "2", position = "A-01", positionScan = "A1")
                ),
                isLoadingTopologies = false,
                initialPosition = "BUFF",
                initialMax = "100",
                initialMin = "0",
                initialMaterial = "LA0602600443",
                initialQty = "42",
                onPositionSelected = { _, _ -> },
                onMaxChange = {},
                onMinChange = {},
                onMaterialChange = {},
                onQtyChange = {},
                onDismiss = {},
                onSave = {},
                onLoadTopologies = {}
            )
        }
    }
}