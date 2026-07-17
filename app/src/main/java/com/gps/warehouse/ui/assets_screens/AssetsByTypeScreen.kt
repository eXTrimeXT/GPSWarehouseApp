package com.gps.warehouse.ui.assets_screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsByTypeScreen(
    assetTypeId: Int?, // null для "Других"
    assetTypeName: String,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Состояния фильтров
    var searchQuery by remember { mutableStateOf("") }
    var inventoryId by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var assetStatus by remember { mutableStateOf<String?>(null) }
    var modelId by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }

    var isFiltersExpanded by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }

    // Триггер загрузки при изменении фильтров или страницы
    LaunchedEffect(searchQuery, inventoryId, serialNumber, assetStatus, modelId, parentId, locationId, currentPage, assetTypeId) {
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

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = assetTypeName,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Поисковая строка с кнопкой фильтров
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; currentPage = 1 },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Поиск по названию") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isFiltersExpanded = !isFiltersExpanded }) {
                        Icon(
                            imageVector = if (isFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.FilterList,
                            contentDescription = "Фильтры"
                        )
                    }
                },
                singleLine = true
            )

            // Раскрывающиеся фильтры
            if (isFiltersExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = inventoryId,
                            onValueChange = { inventoryId = it; currentPage = 1 },
                            label = { Text("Инвентарный номер") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = serialNumber,
                            onValueChange = { serialNumber = it; currentPage = 1 },
                            label = { Text("Серийный номер") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Выпадающий список статусов
                        var statusExpanded by remember { mutableStateOf(false) }
                        val statuses = listOf("Приемка", "В эксплуатации", "Списан", "Ремонт")
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded }
                        ) {
                            OutlinedTextField(
                                value = assetStatus ?: "Любой статус",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Статус") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Любой статус") },
                                    onClick = { assetStatus = null; statusExpanded = false; currentPage = 1 }
                                )
                                statuses.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = { assetStatus = status; statusExpanded = false; currentPage = 1 }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = modelId,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) { modelId = it; currentPage = 1 } },
                            label = { Text("ID модели") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = parentId,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) { parentId = it; currentPage = 1 } },
                            label = { Text("ID родителя") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = locationId,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) { locationId = it; currentPage = 1 } },
                            label = { Text("ID локации") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                searchQuery = ""
                                inventoryId = ""
                                serialNumber = ""
                                assetStatus = null
                                modelId = ""
                                parentId = ""
                                locationId = ""
                                currentPage = 1
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Сбросить фильтры")
                        }
                    }
                }
            }

            // Контент списка
            when (val state = uiState) {
                is AssetViewModel.AssetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AssetViewModel.AssetUiState.AssetsLoadedPaginated -> {
                    val assets = state.assets
                    if (assets.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Активы не найдены", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(assets) { asset ->
                                AssetCardPaginated(
                                    asset = asset,
                                    onClick = { navController.navigate("asset_details/${asset.assetId}") }
                                )
                            }
                            // Кнопка пагинации
                            if (state.hasNext) {
                                item {
                                    Button(
                                        onClick = { currentPage++ },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        enabled = uiState !is AssetViewModel.AssetUiState.Loading
                                    ) {
                                        Text("Загрузить еще")
                                    }
                                }
                            }
                        }
                    }
                }
                is AssetViewModel.AssetUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { currentPage = 1 }) { Text("Повторить") }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AssetCardPaginated(
    asset: AssetResponseDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                        "приемка", "в эксплуатации", "active" -> MaterialTheme.colorScheme.primary
                        "списан", "inactive" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            asset.parentName?.let { parentName ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "В составе: $parentName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// PREVIEW ФУНКЦИИ И MOCK ДАННЫЕ
// ============================================================================

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

@Preview(showBackground = true, name = "Карточка актива (Минимальная / Без типа)")
@Composable
fun AssetCardPaginatedPreview_Minimal() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetCardPaginated(
                asset = getSampleMinimalAssetResponseDto(),
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (Статус: Списан)")
@Composable
fun AssetCardPaginatedPreview_Scrapped() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetCardPaginated(
                asset = getSampleScrappedAssetResponseDto(),
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

// ============================================================================
// MOCK ДАННЫЕ
// ============================================================================

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

private fun getSampleMinimalAssetResponseDto(): AssetResponseDto {
    return AssetResponseDto(
        assetId = 99,
        name = "Тестовый актив",
        inventoryId = "TEST-001",
        serialNumber = null,
        assetStatus = "Приемка",
        comment = null,
        dateIssue = null,
        datePurchasing = null,
        modelId = null,
        modelName = null,
        assetTypeId = null,
        parentId = null,
        locationId = null,
        preparedBy = null,
        checkedBy = null,
        parentName = null,
        manufacturerName = null,
        vendorName = null,
        osName = null,
        createdBy = null,
        updatedBy = null,
        createdAt = "2024-01-01T10:00:00Z",
        updatedAt = null,
        assetTypeName = null,
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