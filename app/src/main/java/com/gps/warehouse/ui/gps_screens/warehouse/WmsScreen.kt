package com.gps.warehouse.ui.gps_screens.warehouse

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.WarehousePermissionDto
import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SapBadge
import com.gps.warehouse.ui.components.SearchAndFilterBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannedData
import com.gps.warehouse.utils.ScannerManager

private const val TAG = "WmsScreen"

// ====================== SCREEN ======================
@Composable
fun WmsScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by mainViewModel.uiState.collectAsState()
    val availableWarehouses by mainViewModel.availableWarehouses.collectAsState()
    val isLoadingMore by mainViewModel.isLoadingMore.collectAsState()
    val hasMore by mainViewModel.hasMoreWms.collectAsState()

    // === Фильтры ===
    var searchQuery by remember { mutableStateOf("") }
    var selectedStorageFilterId by remember { mutableStateOf<String?>(null) }
    var showOnlyNonZeroQty by remember { mutableStateOf(false) }
    var isFiltersExpanded by remember { mutableStateOf(false) }

    // === Сканер ===
    val scannerManager = remember { ScannerManager(context) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }

    // Подписываемся на настройку
    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    // Флаг показа диалога камеры
    var showCameraDialog by remember { mutableStateOf(false) }

    // Инициализация
    LaunchedEffect(Unit) {
        mainViewModel.loadAvailableWarehouses()
        scannerManager.init()
    }

    fun processScannedData(scannedData: String){
        Log.d(TAG, "LE materialCode")
//        if (scannedData.isEmpty()) return
        val parsedData: ScannedData? = BarcodeParser.parse(scannedData)
        val materialCode = parsedData?.material ?: scannedData.trim()
        lastScannedCode = materialCode
        mainViewModel.searchWmsByMaterial(materialCode)
    }

    // Сканирование: материал → поиск
    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData ->
            processScannedData(scannedData)
        }
    }

    DisposableEffect(Unit) {
        onDispose { scannerManager.release() }
    }

    // После сканирования — навигируем, если нашли
    LaunchedEffect(uiState) {
        val code = lastScannedCode ?: return@LaunchedEffect
        if (uiState !is MainViewModel.UiState.WmsLoaded) return@LaunchedEffect

        val foundItem = (uiState as MainViewModel.UiState.WmsLoaded).items.find {
            it.material.equals(code, ignoreCase = true)
        }
        if (foundItem != null) {
            navController.navigate(
                "wms_item_details/${Uri.encode(foundItem.material)}/${foundItem.storageId}"
            )
        } else {
            Toast.makeText(context, "Материал $code не найден", Toast.LENGTH_LONG).show()
        }
        lastScannedCode = null
    }

    // Фильтры → перезагрузка (сработает и при первом Compose)
    LaunchedEffect(searchQuery, selectedStorageFilterId, showOnlyNonZeroQty) {
        mainViewModel.updateWmsFilters(
            storageId = selectedStorageFilterId,
            searchQuery = searchQuery,
            hideZeroQty = showOnlyNonZeroQty
        )
    }

    // Диалог сканирования камерой
    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                // Диалог закроется автоматически через onDismiss
                showCameraDialog = false
            }
        )
    }

    // === Контент ===
    WmsContent(
        uiState = uiState,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStorageFilterId = selectedStorageFilterId,
        onStorageFilterSelected = { selectedStorageFilterId = it },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        onBackClick = { navController.popBackStack() },
        onNavigateToRequests = { navController.navigate("wms_requests") },
        onRetryClick = { mainViewModel.loadWmsData() },
        onItemClick = { item ->
            navController.navigate(
                "wms_item_details/${Uri.encode(item.material)}/${item.storageId}"
            )
        },
        showOnlyNonZeroQty = showOnlyNonZeroQty,
        onShowOnlyNonZeroQtyChange = { showOnlyNonZeroQty = it },
        onResetFilters = {
            searchQuery = ""
            selectedStorageFilterId = null
            showOnlyNonZeroQty = false
        },
        availableWarehouses = availableWarehouses,
        isLoadingMore = isLoadingMore,
        hasMore = hasMore,
        onLoadMore = { mainViewModel.loadMoreWmsData() }
    )
}

// ====================== CONTENT ======================
@Composable
fun WmsContent(
    uiState: MainViewModel.UiState,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit,
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
                availableWarehouses.forEach { wh ->
                    FilterChip(
                        selected = selectedStorageFilterId == wh.id,
                        onClick = { onStorageFilterSelected(wh.id) },
                        label = { Text(wh.name) }
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
            is MainViewModel.UiState.Loading -> CustomLoadingView()

            is MainViewModel.UiState.WmsLoaded -> {
                val allItems = uiState.items
                val lazyListState = rememberLazyListState()

                var lastLoadedCount by remember { mutableIntStateOf(0) }
                var localLoading by remember { mutableStateOf(false) }

                val currentHasMore by rememberUpdatedState(hasMore)
                val currentIsLoading by rememberUpdatedState(isLoadingMore)
                val currentItemsCount by rememberUpdatedState(allItems.size)

                // Сброс localLoading
                LaunchedEffect(allItems.size, isLoadingMore) {
                    if (allItems.size != lastLoadedCount) {
                        lastLoadedCount = allItems.size
                        localLoading = false
                    }
                    if (!isLoadingMore && localLoading) {
                        localLoading = false
                    }
                }

                // Триггер пагинации
                LaunchedEffect(lazyListState) {
                    snapshotFlow {
                        val layoutInfo = lazyListState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@snapshotFlow false
                        val lastVisibleIndex = visibleItems.last().index
                        val totalItems = layoutInfo.totalItemsCount
                        lastVisibleIndex >= totalItems - 5
                    }.collect { isNearEnd ->
                        if (isNearEnd && !localLoading && !currentIsLoading && currentHasMore) {
                            Log.d(TAG, "Triggering loadMore")
                            localLoading = true
                            onLoadMore()
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier) {
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

                            if (!hasMore && allItems.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "• Все материалы загружены •",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Плавающая кнопка сканирования (только если включено в настройках)
                    CameraScanButton(
                        onClick = onCameraScanClick,
                        cameraScanEnabled = cameraScanEnabled,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            is MainViewModel.UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetryClick,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> Unit
        }
    }
}

// ====================== КАРТОЧКА ======================
@Composable
fun WmsItemCard(item: WmsItemDto, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    if (item.sapA == 1) SapBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name.ifBlank { "Без названия" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

// ====================== PREVIEW ======================
@Preview(showBackground = true, name = "WMS Screen - Loaded")
@Composable
fun WmsPreviewLoaded() {
    val fakeItems = listOf(
        WmsItemDto(
            id = 66, material = "LA0602600443", max = 2, min = 0,
            positionId = 0, position = "BUFF", price = 150.0, qty = 4.0,
            sapA = 0, storage = "3051", storageId = 1, name = "СИГНАЛИЗАЦИОННАЯ ЛАМПА"
        ),
        WmsItemDto(
            id = 67, material = "LA0713000190", max = 5, min = 1,
            positionId = 0, position = "A-01", price = 1200.0, qty = 10.0,
            sapA = 1, storage = "4007", storageId = 2, name = "Станция зарядки"
        )
    )
    MaterialTheme {
        Surface {
            WmsContent(
                uiState = MainViewModel.UiState.WmsLoaded(fakeItems),
                cameraScanEnabled = true,
                onCameraScanClick = {},
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
                showOnlyNonZeroQty = false,
                onShowOnlyNonZeroQtyChange = {},
                onResetFilters = {},
                availableWarehouses = listOf(
                    WarehousePermissionDto(id = "1", name = "3051", isLeader = "1", isVirtual = "0"),
                    WarehousePermissionDto(id = "2", name = "4007", isLeader = "0", isVirtual = "0")
                ),
                isLoadingMore = false,
                hasMore = false,
                onLoadMore = {}
            )
        }
    }
}


@Preview(showBackground = true, name = "WMS Screen - Loaded")
@Composable
fun WmsPreviewError() {
    MaterialTheme {
        Surface {
            WmsContent(
                uiState = MainViewModel.UiState.Error("Error"),
                cameraScanEnabled = true,
                onCameraScanClick = {},
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
                showOnlyNonZeroQty = false,
                onShowOnlyNonZeroQtyChange = {},
                onResetFilters = {},
                availableWarehouses = listOf(
                    WarehousePermissionDto(id = "1", name = "3051", isLeader = "1", isVirtual = "0"),
                    WarehousePermissionDto(id = "2", name = "4007", isLeader = "0", isVirtual = "0")
                ),
                isLoadingMore = false,
                hasMore = false,
                onLoadMore = {}
            )
        }
    }
}