package com.gps.warehouse.ui.gps_screens.warehouse

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.TopologyDto
import com.gps.warehouse.data.remote.gps_dto.WarehousePermissionDto
import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SapBadge
import com.gps.warehouse.ui.components.SearchAndFilterBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannedData
import com.gps.warehouse.utils.ScannerManager

// ====================== ЭКРАН: ЛОГИКА ======================
@Composable
fun WmsScreen(
    navController: NavHostController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val availableWarehouses by viewModel.availableWarehouses.collectAsState()

    // === Пагинация и фильтры ===
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMoreWms.collectAsState()

    // === Состояния для редактирования ===
    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<WmsItemDto?>(null) }
    var editMaterial by remember { mutableStateOf("") }
    var editPosition by remember { mutableStateOf("") }
    var editPositionId by remember { mutableStateOf("") }
    var editQty by remember { mutableStateOf("") }
    var editMax by remember { mutableStateOf("") }
    var editMin by remember { mutableStateOf("") }

    // === Топологии ===
    val topologies by viewModel.topologies.collectAsState()
    var isLoadingTopologies by remember { mutableStateOf(false) }

    // === Флаги для цепочки "редактировать → переместить" ===
    var moveAfterEdit by remember { mutableStateOf(false) }
    var pendingMoveTarget by remember { mutableStateOf("") }
    var pendingMoveQty by remember { mutableStateOf("1") }

    // === Основные состояния экрана ===
    var searchQuery by remember { mutableStateOf("") }
    var selectedStorageFilterId by remember { mutableStateOf<String?>(null) }
    var showOnlyNonZeroQty by remember { mutableStateOf(false) }
    var isFiltersExpanded by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<WmsItemDto?>(null) }
    var moveQty by remember { mutableStateOf("1") }
    var targetStorage by remember { mutableStateOf("") }
    var showDialogSuccess by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    val honeywellHelper = remember { ScannerManager(context) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }

    // Инициализация
    LaunchedEffect(Unit) {
        viewModel.loadWmsData()
        viewModel.loadAvailableWarehouses()
        honeywellHelper.init()
    }

    // Обработка сканирования
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val parsedData: ScannedData? = BarcodeParser.parse(scannedData)
                val materialCode = parsedData?.material ?: scannedData.trim()
                lastScannedCode = materialCode
                viewModel.searchWmsByMaterial(materialCode)
            }
        }
    }

    // Обработка состояния UI
    LaunchedEffect(uiState) {
        if (lastScannedCode != null && uiState is MainViewModel.UiState.WmsLoaded) {
            val foundItem = (uiState as MainViewModel.UiState.WmsLoaded).items.find {
                it.material.equals(lastScannedCode, ignoreCase = true)
            }
            if (foundItem != null) {
                itemToMove = foundItem
                searchQuery = lastScannedCode.orEmpty()
                moveQty = if (foundItem.qty > 0) foundItem.qty.toInt().toString() else ""
                targetStorage = ""
                dialogError = null
                showDialogSuccess = false
                showMoveDialog = true
            } else {
                Toast.makeText(context, "Материал $lastScannedCode не найден на складе", Toast.LENGTH_LONG).show()
            }
            lastScannedCode = null
        }

        when (uiState) {
            is MainViewModel.UiState.WmsMoveSuccess -> {
                if (showMoveDialog) showDialogSuccess = true
            }
            is MainViewModel.UiState.Error -> {
                if (showMoveDialog) dialogError = (uiState as MainViewModel.UiState.Error).message
            }
            is MainViewModel.UiState.Loading -> {
                if (showMoveDialog && !showDialogSuccess) dialogError = null
            }
            else -> {}
        }
    }

    // Фильтры
    LaunchedEffect(searchQuery, selectedStorageFilterId, showOnlyNonZeroQty) {
        viewModel.updateWmsFilters(
            storageId = selectedStorageFilterId,
            searchQuery = searchQuery,
            hideZeroQty = showOnlyNonZeroQty)
    }

    DisposableEffect(Unit) {
        onDispose {
            honeywellHelper.release()
        }
    }

    // Основной контент
    WmsContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStorageFilterId = selectedStorageFilterId,
        onStorageFilterSelected = { selectedStorageFilterId = it },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        onBackClick = { navController.popBackStack() },
        onNavigateToRequests = { navController.navigate("wms_requests") },
        onRetryClick = { viewModel.loadWmsData() },
        onItemClick = { item ->
            itemToMove = item
            moveQty = ""
            targetStorage = ""
            dialogError = null
            showDialogSuccess = false
            showMoveDialog = true
        },
        showMoveDialog = showMoveDialog,
        showOnlyNonZeroQty = showOnlyNonZeroQty,
        onShowOnlyNonZeroQtyChange = { showOnlyNonZeroQty = it },
        onResetFilters = {
            searchQuery = ""
            selectedStorageFilterId = null
            showOnlyNonZeroQty = false
            viewModel.updateWmsFilters(null, "", false)
        },
        availableWarehouses = availableWarehouses,
        isLoadingMore = isLoadingMore,
        hasMore = hasMore,
        onLoadMore = { viewModel.loadMoreWmsData() },
    )

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ===
    if (showEditDialog && itemToEdit != null) {
        EditWmsItemDialog(
            item = itemToEdit!!,
            currentStorageId = selectedStorageFilterId ?: itemToEdit!!.storageId.toString(),
            topologies = topologies,
            isLoadingTopologies = isLoadingTopologies,
            initialPosition = editPosition,
            initialPositionId = editPositionId,
            initialMax = editMax,
            initialMin = editMin,
            initialMaterial = editMaterial,
            initialQty = editQty,
            onPositionSelected = { position, positionId ->
                editPosition = position
                editPositionId = positionId
            },
            onMaxChange = { editMax = it },
            onMinChange = { editMin = it },
            onMaterialChange = { editMaterial = it },
            onQtyChange = { editQty = it },
            onDismiss = {
                showEditDialog = false
                itemToEdit = null
            },
            onSave = {
                // Преобразуем строки в числа с защитой от null
                val minInt = editMin.toIntOrNull() ?: itemToEdit!!.min
                val maxInt = editMax.toIntOrNull() ?: itemToEdit!!.max
                val qtyInt = editQty.toIntOrNull() ?: itemToEdit!!.qty.toInt()  // Для non-SAP


                // Валидация
                if (editPosition.isBlank() || editPositionId.isBlank()) {
                    Toast.makeText(context, "Выберите позицию", Toast.LENGTH_SHORT).show()
                    return@EditWmsItemDialog
                }
                // Валидация мин./макс. остатка
                if (minInt < 0 || maxInt < 0 || minInt > maxInt) {
                    Toast.makeText(context, "Проверьте значения остатков", Toast.LENGTH_SHORT).show()
                    return@EditWmsItemDialog
                }
                // Валидация количества (только для non-SAP)
                if (itemToEdit!!.sapA == 0 && (qtyInt < 0 || editQty.isBlank())) {
                    Toast.makeText(context, "Введите корректное количество", Toast.LENGTH_SHORT).show()
                    return@EditWmsItemDialog
                }

                viewModel.updateWmsItem(
                    item = itemToEdit!!,
                    newPosition = editPosition,
                    newPositionId = editPositionId,
                    newMin = minInt,
                    newMax = maxInt,
                    // Передаём новые значения только если sapA == 0
                    newMaterial = if (itemToEdit!!.sapA == 0) editMaterial else null,
                    newQty = if (itemToEdit!!.sapA == 0) qtyInt else null,
                    onSuccess = {
                        if (moveAfterEdit) {
                            val qty = pendingMoveQty.toIntOrNull() ?: 1
                            viewModel.moveWmsMaterial(
                                material = itemToEdit!!.material,
                                fromStorage = itemToEdit!!.storage,
                                toStorage = pendingMoveTarget,
                                qty = qty
                            )
                            moveAfterEdit = false
                        }
                        showEditDialog = false
                        itemToEdit = null
                        viewModel.loadWmsData()
                    },
                    onError = { error ->
                        Toast.makeText(context, "Ошибка: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onLoadTopologies = { storageId ->
                isLoadingTopologies = true
                viewModel.loadTopologies(storageId)
                isLoadingTopologies = false
            }
        )
    }

    // === ДИАЛОГ ПЕРЕМЕЩЕНИЯ ===
    if (showMoveDialog && itemToMove != null) {
        MoveMaterialDialog(
            itemToMove = itemToMove!!,
            moveQty = moveQty,
            targetStorage = targetStorage,
            isLoading = uiState is MainViewModel.UiState.Loading && !showDialogSuccess,
            isSuccess = showDialogSuccess,
            errorMessage = dialogError,
            onEditClick = { item ->
                itemToEdit = item
                editMaterial = item.material
                editPosition = item.position
                editPositionId = item.positionId.toString()
                editQty = item.qty.toInt().toString()
                editMax = item.max.toString()
                editMin = item.min.toString()
                isLoadingTopologies = true
                viewModel.loadTopologies(item.storageId.toString())
                isLoadingTopologies = false
                showEditDialog = true
                showMoveDialog = false
            },
            onDismissRequest = {
                if (!showDialogSuccess && uiState !is MainViewModel.UiState.Loading) {
                    showMoveDialog = false
                    itemToMove = null
                    dialogError = null
                }
            },
            onQtyChange = { moveQty = it },
            onTargetStorageChange = { targetStorage = it },
            onClearError = { dialogError = null },
            onConfirmMove = { item, qtyStr, toStorage ->
                val qty = qtyStr.toDoubleOrNull()?.toInt() ?: 0
                if (qty > 0 && toStorage.isNotEmpty()) {
                    if (showEditDialog && itemToEdit?.id == item.id) {
                        moveAfterEdit = true
                        pendingMoveTarget = toStorage
                        pendingMoveQty = qtyStr
                    } else {
                        dialogError = null
                        showDialogSuccess = false
                        viewModel.moveWmsMaterial(item.material, item.storage, toStorage, qty)
                    }
                }
            },
            onSuccessAcknowledge = {
                showMoveDialog = false
                showDialogSuccess = false
                itemToMove = null
                moveQty = "1"
                targetStorage = ""
                Toast.makeText(context, "Перемещение успешно", Toast.LENGTH_SHORT).show()
                viewModel.loadWmsData()
            }
        )
    }
}

// ====================== CONTENT: ЧИСТЫЙ UI ======================
@Composable
fun WmsContent(
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStorageFilterId: String?,
    onStorageFilterSelected: (String?) -> Unit,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onRetryClick: () -> Unit,
    onItemClick: (WmsItemDto) -> Unit,
    showMoveDialog: Boolean,
    showOnlyNonZeroQty: Boolean,
    onShowOnlyNonZeroQtyChange: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    availableWarehouses: List<WarehousePermissionDto>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Склады",
            actionButton = {
                IconButton(onClick = onNavigateToRequests) {
                    Icon(
                        Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = "Запросы",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isFiltersExpanded = isFiltersExpanded,
            onToggleFilters = onToggleFilters,
            hasActiveFilters = selectedStorageFilterId != null || showOnlyNonZeroQty
        ) {
            Text("Склад: ", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedStorageFilterId == null,
                    onClick = { onStorageFilterSelected(null) },
                    label = { Text("Все") }
                )
                availableWarehouses.forEach { storageId ->
                    FilterChip(
                        selected = selectedStorageFilterId == storageId.id,
                        onClick = { onStorageFilterSelected(storageId.id) },
                        label = { Text(storageId.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Только с остатком", style = MaterialTheme.typography.bodyMedium)
                Checkbox(
                    checked = showOnlyNonZeroQty,
                    onCheckedChange = onShowOnlyNonZeroQtyChange
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onResetFilters,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Сбросить фильтры")
                }
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> {
                if (!showMoveDialog) CustomLoadingView()
            }

            is MainViewModel.UiState.WmsLoaded -> {
                val allItems = uiState.items
                val lazyListState = rememberLazyListState()

                // Локальные флаги, синхронизированные с реальной загрузкой
                var lastLoadedCount by remember { mutableIntStateOf(0) }
                var localLoading by remember { mutableStateOf(false) }

                // Актуальные значения, читаемые из snapshotFlow
                val currentHasMore by rememberUpdatedState(hasMore)
                val currentIsLoading by rememberUpdatedState(isLoadingMore)
                val currentItemsCount by rememberUpdatedState(allItems.size)

                // Сброс localLoading: либо пришло больше элементов, либо VM закончил загрузку
                LaunchedEffect(allItems.size, isLoadingMore) {
                    if (allItems.size != lastLoadedCount) {
                        Log.d(
                            "WMS_PAGINATION_UI",
                            "Items changed: $lastLoadedCount -> ${allItems.size}. Reset localLoading=false"
                        )
                        lastLoadedCount = allItems.size
                        localLoading = false
                    }
                    if (!isLoadingMore && localLoading) {
                        Log.d("WMS_PAGINATION_UI", "VM isLoadingMore=false, reset localLoading")
                        localLoading = false
                    }
                }

                // Триггер пагинации. БЕЗ distinctUntilChanged, БЕЗ uiState в ключах.
                LaunchedEffect(lazyListState) {
                    snapshotFlow {
                        val layoutInfo = lazyListState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@snapshotFlow false
                        val lastVisibleIndex = visibleItems.last().index
                        val totalItems = layoutInfo.totalItemsCount
                        lastVisibleIndex >= totalItems - 5
                    }.collect { isNearEnd ->
                        Log.d(
                            "WMS_PAGINATION_UI",
                            "isNearEnd=$isNearEnd | localLoading=$localLoading | " +
                                    "vmLoading=$currentIsLoading | hasMore=$currentHasMore | " +
                                    "items=$currentItemsCount"
                        )
                        if (isNearEnd && !localLoading && !currentIsLoading && currentHasMore) {
                            Log.d("WMS_PAGINATION_UI", "TRIGGERING onLoadMore!")
                            localLoading = true
                            onLoadMore()
                        } else if (isNearEnd) {
                            Log.d(
                                "WMS_PAGINATION_UI",
                                "isNearEnd but ignored (localLoading=$localLoading, " +
                                        "vmLoading=$currentIsLoading, hasMore=$currentHasMore)"
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Загружено: ${allItems.size} материалов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = lazyListState
                    ) {
                        items(allItems) { item ->
                            WmsItemCard(item = item, onClick = { onItemClick(item) })
                        }

                        // Индикатор загрузки внизу
                        if (isLoadingMore || localLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        // Сообщение "всё загружено" — только когда реально нет следующей страницы
                        if (!hasMore && allItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "• Все материалы загружены •",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            is MainViewModel.UiState.Error -> {
                if (!showMoveDialog) {
                    ErrorStateView(
                        message = uiState.message,
                        onRetry = onRetryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {}
        }
    }
}

// ====================== КАРТОЧКА МАТЕРИАЛА ======================
@Composable
fun WmsItemCard(item: WmsItemDto, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // === Верхняя строка: Артикул + Количество + SAP ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.material,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Бейдж количества
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${item.qty.toInt()} шт.",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // SAP-индикатор
                    if (item.sapA == 1) {
                        SapBadge()
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // === Название материала ===
            Text(
                text = item.name.ifBlank { "Без названия" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // === Мета-информация: Склад + Позиция ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Склад
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Warehouse,
                        "Склад",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.storage.ifBlank { "—" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Позиция
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        "Позиция",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = item.position.ifBlank { "—" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
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
    initialPositionId: String,
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

    // Локальные состояния
    var materialField by remember { mutableStateOf(initialMaterial) }
    var positionField by remember { mutableStateOf(initialPosition) }
    var positionIdField by remember { mutableStateOf(initialPositionId) }
    var qtyField by remember { mutableStateOf(initialQty) }
    var maxField by remember { mutableStateOf(initialMax) }
    var minField by remember { mutableStateOf(initialMin) }

    var expanded by remember { mutableStateOf(false) }

    // Загрузка топологий
    LaunchedEffect(Unit) {
        if (topologies.isEmpty() && currentStorageId.isNotBlank()) {
            onLoadTopologies(currentStorageId)
        }
    }

    // Синхронизация с внешними изменениями
    LaunchedEffect(initialMaterial) { materialField = initialMaterial }
    LaunchedEffect(initialPosition) { positionField = initialPosition }
    LaunchedEffect(initialPositionId) { positionIdField = initialPositionId }
    LaunchedEffect(initialQty) { qtyField = initialQty }
    LaunchedEffect(initialMax) { maxField = initialMax }
    LaunchedEffect(initialMin) { minField = initialMin }

    // Ключевое: imePadding + fixed height предотвращают "прыжки"
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(), // Учитывает клавиатуру
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
                // === Группа 1: Основные поля ===
                if (item.sapA == 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Артикул - показываем только если это виртуальный материал
                            OutlinedTextField(
                                value = materialField,
                                onValueChange = { newValue ->
                                    materialField = newValue
                                    onMaterialChange(newValue)
                                },
                                label = { Text("Артикул") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = (item.sapA == 1),
                                enabled = (item.sapA == 0),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                isError = item.sapA == 0 && materialField.isBlank(),
                                supportingText = {
                                    if (item.sapA == 0 && materialField.isBlank()) {
                                        Text(
                                            "Обязательно", color = MaterialTheme.colorScheme.error
                                        )
                                    } else if (item.sapA == 1) {
                                        Text(
                                            "Не редактируется",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            )

                            // Количество (только для non-SAP)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = qtyField,
                                onValueChange = { newValue ->
                                    if (newValue.all { c -> c.isDigit() }) {
                                        qtyField = newValue
                                        onQtyChange(newValue)
                                    }
                                },
                                label = { Text("Количество") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // === Группа 2: Позиция (топология) ===
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Позиция (топология)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
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
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
                                                onPositionSelected(topology.position, topology.id.toString())
                                                positionField = topology.position
                                                positionIdField = topology.id.toString()
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                if (topology.id.toString() == positionIdField) {
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

                // === Группа 3: Остатки ===
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Лимиты остатков", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Мин. остаток
                            OutlinedTextField(
                                value = minField,
                                onValueChange = { newValue ->
                                    if (newValue.all { c -> c.isDigit() }) {
                                        minField = newValue
                                        onMinChange(newValue)
                                    }
                                },
                                label = { Text("Мин.") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            // Макс. остаток
                            OutlinedTextField(
                                value = maxField,
                                onValueChange = { newValue ->
                                    if (newValue.all { c -> c.isDigit() }) {
                                        maxField = newValue
                                        onMaxChange(newValue)
                                    }
                                },
                                label = { Text("Макс.") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                isError = maxField.toIntOrNull() != null &&
                                        minField.toIntOrNull() != null &&
                                        maxField.toInt() < minField.toInt()
                            )
                        }
                        if (maxField.toIntOrNull() != null && minField.toIntOrNull() != null && maxField.toInt() < minField.toInt()) {
                            Text("Макс. не может быть меньше мин.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus() // Скрываем клавиатуру перед сохранением
                    onSave()
                },
                modifier = Modifier
                    .height(48.dp),
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
                modifier = Modifier
                    .height(48.dp)
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
    onEditClick: (WmsItemDto) -> Unit,
    onDismissRequest: () -> Unit,
    onQtyChange: (String) -> Unit,
    onTargetStorageChange: (String) -> Unit,
    onClearError: () -> Unit,
    onConfirmMove: (WmsItemDto, String, String) -> Unit,
    onSuccessAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = if (isLoading || isSuccess) { {} } else { onDismissRequest },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        ),
        icon = {
            if (isSuccess) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Со склада:", style = MaterialTheme.typography.labelSmall)
                                Text(itemToMove.storage, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("На склад:", style = MaterialTheme.typography.labelSmall)
                                Text(targetStorage, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Количество:", style = MaterialTheme.typography.labelSmall)
                                Text(moveQty, fontWeight = FontWeight.Bold)
                            }
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
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = itemToMove.material,
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
                                        text = itemToMove.position,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (itemToMove.sapA == 1) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF667eea), // Фиолетовый градиент-стиль
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
                                text = itemToMove.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Склад: ${itemToMove.storage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${itemToMove.qty.toInt()} шт.",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetStorage,
                        onValueChange = onTargetStorageChange,
                        label = { Text("Целевой склад") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = targetStorage.isEmpty() && !isLoading
                    )

                    OutlinedTextField(
                        value = moveQty,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                val inputQty = newValue.toIntOrNull() ?: 0
                                val maxQty = itemToMove.qty.toInt()
                                if (inputQty <= maxQty || newValue.isEmpty()) onQtyChange(newValue)
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
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onClearError) {
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

                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выполняется перемещение...")
                        }
                    }

                    // Кнопка "Редактировать"
                    if (!isLoading) {
                        TextButton(
                            onClick = { onEditClick(itemToMove) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Редактировать материал")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(
                    onClick = onSuccessAcknowledge,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("ОК")
                }
            } else {
                Button(
                    onClick = { onConfirmMove(itemToMove, moveQty, targetStorage) },
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

// ====================== ПРЕВЬЮ ======================
@Preview(showBackground = true, name = "WMS Screen - Loaded")
@Composable
fun WmsPreviewLoaded() {
    val fakeItems = listOf(
        WmsItemDto(
            id = 66,
            material = "LA0602600443",
            max = 2,
            min = 0,
            positionId = 0,
            position = "BUFF",
            price = 150.0,
            qty = 4.0,
            sapA = 0,
            storage = "3051",
            storageId = 1,
            name = "СИГНАЛИЗАЦИОННАЯ ЛАМПА"
        ),
        WmsItemDto(
            id = 67,
            material = "LA0713000190",
            max = 5,
            min = 1,
            positionId = 0,
            position = "A-01",
            price = 1200.0,
            qty = 10.0,
            sapA = 1,
            storage = "4007",
            storageId = 2,
            name = "Станция зарядки"
        )
    )
    MaterialTheme {
        Surface {
            WmsContent(
                uiState = MainViewModel.UiState.WmsLoaded(fakeItems),
                searchQuery = "",
                onSearchQueryChange = {},
                selectedStorageFilterId = null,
                onStorageFilterSelected = {},
                isFiltersExpanded = false,
                onToggleFilters = {},
                onBackClick = {},
                onNavigateToRequests = {},
                onRetryClick = {},
                onItemClick = {},
                showMoveDialog = false,
                showOnlyNonZeroQty = true,
                onShowOnlyNonZeroQtyChange = {},
                onResetFilters = {},
                availableWarehouses = listOf(
                    WarehousePermissionDto(id = "1", name = "3051", isLeader = "1", isVirtual = "0"),
                    WarehousePermissionDto(id = "2", name = "4007", isLeader = "0", isVirtual = "0")
                ),
                isLoadingMore = true,
                hasMore = true,
                onLoadMore = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "WMS Screen - MoveDialog")
@Composable
fun MoveDialogPreview() {
    MoveMaterialDialog(
        itemToMove = WmsItemDto(
            id = 1,
            material = "material",
            max = 100,
            min = 1,
            positionId = 0,
            position = "position",
            price = 211.0,
            qty = 222.0,
            sapA = 1,
            storage = "storage",
            storageId = 3,
            name = "name"
        ),
        moveQty = "10",
        targetStorage = "3051",
        isLoading = false,
        isSuccess = false,
        errorMessage = "",
        onEditClick = {},
        onDismissRequest = {},
        onQtyChange = {},
        onTargetStorageChange = {},
        onClearError = {},
        onConfirmMove = {_, _, _ -> {}},
        onSuccessAcknowledge = {}
    )
}

@Preview(showBackground = true, name = "WMS Screen - EditDialog")
@Composable
fun EditDialogPreview() {
    EditWmsItemDialog(
        item = WmsItemDto(
            id = 1,
            material = "material",
            max = 100,
            min = 1,
            positionId = 0,
            position = "BUFF",
            price = 211.0,
            qty = 222.0,
            sapA = 0,
            storage = "storage",
            storageId = 3,
            name = "Test Name"
        ),
        currentStorageId = "1",
        topologies = listOf(
            TopologyDto(id = 1, position = "BUFF"),
            TopologyDto(id = 2, position = "A-01")
        ),
        isLoadingTopologies = false,
        initialPosition = "BUFF",
        initialPositionId = "1",
        initialMax = "100",
        initialMin = "0",
        initialMaterial = "LA0602600443",
        initialQty = "42",
        onPositionSelected = {_, _ -> },
        onMaxChange = {},
        onMinChange = {},
        onMaterialChange = {},
        onQtyChange = {},
        onDismiss = {} ,
        onSave = {} ,
        onLoadTopologies = {}
    )
}