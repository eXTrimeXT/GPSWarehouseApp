package com.gps.warehouse.ui.assets_screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetStatusDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScanButton
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.InventoryQrParser // Импорт утилиты

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsByTypeScreen(
    assetTypeId: Int?,
    assetTypeName: String,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }
    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var inventoryId by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var assetStatus by remember { mutableStateOf<String?>(null) }
    var modelId by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
    var isFiltersExpanded by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) { viewModel.loadAssetStatuses() }

    // Используем вынесенный парсер
    fun processScannedData(scannedData: String) {
        if (scannedData.isEmpty()) return
        val parseSerialNumber = InventoryQrParser.parseSerialNumber(scannedData)
        if (parseSerialNumber != null) {
            val currentState = uiState
            if (currentState is AssetViewModel.AssetUiState.AssetsLoadedPaginated) {
                val foundAsset = currentState.assets.find {
                    it.assetId.toString() == parseSerialNumber ||
                            it.serialNumber.equals(parseSerialNumber, ignoreCase = true) ||
                            it.inventoryId.equals(parseSerialNumber, ignoreCase = true)
                }
                if (foundAsset != null) {
                    navController.navigate("asset_details/${foundAsset.assetId}")
                } else {
                    Toast.makeText(
                        context,
                        "Серийный номер '$parseSerialNumber' не найден в списке",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "Список активов ещё не загружен", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Неверный формат QR-кода", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData -> processScannedData(scannedData) }
    }
    DisposableEffect(Unit) { scannerManager.init(); onDispose { scannerManager.release() } }
    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode); showCameraDialog = false
            })
    }

    LaunchedEffect(
        searchQuery,
        inventoryId,
        serialNumber,
        assetStatus,
        modelId,
        parentId,
        locationId,
        currentPage,
        assetTypeId
    ) {
        viewModel.loadAssetsByFilters(
            page = currentPage,
            pageSize = 50,
            name = searchQuery.takeIf { it.isNotBlank() },
            inventoryId = inventoryId.takeIf { it.isNotBlank() },
            serialNumber = serialNumber.takeIf { it.isNotBlank() },
            assetStatus = assetStatus,
            modelId = modelId.toIntOrNull(),
            assetTypeId = assetTypeId,
            parentId = parentId.toIntOrNull(),
            locationId = locationId.toIntOrNull()
        )
    }

    AssetsByTypeScreenContent(
        assetTypeName = assetTypeName,
        uiState = uiState,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        assetStatuses = viewModel.assetStatuses.collectAsState().value,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it; currentPage = 1 },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        inventoryId = inventoryId,
        onInventoryIdChange = { inventoryId = it; currentPage = 1 },
        serialNumber = serialNumber,
        onSerialNumberChange = { serialNumber = it; currentPage = 1 },
        assetStatus = assetStatus,
        onAssetStatusChange = { assetStatus = it; currentPage = 1 },
        onResetFilters = {
            searchQuery = ""; inventoryId = ""; serialNumber = ""; assetStatus = null; modelId =
            ""; parentId = ""; locationId = ""; currentPage = 1
        },
        onRetry = {
            currentPage = 1; viewModel.loadAssetsByFilters(
            page = 1,
            assetTypeId = assetTypeId
        )
        },
        onLoadMore = { currentPage++ },
        onAssetClick = { assetId -> navController.navigate("asset_details/$assetId") },
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsByTypeScreenContent(
    assetTypeName: String,
    uiState: AssetViewModel.AssetUiState,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit,
    assetStatuses: List<AssetStatusDto>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    inventoryId: String,
    onInventoryIdChange: (String) -> Unit,
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    assetStatus: String?,
    onAssetStatusChange: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onAssetClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(topBar = {
            MyCustomActionBar(
                text = assetTypeName,
                onBackClick = onBackClick
            )
        }) { paddingValues ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Поиск по названию") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = onToggleFilters) {
                            Icon(
                                if (isFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.FilterList,
                                "Фильтры"
                            )
                        }
                    },
                    singleLine = true
                )

                if (isFiltersExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
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
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = serialNumber,
                                onValueChange = onSerialNumberChange,
                                label = { Text("Серийный номер") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            var statusExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = statusExpanded,
                                onExpandedChange = { statusExpanded = !statusExpanded }) {
                                OutlinedTextField(
                                    value = assetStatus ?: "Любой статус",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Статус") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = statusExpanded,
                                    onDismissRequest = { statusExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Любой статус") },
                                        onClick = {
                                            onAssetStatusChange(null); statusExpanded = false
                                        })
                                    if (assetStatuses.isEmpty()) DropdownMenuItem(
                                        text = { Text("Загрузка...") },
                                        onClick = {},
                                        enabled = false
                                    )
                                    else assetStatuses.forEach { statusDto ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    statusDto.status
                                                )
                                            },
                                            onClick = {
                                                onAssetStatusChange(statusDto.status); statusExpanded =
                                                false
                                            })
                                    }
                                }
                            }
                            Button(
                                onClick = onResetFilters,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Сбросить фильтры") }
                        }
                    }
                }

                when (uiState) {
                    is AssetViewModel.AssetUiState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    is AssetViewModel.AssetUiState.AssetsLoadedPaginated -> {
                        val assets = uiState.assets
                        if (assets.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "Активы не найдены",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // ИСПРАВЛЕНИЕ ПЕРЕКРЫТИЯ: динамический отступ снизу
                            val bottomPadding = if (cameraScanEnabled) 80.dp else 16.dp
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
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
                                        text = "Найдено: ${assets.size} из ${uiState.total}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                                items(assets) { asset ->
                                    AssetCardPaginated(
                                        asset = asset,
                                        onClick = { onAssetClick(asset.assetId) })
                                }
                                if (uiState.hasNext) {
                                    item {
                                        Button(
                                            onClick = onLoadMore,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            enabled = uiState !is AssetViewModel.AssetUiState.Loading
                                        ) { Text("Загрузить еще") }
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

                    else -> {}
                }
            }
        }
        CameraScanButton(
            onClick = onCameraScanClick,
            cameraScanEnabled = cameraScanEnabled,
            modifier = Modifier
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun AssetCardPaginated(asset: AssetResponseDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Инв. номер: ${asset.inventoryId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    asset.serialNumber?.let { sn ->
                        Text(
                            text = "S/N: $sn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Подробнее",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = asset.assetTypeName ?: "Без типа",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = asset.assetStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (asset.assetStatus.lowercase()) {
                        "приемка", "в эксплуатации", "active" -> MaterialTheme.colorScheme.primary; "списан", "inactive" -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            asset.parentName?.let { parentName ->
                Spacer(modifier = Modifier.height(4.dp)); Text(
                text = "В составе: $parentName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            }
        }
    }
}

// ============================================================================
// PREVIEW ФУНКЦИИ
// ============================================================================

@Preview(showBackground = true, name = "Экран: Список активов")
@Composable
fun AssetsByTypeScreenContentPreview_Loaded() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetsByTypeScreenContent(
                assetTypeName = "Компьютеры",
                uiState = AssetViewModel.AssetUiState.AssetsLoadedPaginated(
                    assets = listOf(
                        getSampleFullAssetResponseDto(),
                        getSampleScrappedAssetResponseDto()
                    ),
                    total = 2,
                    page = 1,
                    pageSize = 50,
                    totalPages = 1,
                    hasNext = false,
                    hasPrevious = false
                ),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                assetStatuses = listOf(
                    AssetStatusDto(1, "Приемка"),
                    AssetStatusDto(2, "В ремонте"),
                    AssetStatusDto(7, "Списан")
                ),
                searchQuery = "",
                onSearchQueryChange = {},
                isFiltersExpanded = false,
                onToggleFilters = {},
                inventoryId = "",
                onInventoryIdChange = {},
                serialNumber = "",
                onSerialNumberChange = {},
                assetStatus = null,
                onAssetStatusChange = {},
                onResetFilters = {},
                onRetry = {},
                onLoadMore = {},
                onAssetClick = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Экран: Пустой список (с открытыми фильтрами)")
@Composable
fun AssetsByTypeScreenContentPreview_Empty() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetsByTypeScreenContent(
                assetTypeName = "Компьютеры",
                uiState = AssetViewModel.AssetUiState.AssetsLoadedPaginated(
                    assets = emptyList(),
                    total = 0,
                    page = 1,
                    pageSize = 50,
                    totalPages = 0,
                    hasNext = false,
                    hasPrevious = false
                ),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                assetStatuses = listOf(
                    AssetStatusDto(1, "Приемка"),
                    AssetStatusDto(2, "В ремонте"),
                    AssetStatusDto(7, "Списан")
                ),
                searchQuery = "Несуществующий актив",
                onSearchQueryChange = {},
                isFiltersExpanded = true,
                onToggleFilters = {},
                inventoryId = "TEST-999",
                onInventoryIdChange = {},
                serialNumber = "",
                onSerialNumberChange = {},
                assetStatus = "Списан",
                onAssetStatusChange = {},
                onResetFilters = {},
                onRetry = {},
                onLoadMore = {},
                onAssetClick = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Экран: Ошибка")
@Composable
fun AssetsByTypeScreenContentPreview_Error() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetsByTypeScreenContent(
                assetTypeName = "Компьютеры",
                uiState = AssetViewModel.AssetUiState.Error("Ошибка сети: проверьте подключение к интернету"),
                cameraScanEnabled = false,
                onCameraScanClick = {},
                assetStatuses = emptyList(),
                searchQuery = "",
                onSearchQueryChange = {},
                isFiltersExpanded = false,
                onToggleFilters = {},
                inventoryId = "",
                onInventoryIdChange = {},
                serialNumber = "",
                onSerialNumberChange = {},
                assetStatus = null,
                onAssetStatusChange = {},
                onResetFilters = {},
                onRetry = {},
                onLoadMore = {},
                onAssetClick = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (Полная)")
@Composable
fun AssetCardPaginatedPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetCardPaginated(
                asset = getSampleFullAssetResponseDto(),
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (С родительским активом)")
@Composable
fun AssetCardPaginatedPreview_WithParent() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetCardPaginated(
                asset = getSampleFullAssetResponseDto().copy(
                    name = "Процессор Intel Core i7",
                    inventoryId = "INV-002",
                    serialNumber = "SN-998877",
                    assetTypeName = "Комплектующие",
                    assetStatus = "В эксплуатации",
                    parentName = "Сервер Dell PowerEdge R740"
                ),
                onClick = { }
            )
        }
    }
}

// MOCK ДАННЫЕ
private fun getSampleFullAssetResponseDto(): AssetResponseDto {
    return AssetResponseDto(
        assetId = 2,
        name = "Ноутбук Lenovo ThinkPad X1",
        inventoryId = "INV-2024-00158",
        serialNumber = "SN-9876543210",
        assetStatus = "В эксплуатации",
        comment = "Выдан системному администратору",
        dateIssue = "2024-01-15",
        datePurchasing = "2024-01-10",
        modelId = 55,
        modelName = "ThinkPad X1 Carbon Gen 11",
        assetTypeId = 1,
        parentId = 4,
        locationId = 3,
        preparedBy = "0000015370",
        checkedBy = "0000015370",
        parentName = "Рабочая станция №12",
        manufacturerName = "Lenovo",
        vendorName = "ООО ТехноПоставка",
        osName = "Windows 11 Pro",
        createdBy = "0000015370",
        updatedBy = "0000015370",
        createdAt = "2024-01-01T10:00:00Z",
        updatedAt = "2024-01-15T12:30:00Z",
        assetTypeName = "Компьютер",
        location = null,
        users = null,
        parent = null
    )
}

private fun getSampleScrappedAssetResponseDto(): AssetResponseDto {
    return getSampleFullAssetResponseDto().copy(
        name = "Старый монитор Dell",
        inventoryId = "INV-2020-9999",
        serialNumber = "DL-999888",
        assetStatus = "Списан",
        assetTypeName = "Периферия",
        parentName = null
    )
}