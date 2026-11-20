package com.gps.warehouse.ui.assets_screens.inventory

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SapBadge
import com.gps.warehouse.ui.components.SplashLoadingScreen
import com.gps.warehouse.utils.InventoryQrParser
import com.gps.warehouse.utils.ScannerManager
import kotlinx.coroutines.launch

private const val TAG = "INVENTORY_SCREEN"

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationItemsScreen(
    sessionId: Int,
    isCompleted: Boolean,
    navController: NavHostController,
    assetViewModel: AssetViewModel,
    mainViewModel: MainViewModel
) {
    val uiState by assetViewModel.inventorizationItemsUiState.collectAsState()
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }
    val focusManager = LocalFocusManager.current
    val quantityFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Индикатор серверного поиска при сканировании
    var isSearching by remember { mutableStateOf(false) }
    var isSavingFact by remember { mutableStateOf(false) }

    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<InventorizationItemDto?>(null) }
    var inputQty by remember { mutableStateOf("") }
    var showCompleteDialog by remember { mutableStateOf(false) }

    // ID элемента, который нужно выделить ПОСЛЕ перезагрузки списка
    // (когда отсканированный актив скрыт фильтрами или на другой странице)
    var pendingSelectAssetId by remember { mutableStateOf<Int?>(null) }

    // === ФИЛЬТРЫ ===
    var searchQuery by remember { mutableStateOf("") }
    var inventoryId by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var materialId by remember { mutableStateOf("") }
    var isCheckedFilter by remember { mutableStateOf<String?>(null) }   // null | "checked" | "not_checked"
    var isFiltersExpanded by remember { mutableStateOf(false) }

    // === ПАГИНАЦИЯ ===
    var currentPage by remember { mutableIntStateOf(1) }
    var isRequestInFlight by remember { mutableStateOf(false) }

    // === ХЕЛПЕР: сброс всех фильтров ===
    fun resetAllFilters() {
        Log.d(TAG, "resetAllFilters()")
        searchQuery = ""
        inventoryId = ""
        serialNumber = ""
        materialId = ""
        isCheckedFilter = null
        currentPage = 1
    }

    // === Выделение / снятие выделения элемента ===
    fun toggleSelection(item: InventorizationItemDto) {
        if (selectedAsset?.inventorizationId == item.inventorizationId) {
            selectedAsset = null
            focusManager.clearFocus()
        } else {
            selectedAsset = item
            inputQty = item.quantityFact?.toString() ?: ""
        }
    }

    // ==================== СКАНЕР: обработка ====================
    fun processScannedData(scannedData: String) {
        if (scannedData.isEmpty()) return
        if (isSavingFact) {
            Toast.makeText(context, "Идёт сохранение, подождите...", Toast.LENGTH_SHORT).show()
            return
        }
        if (isCompleted) {
            Toast.makeText(context, "Инвентаризация завершена. Изменения невозможны.", Toast.LENGTH_SHORT).show()
            return
        }

        val scannedValue = InventoryQrParser.parseAny(scannedData)
        if (scannedValue == null) {
            Toast.makeText(context, "Не удалось распознать QR-код", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "processScannedData: scannedValue=$scannedValue")

        val currentState = uiState
        if (currentState !is AssetViewModel.AssetUiState.InventorizationItemsLoaded) {
            Toast.makeText(context, "Список активов ещё не загружен", Toast.LENGTH_SHORT).show()
            return
        }

        // === 1. Быстрый локальный поиск в ТЕКУЩЕМ списке ===
        val foundLocally = currentState.items.find {
            (it.assetId != null && it.assetId.toString() == scannedValue) ||
                    (it.materialId != null && it.materialId.equals(scannedValue, ignoreCase = true)) ||
                    (it.materialId != null && it.materialId.equals("0${scannedValue}0000", ignoreCase = true)) ||
                    it.serialNumber.equals(scannedValue, ignoreCase = true) ||
                    it.serialNumber.equals(scannedValue.takeLast(18), ignoreCase = true) ||
                    it.inventoryId.equals(scannedValue, ignoreCase = true) ||
                    it.inventoryId.equals("0$scannedValue", ignoreCase = true) ||
                    it.inventoryId.equals("1$scannedValue", ignoreCase = true)
        }

        if (foundLocally != null) {
            Log.d(TAG, "processScannedData: найден локально inventorizationId=${foundLocally.inventorizationId}")
            toggleSelection(foundLocally)
            return
        }

        // === 2. Локально не нашли — ищем на сервере ===
        scope.launch {
            isSearching = true
            try {
                val (foundAsset, usedValue) = assetViewModel.findAssetByScanValue(scannedValue)
                Log.d(TAG, "processScannedData: findAssetByScanValue -> assetId=${foundAsset?.assetId}, materialId=${foundAsset?.materialId}, usedValue=$usedValue")

                if (foundAsset == null) {
                    Toast.makeText(
                        context,
                        "Актив '$scannedValue' не найден",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // === 3. Ищем строку инвентаризации в сессии ===
                val foundItem = assetViewModel.findInventorizationItemByAsset(
                    sessionId = sessionId,
                    asset = foundAsset,
                    usedValue = usedValue
                )
                Log.d(TAG, "processScannedData: findInventorizationItemByAsset -> inventorizationId=${foundItem?.inventorizationId}")

                if (foundItem == null) {
                    Toast.makeText(
                        context,
                        "Актив '${foundAsset.name ?: scannedValue}' не входит в эту сессию инвентаризации",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // === 4. Элемент найден. Проверяем, виден ли он в текущем списке ===
                val inCurrentList = currentState.items.any {
                    it.inventorizationId == foundItem.inventorizationId
                }

                if (inCurrentList) {
                    // Уже в списке — выделяем сразу
                    toggleSelection(foundItem)
                } else {
                    // Элемент скрыт фильтрами или на другой странице.
                    // Заполняем фильтры значениями найденного актива, чтобы список сузился,
                    // и запоминаем ID для выделения после перезагрузки.
                    Toast.makeText(
                        context,
                        "Актив найден. Применяем фильтры...",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Заполняем ОДНО самое надёжное поле (по приоритету уникальности)
                    when {
                        !foundItem.inventoryId.isNullOrBlank() -> {
                            Log.d(TAG, "applyFilters: inventoryId=${foundItem.inventoryId}")
                            searchQuery = ""
                            serialNumber = ""
                            materialId = ""
                            isCheckedFilter = null
                            inventoryId = foundItem.inventoryId
                        }
                        !foundItem.serialNumber.isNullOrBlank() -> {
                            Log.d(TAG, "applyFilters: serialNumber=${foundItem.serialNumber}")
                            searchQuery = ""
                            inventoryId = ""
                            materialId = ""
                            isCheckedFilter = null
                            serialNumber = foundItem.serialNumber
                        }
                        !foundItem.materialId.isNullOrBlank() -> {
                            Log.d(TAG, "applyFilters: materialId=${foundItem.materialId}")
                            searchQuery = ""
                            inventoryId = ""
                            serialNumber = ""
                            isCheckedFilter = null
                            materialId = foundItem.materialId
                        }
                        else -> {
                            // Fallback — по названию
                            Log.d(TAG, "applyFilters: searchQuery=${foundItem.assetName}")
                            inventoryId = ""
                            serialNumber = ""
                            materialId = ""
                            isCheckedFilter = null
                            searchQuery = foundItem.assetName
                        }
                    }

                    pendingSelectAssetId = foundItem.inventorizationId
                    currentPage = 1
                }
            } catch (e: Exception) {
                Log.e(TAG, "processScannedData: ошибка поиска", e)
                Toast.makeText(context, "Ошибка поиска: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSearching = false
            }
        }
    }

    // ==================== Диалог камеры ====================
    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                showCameraDialog = false
            }
        )
    }

    // ==================== Фокус на поле ввода количества ====================
    LaunchedEffect(selectedAsset?.inventorizationId) {
        if (selectedAsset != null) {
            kotlinx.coroutines.delay(50)
            quantityFocusRequester.requestFocus()
        }
    }

    // ==================== ТСД-сканер ====================
    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData ->
            processScannedData(scannedData)
        }
    }

    // Сбрасываем отложенное выделение при ручном изменении фильтров
    LaunchedEffect(searchQuery, inventoryId, serialNumber, materialId, isCheckedFilter) {
        if (pendingSelectAssetId != null) {
            Log.d(TAG, "Manual filter change -> drop pendingSelectAssetId")
            pendingSelectAssetId = null
        }
    }

    DisposableEffect(Unit) {
        scannerManager.init()
        onDispose { scannerManager.release() }
    }

    // ==================== ЕДИНЫЙ ИСТОЧНИК ЗАПРОСОВ ====================
    LaunchedEffect(
        sessionId,
        searchQuery,
        inventoryId,
        serialNumber,
        materialId,
        isCheckedFilter,
        currentPage
    ) {
        Log.d(
            TAG,
            "loadLaunchedEffect -> page=$currentPage, search='$searchQuery', inv='$inventoryId', " +
                    "sn='$serialNumber', materialId='$materialId', isChecked=$isCheckedFilter"
        )
        isRequestInFlight = true
        try {
            assetViewModel.loadInventorizationItems(
                sessionId = sessionId,
                page = currentPage,
                pageSize = 100,
                name = searchQuery.takeIf { it.isNotBlank() },
                inventoryId = inventoryId.takeIf { it.isNotBlank() },
                serialNumber = serialNumber.takeIf { it.isNotBlank() },
                materialId = materialId.takeIf { it.isNotBlank() },
                isChecked = isCheckedFilter,
                append = currentPage > 1,
                showCacheOnStart = currentPage == 1 &&
                        searchQuery.isBlank() &&
                        inventoryId.isBlank() &&
                        serialNumber.isBlank() &&
                        materialId.isBlank() &&
                        isCheckedFilter == null,
            )
        } finally {
            isRequestInFlight = false
        }
    }

    // ==================== ОТЛОЖЕННОЕ ВЫДЕЛЕНИЕ после перезагрузки списка ====================
    LaunchedEffect(uiState, pendingSelectAssetId) {
        val pendingId = pendingSelectAssetId ?: return@LaunchedEffect
        Log.d(TAG, "pendingSelect: ждём элемент inventorizationId=$pendingId, uiState=${uiState::class.simpleName}")

        when (val s = uiState) {
            is AssetViewModel.AssetUiState.InventorizationItemsLoaded -> {
                val target = s.items.find { it.inventorizationId == pendingId }
                if (target != null) {
                    Log.d(TAG, "pendingSelect: элемент найден, выделяем")
                    selectedAsset = target
                    inputQty = target.quantityFact?.toString() ?: ""
                    pendingSelectAssetId = null
                } else {
                    Log.d(TAG, "pendingSelect: элемент ещё не в списке, ждём дальше")
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Log.e(TAG, "pendingSelect: ошибка загрузки, сбрасываем pending")
                pendingSelectAssetId = null
                Toast.makeText(context, "Не удалось загрузить список: ${s.message}", Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    // ==================== Рендер ====================
    InventorizationItemsContent(
        sessionId = sessionId,
        uiState = uiState,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        isSavingFact = isSavingFact,
        isSearching = isSearching,
        isCompleted = isCompleted,
        selectedAsset = selectedAsset,
        inputQty = inputQty,
        showCompleteDialog = showCompleteDialog,
        quantityFocusRequester = quantityFocusRequester,

        // Фильтры
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it; currentPage = 1 },
        inventoryId = inventoryId,
        onInventoryIdChange = { inventoryId = it; currentPage = 1 },
        serialNumber = serialNumber,
        onSerialNumberChange = { serialNumber = it; currentPage = 1 },
        materialId = materialId,
        onMaterialIdChange = { materialId = it; currentPage = 1 },
        isCheckedFilter = isCheckedFilter,
        onIsCheckedFilterChange = { isCheckedFilter = it; currentPage = 1 },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        onResetFilters = { resetAllFilters() },

        // Пагинация
        onLoadMore = {
            if (isSavingFact) return@InventorizationItemsContent
            val state = uiState as? AssetViewModel.AssetUiState.InventorizationItemsLoaded
                ?: return@InventorizationItemsContent
            if (!state.hasNext) return@InventorizationItemsContent
            if (isRequestInFlight) return@InventorizationItemsContent
            currentPage += 1
        },

        // Обработчики
        onQtyChange = { inputQty = it },
        onAssetSelect = { asset -> toggleSelection(asset) },
        onConfirmClick = {
            val asset = selectedAsset ?: return@InventorizationItemsContent
            val qty = inputQty.toIntOrNull() ?: return@InventorizationItemsContent
            if (qty < 0 || isSavingFact) return@InventorizationItemsContent

            isSavingFact = true
            // ВАЖНО: НЕ сбрасываем selectedAsset, НЕ очищаем фильтры
            assetViewModel.checkInventorizationItem(
                sessionId = sessionId,
                assetId = asset.assetId,
                materialId = asset.materialId,
                quantityFact = qty,
                onSuccess = {
                    // /check прошёл. Теперь можно перезагрузить список С ТЕКУЩИМИ ФИЛЬТРАМИ
                    // и заново выделить этот же элемент (чтобы обновилось isChecked / quantityFact)
                    pendingSelectAssetId = asset.inventorizationId
                    assetViewModel.loadInventorizationItems(
                        sessionId = sessionId,
                        page = 1,                       // после /check всегда тянем первую страницу
                        pageSize = 100,
                        name = searchQuery.takeIf { it.isNotBlank() },
                        inventoryId = inventoryId.takeIf { it.isNotBlank() },
                        serialNumber = serialNumber.takeIf { it.isNotBlank() },
                        materialId = materialId.takeIf { it.isNotBlank() },
                        isChecked = isCheckedFilter,
                        append = false,
                        showCacheOnStart = false,
                    )
                    isSavingFact = false
                },
                onError = { msg ->
                    isSavingFact = false
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            )
        },
        onShowCompleteDialogChange = { showCompleteDialog = it },
        onCompleteSession = {
            assetViewModel.completeInventorizationSession(sessionId)
            navController.popBackStack()
        },
        onDetailsClick = { assetId -> navController.navigate("asset_details/$assetId") },
        onRetry = {
            assetViewModel.loadInventorizationItems(
                sessionId = sessionId,
                page = currentPage,
                pageSize = 100,
                name = searchQuery.takeIf { it.isNotBlank() },
                inventoryId = inventoryId.takeIf { it.isNotBlank() },
                serialNumber = serialNumber.takeIf { it.isNotBlank() },
                materialId = materialId.takeIf { it.isNotBlank() },
                isChecked = isCheckedFilter,
                append = false,
                showCacheOnStart = false,
            )
        },
        onBackClick = { navController.popBackStack() },
        currentPage = currentPage,
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
    isSavingFact: Boolean,
    isSearching: Boolean,
    isCompleted: Boolean,
    selectedAsset: InventorizationItemDto?,
    inputQty: String,
    showCompleteDialog: Boolean,
    quantityFocusRequester: FocusRequester,

    // Фильтры
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    inventoryId: String,
    onInventoryIdChange: (String) -> Unit,
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    materialId: String,
    onMaterialIdChange: (String) -> Unit,
    isCheckedFilter: String?,
    onIsCheckedFilterChange: (String?) -> Unit,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onResetFilters: () -> Unit,

    // Пагинация
    onLoadMore: () -> Unit,
    currentPage: Int,

    // Обработчики
    onQtyChange: (String) -> Unit,
    onAssetSelect: (InventorizationItemDto) -> Unit,
    onConfirmClick: () -> Unit,
    onShowCompleteDialogChange: (Boolean) -> Unit,
    onCompleteSession: () -> Unit,
    onDetailsClick: (assetId: Int) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // Скролл к выбранному ассету (если он есть в текущем списке)
    LaunchedEffect(selectedAsset?.inventorizationId) {
        val sel = selectedAsset ?: return@LaunchedEffect
        val state = uiState as? AssetViewModel.AssetUiState.InventorizationItemsLoaded
            ?: return@LaunchedEffect
        val index = state.items.indexOfFirst {
            it.inventorizationId == sel.inventorizationId
        }
        if (index != -1) {
            listState.animateScrollToItem(index)
        }
    }

    // Триггер пагинации в конце списка
    LaunchedEffect(listState, uiState, currentPage) {
        if (isSavingFact) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow false
            val lastVisibleItem = visibleItems.last()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem.index >= totalItems - 3
        }.collect { isNearEnd ->
            val state = uiState as? AssetViewModel.AssetUiState.InventorizationItemsLoaded
            if (isNearEnd && state != null && state.hasNext) {
                onLoadMore()
            }
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
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Завершить инвентаризацию",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null
            )

            // === Поле поиска + иконка фильтров ===
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        "Поиск",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onToggleFilters) {
                        BadgedBox(
                            badge = {
                                if (
                                    inventoryId.isNotBlank() ||
                                    serialNumber.isNotBlank() ||
                                    materialId.isNotBlank() ||
                                    isCheckedFilter != null
                                ) {
                                    Badge(modifier = Modifier.size(8.dp))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFiltersExpanded) Icons.Default.ExpandLess
                                else Icons.Default.FilterList,
                                contentDescription = "Фильтры"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
//                enabled = !isCompleted
            )

            // === Раскрывающиеся фильтры ===
            AnimatedVisibility(
                visible = isFiltersExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = inventoryId,
                            onValueChange = onInventoryIdChange,
                            label = { Text("Инвентарный номер") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = serialNumber,
                            onValueChange = onSerialNumberChange,
                            label = { Text("Серийный номер") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = materialId,
                            onValueChange = onMaterialIdChange,
                            label = { Text("Номер основного средства") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("Статус сверки", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isCheckedFilter == null,
                                onClick = { onIsCheckedFilterChange(null) },
                                label = { Text("Все") }
                            )
                            FilterChip(
                                selected = isCheckedFilter == "checked",
                                onClick = { onIsCheckedFilterChange("checked") },
                                label = { Text("Сверено") }
                            )
                            FilterChip(
                                selected = isCheckedFilter == "not_checked",
                                onClick = { onIsCheckedFilterChange("not_checked") },
                                label = { Text("Не сверено") }
                            )
                        }
                    }
                }
            }

            // === Кнопка "Сбросить" ===
            val hasActiveFilters = inventoryId.isNotBlank() ||
                    serialNumber.isNotBlank() ||
                    materialId.isNotBlank() ||
                    (isCheckedFilter != null)

            AnimatedVisibility(
                visible = hasActiveFilters,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onResetFilters,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сбросить фильтры", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (isCompleted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                is AssetViewModel.AssetUiState.Loading -> SplashLoadingScreen()

                is AssetViewModel.AssetUiState.InventorizationItemsLoaded -> {
                    val items = uiState.items
                    if (items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Inventory,
                                    null,
                                    Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Активы не найдены",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hasActiveFilters) {
                                    TextButton(onClick = onResetFilters) {
                                        Text("Сбросить фильтры")
                                    }
                                }
                            }
                        }
                    } else {
                        val bottomPadding = if (cameraScanEnabled) 80.dp else 16.dp
                        Text(
                            text = "Загружено: ${items.size} / ${uiState.total}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(start = 16.dp)
                        )
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
                            items(items, key = { it.inventorizationId }) { item ->
                                InventoryItemCard(
                                    item = item,
                                    isSelected = selectedAsset?.inventorizationId == item.inventorizationId,
                                    onClick = { if (!isCompleted && !isSavingFact) onAssetSelect(item) },
                                    inputQty = if (selectedAsset?.inventorizationId == item.inventorizationId) inputQty else "",
                                    onQtyChange = if (selectedAsset?.inventorizationId == item.inventorizationId) onQtyChange else { _ -> },
                                    onConfirmClick = onConfirmClick,
                                    focusRequester = if (selectedAsset?.inventorizationId == item.inventorizationId) quantityFocusRequester else null,
                                    isLoading = isSavingFact && selectedAsset?.inventorizationId == item.inventorizationId
                                )
                            }

                            // Индикатор подгрузки
                            if (items.size >= currentPage * 50) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is AssetViewModel.AssetUiState.Error -> ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f)
                )

                else -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { SplashLoadingScreen() }
            }
        }

        // Блокировка камеры и сканера при сохранении
        if (!isCompleted && !isSavingFact) {
            CameraScanButton(
                onClick = onCameraScanClick,
                cameraScanEnabled = cameraScanEnabled,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Оверлей поиска
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SplashLoadingScreen(message = "Поиск ...")
                    }
                }
            }
        }

        // Оверлей сохранения факта
        if (isSavingFact) {
            SplashLoadingScreen()
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Card(
//                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
//                    ),
//                    elevation = CardDefaults.cardElevation(8.dp)
//                ) {
//                    Column(
//                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(40.dp),
//                            strokeWidth = 3.dp,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                        Text(
//                            "Сохранение…",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.SemiBold
//                        )
//                        Text(
//                            "Пожалуйста, подождите.\nЭто может занять до 20 секунд.",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            textAlign = TextAlign.Center
//                        )
//                    }
//                }
//            }
        }
    }

    if (showCompleteDialog && !isCompleted) {
        AlertDialog(
            onDismissRequest = { onShowCompleteDialogChange(false) },
            icon = { Icon(Icons.Default.DoneAll, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Завершить инвентаризацию?") },
            text = { Text("Вы уверены, что хотите завершить инвентаризацию сессии #$sessionId? После этого редактирование будет невозможно.") },
            confirmButton = {
                Button(
                    onClick = {
                        onCompleteSession()
                        onShowCompleteDialogChange(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { onShowCompleteDialogChange(false) }) { Text("Отмена") }
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
    var textFieldValue by remember(inputQty) {
        mutableStateOf(TextFieldValue(text = inputQty, selection = TextRange(inputQty.length)))
    }

    LaunchedEffect(inputQty) {
        textFieldValue = TextFieldValue(text = inputQty, selection = TextRange(inputQty.length))
    }

    val containerColor = when {
        isSelected -> {
            if (item.isChecked) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer
        }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.assetName,
                    modifier = Modifier.weight(0.75f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (item.isChecked) "Сверено" else "Не сверено",
                    tint = if (item.isChecked) Color(0, 150, 0, 255) else Color.Red,
                    modifier = Modifier
                        .size(20.dp)
                        .weight(0.1f)
                )
                if (item.materialId != null) SapBadge()
            }

            Text(
                text = "Asset ID: ${item.assetId ?: "Не указан"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Номер SAP: ${item.materialId ?: "Не указан"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            item.serialNumber?.let {
                Text("Серийный номер: $it", style = MaterialTheme.typography.bodyMedium)
            }
            item.inventoryId?.let {
                Text("Инв. номер: $it", style = MaterialTheme.typography.bodyMedium)
            }
            item.checkedByFullName?.let {
                Text("Отсканировал: $it", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("План: ${item.quantity ?: 0}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(16.dp))

                if (isSelected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            Text(
                                "Сохранение…",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "Факт:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = { newValue ->
                                    textFieldValue = newValue
                                    onQtyChange(newValue.text)
                                },
                                placeholder = { Text("0", style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { onConfirmClick() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .then(
                                        if (focusRequester != null) Modifier.focusRequester(focusRequester)
                                        else Modifier
                                    ),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = MaterialTheme.shapes.small,
                                enabled = !item.isChecked,
                            )

                            // Если актив не проверен, то показываем кнопку
                            if (!item.isChecked) {
                                IconButton(
                                    onClick = onConfirmClick,
                                    enabled = textFieldValue.text.toIntOrNull() != null,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Подтвердить",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (textFieldValue.text.toIntOrNull() != null)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Факт: ${item.quantityFact ?: "–"}",
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
@Preview(device = "spec:width=1080px,height=2740px,dpi=440")
@Composable
private fun InventorizationItemsContentPreview() {
    MaterialTheme {
        Surface {
            InventorizationItemsContent(
                sessionId = 42,
                uiState = AssetViewModel.AssetUiState.InventorizationItemsLoaded(
                    sessionId = 42,
                    total = 5,
                    hasNext = true,
                    items = listOf(
                        InventorizationItemDto(
                            inventorizationId = 1,
                            sessionId = 42,
                            assetId = null,
                            materialId = "1",
                            serialNumber = "serial_number",
                            inventoryId = "inv_number",
                            assetName = "Компьютер Dell",
                            isChecked = true,
                            quantity = 10,
                            quantityFact = 8,
                            checkedByFullName = "ФИО"
                        ),
                        InventorizationItemDto(
                            inventorizationId = 2,
                            sessionId = 42,
                            assetId = null,
                            materialId = "333",
                            serialNumber = "serial_number",
                            inventoryId = "inv_number",
                            assetName = "Монитор LG",
                            isChecked = false,
                            quantity = 5,
                            quantityFact = null,
                            checkedByFullName = "ФИО"
                        ),
                        InventorizationItemDto(
                            inventorizationId = 3,
                            sessionId = 42,
                            assetId = 103,
                            materialId = null,
                            serialNumber = "serial_number",
                            inventoryId = "inv_number",
                            assetName = "Клавиатура",
                            isChecked = false,
                            quantity = 20,
                            quantityFact = null,
                            checkedByFullName = "ФИО"
                        )
                    )
                ),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                isSavingFact = false,
                isSearching = false,
                isCompleted = false,
                selectedAsset = InventorizationItemDto(
                    inventorizationId = 1,
                    sessionId = 42,
                    assetId = null,
                    materialId = "1",
                    serialNumber = "serial_number",
                    inventoryId = "inv_number",
                    assetName = "Компьютер Dell",
                    isChecked = true,
                    quantity = 10,
                    quantityFact = 8,
                    checkedByFullName = "ФИО"
                ),
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
                onBackClick = {},
                materialId = "",
                inventoryId = "",
                serialNumber = "",
                onResetFilters = {},
                searchQuery = "",
                onLoadMore = {},
                onToggleFilters = {},
                isFiltersExpanded = true,
                onSearchQueryChange = { _ -> },
                currentPage = 1,
                isCheckedFilter = null,
                onMaterialIdChange = {},
                onInventoryIdChange = {},
                onSerialNumberChange = {},
                onIsCheckedFilterChange = {}
            )
        }
    }
}

@Preview
@Composable
private fun InventorizationItemsContentPreviewLoading() {
    MaterialTheme {
        Surface {
            InventorizationItemsContent(
                sessionId = 42,
                uiState = AssetViewModel.AssetUiState.Loading,
                cameraScanEnabled = true,
                onCameraScanClick = {},
                isSavingFact = false,
                isSearching = true,
                isCompleted = false,
                selectedAsset = null,
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
                onBackClick = {},
                onIsCheckedFilterChange = {},
                onLoadMore = {},
                onMaterialIdChange = {},
                onInventoryIdChange = {},
                currentPage = 1,
                isCheckedFilter = null,
                isFiltersExpanded = false,
                searchQuery = "",
                materialId = "",
                inventoryId = "",
                serialNumber = "",
                onSerialNumberChange = {},
                onResetFilters = {},
                onToggleFilters = {},
                onSearchQueryChange = {}
            )
        }
    }
}