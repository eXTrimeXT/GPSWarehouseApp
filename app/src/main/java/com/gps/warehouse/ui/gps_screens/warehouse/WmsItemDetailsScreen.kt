package com.gps.warehouse.ui.gps_screens.warehouse

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.TopologyDto
import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.ChangeTopologyDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SapBadge
import com.gps.warehouse.utils.ScannerManager
import kotlinx.coroutines.delay

private const val TAG = "WmsItemDetails"

// ====================== Внутреннее состояние редактирования ======================
data class WmsEditState(
    val material: String,
    val position: List<String>,
    val positionId: List<Int>,
    val qty: String,
    val min: String,
    val max: String
) {
    companion object {
        fun fromItem(item: WmsItemDto) = WmsEditState(
            material = item.material,
            position = item.position,
            positionId = item.positionId,
            qty = item.qty.toInt().toString(),
            min = item.min.toString(),
            max = item.max.toString()
        )
    }
}

// ====================== SCREEN ======================
@OptIn(ExperimentalMaterial3Api::class)
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

    // === Режим редактирования ===
    var isEditing by remember { mutableStateOf(false) }
    var editState by remember { mutableStateOf<WmsEditState?>(null) }

    // === Диалоги ===
    var showMoveDialog by remember { mutableStateOf(false) }
    var moveQty by remember { mutableStateOf("") }
    var targetStorage by remember { mutableStateOf("") }
    var showMoveSuccess by remember { mutableStateOf(false) }
    var moveError by remember { mutableStateOf<String?>(null) }

    // === Диалог изменения топологии ===
    var showChangeTopologyDialog by remember { mutableStateOf(false) }
    var showCameraForTopology by remember { mutableStateOf(false) }

    // Временное состояние для диалога выбора топологий
    var dialogSelectedTopologies by remember { mutableStateOf<List<TopologyDto>>(emptyList()) }

    // === Сканер ===
    val scannerManager = remember { ScannerManager(context) }

    // Ищем элемент в ТЕКУЩЕМ состоянии
    val currentItem: WmsItemDto? = remember(uiState, material, storageId) {
        (uiState as? MainViewModel.UiState.WmsLoaded)?.items?.find {
            it.material == material && it.storageId.toString() == storageId
        }
    }

    // Кэш последнего найденного элемента
    var lastFoundItem by remember(material, storageId) { mutableStateOf<WmsItemDto?>(null) }
    LaunchedEffect(currentItem) {
        if (currentItem != null) {
            lastFoundItem = currentItem
        }
    }
    val item: WmsItemDto? = currentItem ?: lastFoundItem

    // === НОВОЕ: Локальное состояние для хранения актуальных топологий,
    // чтобы избежать потери данных при быстром сканировании до обновления ViewModel ===
    var activeItemPositions by remember(item?.material, item?.storageId) {
        mutableStateOf(item?.position ?: emptyList())
    }

    // Синхронизируем с реальным item, когда приходят свежие данные с сервера
    LaunchedEffect(item?.position) {
        item?.position?.let { activeItemPositions = it }
    }


    // Сканирование топологии и склада
//    LaunchedEffect(Unit) {
//        scannerManager.barcodeFlow.collect { scannedData ->
//            if (scannedData.isEmpty()) return@collect
//            val scannedCode = scannedData.trim()
//
//            when {
//                showMoveDialog -> {
//                    Log.d(TAG, "Move scan: $scannedCode")
//                    targetStorage = scannedCode
//                    moveError = null
//                    Toast.makeText(context, "Склад: $scannedCode", Toast.LENGTH_SHORT).show()
//                }
//
//                showChangeTopologyDialog -> {
//                    val matched = topologies.find {
//                        it.positionScan.equals(scannedCode, ignoreCase = true)
//                    }
//                    Log.d(TAG, "Topology scan: $scannedCode, matched=${matched?.position}")
//
//                    if (matched != null) {
//                        if (dialogSelectedTopologies.any { it.id == matched.id }) {
//                            dialogSelectedTopologies = dialogSelectedTopologies.filter { it.id != matched.id }
//                            Toast.makeText(context, "Топология ${matched.position} удалена", Toast.LENGTH_SHORT).show()
//                        } else {
//                            dialogSelectedTopologies = dialogSelectedTopologies + matched
//                            Toast.makeText(context, "Топология ${matched.position} добавлена", Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        Toast.makeText(context, "Позиция '$scannedCode' не найдена на этом складе", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//        }
//    }

    // Сканирование топологии и склада
//    LaunchedEffect(Unit) {
//        scannerManager.barcodeFlow.collect { scannedData ->
//            if (scannedData.isEmpty()) return@collect
//            val scannedCode = scannedData.trim()
//
//            when {
//                showMoveDialog -> {
//                    Log.d(TAG, "Move scan: $scannedCode")
//                    targetStorage = scannedCode
//                    moveError = null
//                    Toast.makeText(context, "Склад: $scannedCode", Toast.LENGTH_SHORT).show()
//                }
//
//                showChangeTopologyDialog -> {
//                    val matched = topologies.find {
//                        it.positionScan.equals(scannedCode, ignoreCase = true)
//                    }
//                    Log.d(TAG, "Topology scan (dialog): $scannedCode, matched=${matched?.position}")
//
//                    if (matched != null) {
//                        if (dialogSelectedTopologies.any { it.id == matched.id }) {
//                            dialogSelectedTopologies = dialogSelectedTopologies.filter { it.id != matched.id }
//                            Toast.makeText(context, "Топология ${matched.position} удалена", Toast.LENGTH_SHORT).show()
//                        } else {
//                            dialogSelectedTopologies = dialogSelectedTopologies + matched
//                            Toast.makeText(context, "Топология ${matched.position} добавлена", Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        Toast.makeText(context, "Позиция '$scannedCode' не найдена на этом складе", Toast.LENGTH_LONG).show()
//                    }
//                }
//
//                else -> {
//                    // Сканирование топологии без открытого диалога (быстрое обновление)
//                    val matched = topologies.find {
//                        it.positionScan.equals(scannedCode, ignoreCase = true)
//                    }
//                    Log.d(TAG, "Topology scan (no dialog): $scannedCode, matched=${matched?.position}")
//
//                    if (matched != null) {
//                        val currentPositions = (if (isEditing) editState?.position else item?.position) ?: emptyList()
//                        val isAlreadyAssigned = currentPositions.contains(matched.position)
//
//                        if (isEditing) {
//                            // Если включен режим редактирования, обновляем локальное состояние editState
//                            val currentIds = editState?.positionId ?: emptyList()
//                            val newPositions = if (isAlreadyAssigned) {
//                                currentPositions.filter { it != matched.position }
//                            } else {
//                                currentPositions + matched.position
//                            }
//                            val newIds = if (isAlreadyAssigned) {
//                                currentIds.filter { it != (matched.id.toIntOrNull() ?: 0) }
//                            } else {
//                                currentIds + (matched.id.toIntOrNull() ?: 0)
//                            }
//
//                            // Логично, что материала без имени быть не может
//                            if (item?.material != null)
//                            editState = editState?.copy(
//                                position = newPositions,
//                                positionId = newIds
//                            ) ?: WmsEditState(
//                                material = item.material,
//                                position = newPositions,
//                                positionId = newIds,
//                                qty = item.qty.toInt().toString(),
//                                min = item.min.toString(),
//                                max = item.max.toString()
//                            )
//
//                            Toast.makeText(context, "Топология ${matched.position} ${if (isAlreadyAssigned) "удалена" else "добавлена"} (нажмите 'Сохранить')", Toast.LENGTH_SHORT).show()
//                        } else {
//                            // Если режим редактирования выключен, сразу отправляем запрос на обновление через API
//                            val currentTopologies = topologies.filter { currentPositions.contains(it.position) }
//                            val newSelectedTopologies = if (isAlreadyAssigned) {
//                                currentTopologies.filter { it.id != matched.id }
//                            } else {
//                                currentTopologies + matched
//                            }
//
//                            if (item != null) {
//                                saveTopologyChange(
//                                    item = item,
//                                    selectedTopologies = newSelectedTopologies,
//                                    context = context,
//                                    mainViewModel = mainViewModel,
//                                    onSuccess = {
//                                        Toast.makeText(
//                                            context,
//                                            "Топология ${matched.position} ${if (isAlreadyAssigned) "удалена" else "добавлена"}",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                    }
//                                )
//                            }
//                        }
//                    } else {
//                        Toast.makeText(context, "Позиция '$scannedCode' не найдена на этом складе", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//        }
//    }

    // Сканирование топологии и склада
    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData ->
            if (scannedData.isEmpty()) return@collect
            val scannedCode = scannedData.trim()

            when {
                showMoveDialog -> {
                    Log.d(TAG, "Move scan: $scannedCode")
                    targetStorage = scannedCode
                    moveError = null
                    Toast.makeText(context, "Склад: $scannedCode", Toast.LENGTH_SHORT).show()
                }

                showChangeTopologyDialog -> {
                    val matched = topologies.find {
                        it.positionScan.equals(scannedCode, ignoreCase = true)
                    }
                    Log.d(TAG, "Topology scan (dialog): $scannedCode, matched=${matched?.position}")

                    if (matched != null) {
                        if (dialogSelectedTopologies.any { it.id == matched.id }) {
                            dialogSelectedTopologies = dialogSelectedTopologies.filter { it.id != matched.id }
                            Toast.makeText(context, "Топология ${matched.position} удалена", Toast.LENGTH_SHORT).show()
                        } else {
                            dialogSelectedTopologies = dialogSelectedTopologies + matched
                            Toast.makeText(context, "Топология ${matched.position} добавлена", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Позиция '$scannedCode' не найдена на этом складе", Toast.LENGTH_LONG).show()
                    }
                }

                else -> {
                    // Сканирование топологии без открытого диалога (быстрое обновление)
                    val matched = topologies.find {
                        it.positionScan.equals(scannedCode, ignoreCase = true)
                    }
                    Log.d(TAG, "Topology scan (no dialog): $scannedCode, matched=${matched?.position}")

                    if (matched != null) {
                        // ИСПОЛЬЗУЕМ activeItemPositions вместо item.position, чтобы не терять данные при быстром сканировании
                        val currentPositions = if (isEditing) {
                            editState?.position ?: emptyList()
                        } else {
                            activeItemPositions
                        }
                        val isAlreadyAssigned = currentPositions.contains(matched.position)

                        if (isEditing) {
                            val currentIds = editState?.positionId ?: emptyList()
                            val newPositions = if (isAlreadyAssigned) {
                                currentPositions.filter { it != matched.position }
                            } else {
                                currentPositions + matched.position
                            }
                            val newIds = if (isAlreadyAssigned) {
                                currentIds.filter { it != (matched.id.toIntOrNull() ?: 0) }
                            } else {
                                currentIds + (matched.id.toIntOrNull() ?: 0)
                            }

                            if (item?.material != null) {
                                editState = editState?.copy(
                                    position = newPositions,
                                    positionId = newIds
                                ) ?: WmsEditState(
                                    material = item.material,
                                    position = newPositions,
                                    positionId = newIds,
                                    qty = item.qty.toInt().toString(),
                                    min = item.min.toString(),
                                    max = item.max.toString()
                                )
                            }
                            Toast.makeText(context, "Топология ${matched.position} ${if (isAlreadyAssigned) "удалена" else "добавлена"} (нажмите 'Сохранить')", Toast.LENGTH_SHORT).show()
                        } else {
                            val currentTopologies = topologies.filter { currentPositions.contains(it.position) }
                            val newSelectedTopologies = if (isAlreadyAssigned) {
                                currentTopologies.filter { it.id != matched.id }
                            } else {
                                currentTopologies + matched
                            }

                            if (item != null) {
                                // МГНОВЕННО обновляем локальное состояние, чтобы следующее сканирование видело актуальный список
                                activeItemPositions = newSelectedTopologies.map { it.position }

                                saveTopologyChange(
                                    item = item,
                                    selectedTopologies = newSelectedTopologies,
                                    context = context,
                                    mainViewModel = mainViewModel,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Топология ${matched.position} ${if (isAlreadyAssigned) "удалена" else "добавлена"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    } else {
                        Toast.makeText(context, "Позиция '$scannedCode' не найдена на этом складе", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Загрузка топологий при открытии экрана
    LaunchedEffect(storageId) {
        scannerManager.init()
        if (storageId.isNotBlank()) {
            mainViewModel.loadTopologies(storageId)
        }
    }

    // Инициализация editState когда item и topologies загружены
    LaunchedEffect(item, topologies) {
        if (item != null && isEditing) {
            editState = WmsEditState.fromItem(item)
        }
    }

    DisposableEffect(Unit) {
        scannerManager.init()
        onDispose { scannerManager.release() }
    }

    // Камера для топологии
    if (showCameraForTopology) {
        CameraScannerDialog(
            onDismiss = { showCameraForTopology = false },
            onBarcodeDetected = { scannedCode ->
                showCameraForTopology = false
                val matched = topologies.find { it.positionScan.equals(scannedCode, ignoreCase = true) }
                if (matched != null) {
                    if (dialogSelectedTopologies.any { it.id == matched.id }) {
                        dialogSelectedTopologies = dialogSelectedTopologies.filter { it.id != matched.id }
                        Toast.makeText(context, "Топология ${matched.position} удалена", Toast.LENGTH_SHORT).show()
                    } else {
                        dialogSelectedTopologies = dialogSelectedTopologies + matched
                        Toast.makeText(context, "Топология ${matched.position} добавлена", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Позиция '$scannedCode' не найдена", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Реакция на результат перемещения
    LaunchedEffect(uiState) {
        if (!showMoveDialog) return@LaunchedEffect
        when (val s = uiState) {
            is MainViewModel.UiState.WmsMoveSuccess -> showMoveSuccess = true
            is MainViewModel.UiState.Error -> moveError = s.message
            else -> Unit
        }
    }

    // Экран ошибки показываем ТОЛЬКО если данных реально нет
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

    // === Контент ===
    WmsItemDetailsContent(
        item = item,
        topologies = topologies,
        isEditing = isEditing,
        editState = editState,
        onEditStateChange = { editState = it },
        onToggleEdit = {
            editState = if (!isEditing) {
                WmsEditState.fromItem(item)
            } else {
                null
            }
            isEditing = !isEditing
        },
        // Переходим назад, только если мы не в редактировании
        onBackClick = {
            if (!isEditing) {
                navController.popBackStack()
            }
            else {
                isEditing = false
            }
        },
        onMoveClick = {
            moveQty = if (item.qty > 0) item.qty.toInt().toString() else ""
            targetStorage = ""
            moveError = null
            showMoveSuccess = false
            showMoveDialog = true
        },
        onSave = {
            val state = editState ?: return@WmsItemDetailsContent
            val minInt = state.min.toIntOrNull() ?: item.min
            val maxInt = state.max.toIntOrNull() ?: item.max
            val qtyInt = state.qty.toIntOrNull() ?: item.qty.toInt()

            // Валидация
            if (state.position.isEmpty()) {
                Toast.makeText(context, "Выберите позицию", Toast.LENGTH_SHORT).show()
                return@WmsItemDetailsContent
            }
            if (minInt < 0 || maxInt < 0 || minInt > maxInt) {
                Toast.makeText(context, "Проверьте значения остатков", Toast.LENGTH_SHORT).show()
                return@WmsItemDetailsContent
            }
            if (item.sapA == 0 && (qtyInt < 0 || state.qty.isBlank())) {
                Toast.makeText(context, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                return@WmsItemDetailsContent
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
                    isEditing = false
                    editState = null
                    mainViewModel.loadWmsData()
                    Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "Ошибка: $error", Toast.LENGTH_SHORT).show()
                }
            )
        },
        onCancelEdit = {
            isEditing = false
            editState = null
        },
        onChangeTopologyClick = {
            // Берем самые свежие данные: либо из режима редактирования, либо из нашего "живого" кэша
            val currentPositions = if (isEditing) {
                editState?.position ?: emptyList()
            } else {
                activeItemPositions
            }

            dialogSelectedTopologies = topologies.filter { topo ->
                currentPositions.contains(topo.position)
            }
            showChangeTopologyDialog = true
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

    // === ДИАЛОГ ИЗМЕНЕНИЯ ТОПОЛОГИИ ===
    if (showChangeTopologyDialog) {
        ChangeTopologyDialog(
            selectedTopologies = dialogSelectedTopologies,
            allTopologies = topologies,
            currentMaterial = item.material,
            onDismiss = { showChangeTopologyDialog = false },
            onToggleTopology = { topology ->
                dialogSelectedTopologies = if (dialogSelectedTopologies.any { it.id == topology.id }) {
                    dialogSelectedTopologies.filter { it.id != topology.id }
                } else {
                    dialogSelectedTopologies + topology
                }
            },
            onConfirm = {
                if (isEditing) {
                    editState = editState?.copy(
                        position = dialogSelectedTopologies.map { it.position },
                        positionId = dialogSelectedTopologies.map { it.id.toInt() }
                    )
                    showChangeTopologyDialog = false
                    Toast.makeText(context, "Топологии обновлены. Нажмите 'Сохранить'", Toast.LENGTH_SHORT).show()
                } else {
                    saveTopologyChange(
                        item = item,
                        selectedTopologies = dialogSelectedTopologies,
                        context = context,
                        mainViewModel = mainViewModel,
                        onSuccess = {
                            showChangeTopologyDialog = false
                            Toast.makeText(context, "Топологии сохранены", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            onCameraScanClick = { showCameraForTopology = true }
        )
    }
}

/**
 * Автосохранение топологии без подтверждения.
 * Сохраняет только позицию (position + positionId), остальные поля берёт из текущего item.
 */
private fun saveTopologyChange(
    item: WmsItemDto,
    selectedTopologies: List<TopologyDto>,
    context: android.content.Context,
    mainViewModel: MainViewModel,
    onSuccess: () -> Unit
) {
    mainViewModel.updateWmsItem(
        item = item,
        newPosition = selectedTopologies.map { it.position },
        newPositionId = selectedTopologies.map { it.id.toInt() },
        newMin = item.min,
        newMax = item.max,
        newMaterial = null,
        newQty = null,
        onSuccess = {
            mainViewModel.loadWmsData()
            onSuccess()
        },
        onError = { error ->
            Toast.makeText(context, "Ошибка сохранения: $error", Toast.LENGTH_SHORT).show()
        }
    )
}

// ====================== CONTENT ======================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmsItemDetailsContent(
    item: WmsItemDto,
    topologies: List<TopologyDto>,
    isEditing: Boolean,
    editState: WmsEditState?,
    onEditStateChange: (WmsEditState) -> Unit,
    onToggleEdit: () -> Unit,
    onBackClick: () -> Unit,
    onMoveClick: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onChangeTopologyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // === ActionBar ===
        MyCustomActionBar(
            text = item.material,
            onBackClick = onBackClick,
            actionButton = {
                if (isEditing) {
                    Row {
                        IconButton(onClick = onSave) {
                            Icon(
                                Icons.Default.Save,
                                "Сохранить",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onCancelEdit) {
                            Icon(
                                Icons.Default.Close,
                                "Отмена",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onToggleEdit) {
                        Icon(
                            Icons.Default.Edit,
                            "Редактировать",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        // === Основной контент ===
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Хедер-карточка: материал + остаток
            item {
                WmsHeaderCard(
                    item = item,
                    isEditing = isEditing,
                    editState = editState,
                    onEditStateChange = onEditStateChange
                )
            }

            // Сетка: Местоположение + Лимиты
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WmsLocationCard(
                        item = item,
                        allTopologies = topologies,
                        isEditing = isEditing,
                        editState = editState,
                        onEditStateChange = onEditStateChange,
                        onChangeTopologyClick = onChangeTopologyClick,
                        modifier = Modifier.weight(1f)
                    )
                    WmsLimitsCard(
                        item = item,
                        isEditing = isEditing,
                        editState = editState,
                        onEditStateChange = onEditStateChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // === Кнопки действий внизу (только если НЕ в режиме редактирования) ===
        if (!isEditing) {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onChangeTopologyClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Изменить топологию")
                    }

                    Button(
                        onClick = onMoveClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Переместить материал")
                    }
                }
            }
        }
    }
}

// ====================== ХЕДЕР-КАРТОЧКА ======================
@Composable
private fun WmsHeaderCard(
    item: WmsItemDto,
    isEditing: Boolean,
    editState: WmsEditState?,
    onEditStateChange: (WmsEditState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Верхняя строка: название + бейджи
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                if (item.sapA == 1) SapBadge()
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing && item.sapA == 0) {
                // Режим редактирования: количество можно менять
                OutlinedTextField(
                    value = editState?.qty ?: item.qty.toInt().toString(),
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            editState?.let { state ->
                                onEditStateChange(state.copy(qty = value))
                            }
                        }
                    },
                    label = { Text("Количество (шт.)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
            } else {
                // Обычный режим: остаток крупно + цена справа
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Остаток",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${item.qty.toInt()}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "шт.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Цена",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${item.price}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ====================== КАРТОЧКА МЕСТОПОЛОЖЕНИЯ ======================
@Composable
private fun WmsLocationCard(
    item: WmsItemDto,
    allTopologies: List<TopologyDto>, // <-- Новый параметр
    isEditing: Boolean,
    editState: WmsEditState?,
    onEditStateChange: (WmsEditState) -> Unit,
    onChangeTopologyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Местоположение", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))

            CompactInfoRow(icon = Icons.Outlined.Warehouse, label = "Склад", value = item.storage.ifBlank { "—" })
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Place, "Топологии", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Топологии",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!isEditing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = if (isEditing) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (isEditing) {
                // === РЕЖИМ РЕДАКТИРОВАНИЯ: Список с чекбоксами ===
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp) // Ограничиваем высоту, чтобы не занимать весь экран
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    if (allTopologies.isEmpty()) {
                        Text("Нет доступных позиций", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        allTopologies.forEach { topology ->
                            val isSelected = editState?.position?.contains(topology.position) == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentPositions = editState?.position ?: emptyList()
                                        val currentIds = editState?.positionId ?: emptyList()

                                        val newPositions = if (isSelected) {
                                            currentPositions - topology.position
                                        } else {
                                            currentPositions + topology.position
                                        }

                                        val newIds = if (isSelected) {
                                            currentIds - (topology.id.toIntOrNull() ?: 0)
                                        } else {
                                            currentIds + (topology.id.toIntOrNull() ?: 0)
                                        }

                                        onEditStateChange(
                                            (editState ?: WmsEditState.fromItem(item)).copy(
                                                position = newPositions,
                                                positionId = newIds
                                            )
                                        )
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null // Обработчик находится на Row
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = topology.position,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                // Кнопка для быстрого добавления через сканер прямо из режима редактирования
                TextButton(
                    onClick = onChangeTopologyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Сканировать")
                }

            } else {
                // === ОБЫЧНЫЙ РЕЖИМ: Чипсы (FlowRow) ===
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val positionsToShow = item.position
                    if (positionsToShow.isEmpty()) {
                        Text("Не выбрано", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        positionsToShow.forEach { pos ->
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    text = pos,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ====================== КАРТОЧКА ЛИМИТОВ ======================
@Composable
private fun WmsLimitsCard(
    item: WmsItemDto,
    isEditing: Boolean,
    editState: WmsEditState?,
    onEditStateChange: (WmsEditState) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Tune,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Лимиты",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(10.dp))

            if (isEditing) {
                // Режим редактирования: поля ввода
                val minInt = editState?.min?.toIntOrNull()
                val maxInt = editState?.max?.toIntOrNull()
                val isError = minInt != null && maxInt != null && maxInt < minInt

                OutlinedTextField(
                    value = editState?.min ?: item.min.toString(),
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            editState?.let { state ->
                                onEditStateChange(state.copy(min = value))
                            }
                        }
                    },
                    label = { Text("Мин.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = editState?.max ?: item.max.toString(),
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            editState?.let { state ->
                                onEditStateChange(state.copy(max = value))
                            }
                        }
                    },
                    label = { Text("Макс.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        {
                            Text(
                                "Макс. должен быть ≥ мин.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else null
                )
            } else {
                // Обычный режим: бейджи
                LimitValueRow(
                    label = "Мин.",
                    value = item.min.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(Modifier.height(8.dp))

                LimitValueRow(
                    label = "Макс.",
                    value = item.max.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ====================== КАРТОЧКА ДЕТАЛЕЙ ======================
@Composable
private fun WmsDetailsCard(item: WmsItemDto) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Детали",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(10.dp))

            CompactInfoRow(
                icon = Icons.Outlined.QrCode2,
                label = "Артикул",
                value = item.material
            )

            Spacer(Modifier.height(8.dp))

            if (item.positionId.isNotEmpty()) {
                CompactInfoRow(
                    icon = Icons.Outlined.PinDrop,
                    label = "ID позиции",
                    value = item.positionId.toString()
                )
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = "ID записи: ${item.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ====================== ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ======================
@Composable
private fun CompactInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                label,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun LimitValueRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Локальное TextFieldValue для управления курсором
    var storageField by remember { mutableStateOf(TextFieldValue(targetStorage)) }

    LaunchedEffect(targetStorage) {
        if (storageField.text != targetStorage) {
            storageField = TextFieldValue(
                text = targetStorage,
                selection = TextRange(targetStorage.length)
            )
        }
    }

    // Автофокус на поле «Целевой склад» при открытии диалога
    LaunchedEffect(Unit) {
        // Небольшая задержка, чтобы диалог успел отрисоваться
        delay(200)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

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
                if (isSuccess) "Успешно!" else "Перемещение",
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
                    ItemInfoCard(itemToMove)

                    // Поле целевого склада
                    OutlinedTextField(
                        value = storageField,
                        onValueChange = { newValue ->
                            storageField = newValue
                            onTargetStorageChange(newValue.text)
                        },
                        label = { Text("Целевой склад") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = targetStorage.isEmpty() && !isLoading,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

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
                if (item.sapA == 1) { SapBadge() }
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
                Icon(
                    Icons.Outlined.Warehouse,
                    "Склад",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.storage,
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
                Spacer(modifier = Modifier.weight(1f))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        "Позиция",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    val positionsToShow = item.position

                    if (positionsToShow.isEmpty()) {
                        Text("Не выбрано", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        positionsToShow.forEach { pos ->
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    text = pos,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
@Preview(showBackground = true, showSystemUi = true, name = "Режим просмотра", device = "spec:width=350dp,height=870dp")
@Composable
fun WmsItemDetailsContentPreview() {
    MaterialTheme {
        Surface {
            WmsItemDetailsContent(
                item = WmsItemDto(
                    id = 1,
                    name = "Сигнализационная лампа",
                    material = "LA0602600443",
                    max = 111,
                    min = 10,
                    positionId = emptyList(),
                    position = listOf("BUFF", "A-01", "A-02"),
                    price = 150.5,
                    qty = 42.0,
                    sapA = 1,
                    storage = "3051",
                    storageId = 1
                ),
                topologies = listOf(
                    TopologyDto("0", "BUFF", "BUFF"),
                    TopologyDto("1", "A-1", "A1"),
                    TopologyDto("2", "A-2", "A2"),
                ),
                isEditing = false,
                editState = WmsEditState.fromItem(
                    WmsItemDto(
                        id = 1, name = "Сигнализационная лампа", material = "LA0602600443",
                        max = 111, min = 10, positionId = listOf(1), position = listOf("BUFF"),
                        price = 150.5, qty = 42.0, sapA = 1, storage = "3051", storageId = 1
                    )
                ),
                onEditStateChange = {},
                onToggleEdit = {},
                onBackClick = {},
                onMoveClick = {},
                onSave = {},
                onCancelEdit = {},
                onChangeTopologyClick = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Режим редактирования")
@Composable
fun WmsItemDetailsContentPreview_Editing() {
    MaterialTheme {
        Surface {
            WmsItemDetailsContent(
                item = WmsItemDto(
                    id = 1,
                    name = "Сигнализационная лампа",
                    material = "LA0602600443",
                    max = 111,
                    min = 10,
                    positionId = listOf(1),
                    position = listOf("BUFF", "A-01", "A-02"),
                    price = 150.5,
                    qty = 42.0,
                    sapA = 0,
                    storage = "3051",
                    storageId = 1
                ),
                topologies = listOf(
                    TopologyDto("0", "BUFF", "BUFF"),
                    TopologyDto("1", "A-1", "A1"),
                    TopologyDto("2", "A-2", "A2"),
                ),
                isEditing = true,
                editState = WmsEditState(
                    material = "LA0602600443",
                    position = listOf("BUFF", "A-01", "A-02"),
                    positionId = listOf(0, 1, 2),
                    qty = "42",
                    min = "10",
                    max = "111"
                ),
                onEditStateChange = {},
                onToggleEdit = {},
                onBackClick = {},
                onMoveClick = {},
                onSave = {},
                onCancelEdit = {},
                onChangeTopologyClick= {}
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
                    positionId = emptyList(), position = listOf("position"), price = 211.0,
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