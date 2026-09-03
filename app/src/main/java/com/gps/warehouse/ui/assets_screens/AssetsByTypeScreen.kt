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
import com.gps.warehouse.data.remote.assets_dto.AssetLocationResponse
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetStatusDto
import com.gps.warehouse.data.remote.assets_dto.AssetUserFullResponse
import com.gps.warehouse.data.remote.assets_dto.PositionResponse
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
                    text = asset.assetStatus.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (asset.assetStatus?.lowercase()) {
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

// PREVIEW ФУНКЦИИ
@Preview(showBackground = true, name = "Экран: Список активов")
@Composable
fun AssetsByTypeScreenContentPreview_Loaded() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetsByTypeScreenContent(
                assetTypeName = "Компьютеры",
                uiState = AssetViewModel.AssetUiState.AssetsLoadedPaginated(
                    assets = listOf(
                        getSampleAsset(),
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
                asset = getSampleAsset(),
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
                asset = getSampleAsset().copy(
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
private fun getSampleAsset(): AssetResponseDto {
    return AssetResponseDto(
        assetId = 48,
        name = "Актив 2",
        inventoryId = "INV_NUMBER_48",
        serialNumber = "SER_NUMBER_48",
        assetStatus = "В работе",
        assetStatusId = 10,
        comment = "Описание123123",
        dateIssue = "2026-08-19",
        datePurchasing = null,
        modelId = null,
        modelName = "модель 3",
        assetTypeId = 10,
        parentId = null,
        locationId = null,
        quantity = 120,
        preparedBy = null,
        checkedBy = null,
        parentName = "чего то там",
        manufacturerName = "китай",
        vendorName = "Z",
        osName = "окнО",

        // ✅ Сервисная информация
        everyWeekCheck = false,
        nextService = "2026-09-08",
        servicePeriod = 5,

        // ✅ Мета
        createdBy = "0000012657",
        updatedBy = "0000015370",
        createdAt = "2026-07-14T19:23:04.784110",
        updatedAt = "2026-09-03T09:26:16.840853",
        assetTypeName = "Оборудование MU",

        // ✅ Вложенные объекты
        location = AssetLocationResponse(
            workshopId = 6,
            workshopName = "Логистика",
            place = "mesto213111111111111",
            level = 4,
            x = 237,
            y = 415
        ),

        users = listOf(
            AssetUserFullResponse(
                guid = "974f470d-a7cd-11ef-a3b2-000c290ca5c4",
                employeeId = "0000010680",
                birthDate = "1994-12-30",
                employmentDate = "2024-11-21",
                dismissalDate = null,
                phone = "+79805882104",
                email = "Andrey.Malykh@hmmr.ru",
                comment = null,
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "80911547-78ee-11f0-a3c1-000c290ca5c4",
                createdAt = "2026-07-08T14:17:34.650680",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Малых Андрей Владимирович",
                fullNameEn = "Malykh Andrey Vladimirovich",
                society = null,
                department = null,
                division = null,
                group = null,
                position = PositionResponse(name = "Системный администратор", nameEn = null),
                startDate = "2026-08-24",
                endDate = null,
                assignmentType = "user"
            )
        ),

        responsibleUsers = listOf(
            AssetUserFullResponse(
                guid = "14ba77ab-2d91-11f1-a3cb-000c290ca5c4",
                employeeId = "0000015370",
                birthDate = "2002-09-06",
                employmentDate = "2026-04-01",
                dismissalDate = null,
                phone = "+79190809746",
                email = "Timur.Malyshev@hmmr.ru",
                comment = "Проверка",
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "6334328f-f69a-11f0-a3c7-000c290ca5c4",
                createdAt = "2026-07-08T14:17:44.545594",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Малышев Тимур Максимович",
                fullNameEn = "Malyshev Timur Maksimovich",
                society = null,
                department = null,
                division = null,
                group = null,
                position = PositionResponse(name = "Инженер", nameEn = null),
                startDate = "2026-08-26",
                endDate = null,
                assignmentType = "responsible"
            )
        ),

        servingUsers = listOf(
            AssetUserFullResponse(
                guid = "c0ed588f-1c4a-11f1-a3ca-000c290ca5c4",
                employeeId = "0000014942",
                birthDate = "2003-04-19",
                employmentDate = "2026-03-10",
                dismissalDate = null,
                phone = "+79805882044",
                email = "Oleg.Feshchenko@hmmr.ru",
                comment = null,
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "6334328f-f69a-11f0-a3c7-000c290ca5c4",
                createdAt = "2026-07-08T14:17:43.360340",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Фещенко Олег Игоревич",
                fullNameEn = "Feshchenko Oleg Igorevich",
                society = null,
                department = null,
                division = null,
                group = null,
                position = PositionResponse(name = "Техник", nameEn = null),
                startDate = "2026-09-02",
                endDate = null,
                assignmentType = "serving"
            )
        ),

        // ✅ Текущий пользователь
        currentUser = "0000012657",
        currentUserFullName = "Евсиков Константин Александрович",

        // ✅ Родительский актив
        parent = null
    )
}

private fun getSampleScrappedAssetResponseDto(): AssetResponseDto {
    return getSampleAsset().copy(
        name = "Старый монитор Dell",
        inventoryId = "INV-2020-9999",
        serialNumber = "DL-999888",
        assetStatus = "Списан",
        assetTypeName = "Периферия",
        parentName = null
    )
}