package com.gps.warehouse.ui.assets_screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetStatusDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.InventoryQrParser
import com.gps.warehouse.utils.ScannerManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
                    Toast.makeText(context, "Серийный номер '$parseSerialNumber' не найден в списке", Toast.LENGTH_SHORT).show()
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
    DisposableEffect(Unit) {
        scannerManager.init()
        onDispose { scannerManager.release() }
    }

    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                showCameraDialog = false
            }
        )
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
            searchQuery = ""
            inventoryId = ""
            serialNumber = ""
            assetStatus = null
            modelId = ""
            parentId = ""
            locationId = ""
            currentPage = 1
        },
        onRetry = {
            currentPage = 1
            viewModel.loadAssetsByFilters(page = 1, assetTypeId = assetTypeId)
        },
        onLoadMore = {
            if (uiState is AssetViewModel.AssetUiState.AssetsLoadedPaginated) {
                val state = uiState as AssetViewModel.AssetUiState.AssetsLoadedPaginated
                if (state.hasNext) currentPage++
            }
        },
        onAssetClick = { assetId, materialId ->
            navController.navigate("asset_details?assetId=$assetId&materialId=$materialId")
        },
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
    onAssetClick: (Int?, String?) -> Unit,
    onBackClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // Флаг для защиты от повторных вызовов во время загрузки новой страницы
    var isFetching by remember { mutableStateOf(false) }

    // ЕДИНСТВЕННЫЙ и надежный триггер пагинации
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val totalItemsCount = layoutInfo.totalItemsCount
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                // Срабатываем, когда до конца списка осталось 3 элемента или меньше
                totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
            }
            .distinctUntilChanged() // Ключевой момент: не вызывает collect, если значение (true/false) не изменилось
            .collect { isNearEnd ->
                if (isNearEnd && !isFetching) {
                    val state = uiState as? AssetViewModel.AssetUiState.AssetsLoadedPaginated
                    if (state != null && state.hasNext) {
                        isFetching = true // Блокируем повторные вызовы до обновления состояния
                        onLoadMore()
                    }
                }
            }
    }

    // Сбрасываем флаг блокировки, когда состояние обновилось (данные пришли или произошла ошибка)
    LaunchedEffect(uiState) {
        if (uiState is AssetViewModel.AssetUiState.AssetsLoadedPaginated || uiState is AssetViewModel.AssetUiState.Error) {
            isFetching = false
        }
    }

    Scaffold(
        topBar = {
            // ВОЗВРАЩАЕМ MyCustomActionBar с кнопкой сканирования в параметре action
            MyCustomActionBar(
                text = assetTypeName,
                onBackClick = onBackClick,
                actionButton = {
                    if (cameraScanEnabled) {
                        IconButton(onClick = onCameraScanClick) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Сканировать камерой",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поле поиска с кнопкой фильтра
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск по названию") },
                leadingIcon = { Icon(Icons.Default.Search, "Поиск", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    IconButton(onClick = onToggleFilters) {
                        BadgedBox(
                            badge = {
                                if (inventoryId.isNotBlank() || serialNumber.isNotBlank() || assetStatus != null) {
                                    Badge(modifier = Modifier.size(8.dp))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.FilterList,
                                contentDescription = "Фильтры"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Анимированный блок фильтров
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        var statusExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                            OutlinedTextField(
                                value = assetStatus ?: "Любой статус",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Статус") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Любой статус") },
                                    onClick = { onAssetStatusChange(null); statusExpanded = false }
                                )
                                if (assetStatuses.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Загрузка...") }, onClick = {}, enabled = false)
                                } else {
                                    assetStatuses.forEach { statusDto ->
                                        DropdownMenuItem(
                                            text = { Text(statusDto.status) },
                                            onClick = { onAssetStatusChange(statusDto.status); statusExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onResetFilters,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Сбросить") }
                        }
                    }
                }
            }

            // Основной контент списка
            when (uiState) {
                is AssetViewModel.AssetUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is AssetViewModel.AssetUiState.AssetsLoadedPaginated -> {
                    val assets = uiState.assets
                    if (assets.isEmpty() && uiState.page == 1) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.Inventory,
                                    "Пусто",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Активы не найдены",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Попробуйте изменить параметры поиска или фильтры",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (assets.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Найдено: ${assets.size} из ${uiState.total}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            items(assets) { asset ->
                                AssetCardModern(asset = asset, onClick = { onAssetClick(asset.assetId, asset.materialId) })
                            }

                            if (uiState.hasNext) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                else -> {}
            }
        }
    }
}

// ИСПРАВЛЕНИЕ: ActionBar с иконкой сканирования
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetsActionBar(
    title: String,
    onBackClick: () -> Unit,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад"
                )
            }
        },
        actions = {
            if (cameraScanEnabled) {
                IconButton(onClick = onCameraScanClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Сканировать камерой",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun AssetCardModern(asset: AssetResponseDto, onClick: () -> Unit) {
    val statusColor = when (asset.assetStatus?.lowercase()) {
        "приемка", "в эксплуатации", "active", "available" -> MaterialTheme.colorScheme.primary
        "списан", "inactive", "scrapped" -> MaterialTheme.colorScheme.error
        "в ремонте", "maintenance" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusContainerColor = when (asset.assetStatus?.lowercase()) {
        "приемка", "в эксплуатации", "active", "available" -> MaterialTheme.colorScheme.primaryContainer
        "списан", "inactive", "scrapped" -> MaterialTheme.colorScheme.errorContainer
        "в ремонте", "maintenance" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

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
        Column(modifier = Modifier.padding(16.dp)) {
            // Верхняя строка: Название и Статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = asset.name ?: "Без названия",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusContainerColor,
                    modifier = Modifier.heightIn(min = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(statusColor)
                        )
                        Text(
                            text = asset.assetStatus ?: "Неизвестно",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Средняя строка: Инвентарный и серийный номера
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.QrCode,
                        "Инв. номер",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = asset.inventoryId ?: "Не указан",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (asset.inventoryId.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!asset.serialNumber.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Code,
                            "Серийный номер",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = asset.serialNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Нижняя строка: Тип и родительский элемент
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = asset.assetTypeName ?: "Без типа",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!asset.parentName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Outlined.AccountTree,
                            "В составе",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = asset.parentName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEW ФУНКЦИИ
// ==========================================
@Preview(showBackground = true, name = "Экран: Список активов (Современный)")
@Composable
fun AssetsByTypeScreenContentPreview_Loaded() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AssetsByTypeScreenContent(
                assetTypeName = "Компьютеры",
                uiState = AssetViewModel.AssetUiState.AssetsLoadedPaginated(
                    assets = listOf(
                        getSampleAsset(),
                        getSampleScrappedAssetResponseDto(),
                        getSampleAsset().copy(name = "Ноутбук Lenovo ThinkPad", inventoryId = "INV-2023-1122", assetStatus = "В эксплуатации", parentName = "Склад №2"),
                        getSampleAsset().copy(name = "Принтер HP LaserJet", inventoryId = "INV-2022-3344", assetStatus = "В ремонте", assetTypeName = "Периферия")
                    ),
                    total = 45,
                    page = 1,
                    pageSize = 50,
                    totalPages = 1,
                    hasNext = true,
                    hasPrevious = false
                ),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                assetStatuses = listOf(
                    AssetStatusDto(1, "Приемка"),
                    AssetStatusDto(2, "В ремонте"),
                    AssetStatusDto(3, "В эксплуатации"),
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
                onAssetClick = { _, _ -> {} },
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Экран: Пустой список с фильтрами")
@Composable
fun AssetsByTypeScreenContentPreview_Empty() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                onAssetClick = { _, _ -> {} },
                onBackClick = {}
            )
        }
    }
}


private fun getSampleScrappedAssetResponseDto(): AssetResponseDto {
    return getSampleAsset().copy(
        assetId = 102,
        materialId = "MAT-002",
        name = "Старый монитор Dell",
        inventoryId = "INV-2020-9999",
        serialNumber = "DL-999888",
        assetStatus = "Списан",
        assetTypeName = "Периферия",
        parentName = null
    )
}