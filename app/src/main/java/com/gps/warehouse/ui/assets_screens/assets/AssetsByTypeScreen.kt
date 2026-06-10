package com.gps.warehouse.ui.assets_screens.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetShortDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsByTypeScreen(
    typeDomain: String,
    navController: NavHostController,
    viewModel: AssetViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Загружаем ВСЕ активы (фильтрация будет на клиенте)
    LaunchedEffect(typeDomain) {
        viewModel.loadAssets()
    }

    val typeName = if (typeDomain == "others") "Другие" else getAssetTypeDisplayName(typeDomain)

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = typeName,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поисковая строка
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Поиск") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            when (val state = uiState) {
                is AssetViewModel.AssetUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is AssetViewModel.AssetUiState.AssetsLoaded -> {
                    // КЛЮЧЕВАЯ ФИЛЬТРАЦИЯ по type_asset
                    val filteredByType = state.assets.filter { asset ->
                        if (typeDomain == "others") {
                            asset.typeAsset == null  // Активы БЕЗ типа
                        } else {
                            asset.typeAsset == typeDomain  // Активы с нужным типом
                        }
                    }

                    // Дополнительная фильтрация по поисковому запросу
                    val filteredAssets = filteredByType.filter {
                        searchQuery.isEmpty() ||
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.inventoryId.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredAssets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Активы не найдены",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Попробуйте изменить поисковый запрос",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Счётчик найденных активов
                        Text(
                            text = "Найдено: ${filteredAssets.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAssets) { asset ->
                                AssetCard(
                                    asset = asset,
                                    onClick = {
                                        navController.navigate("asset_details/${asset.assetId}")
                                    }
                                )
                            }
                        }
                    }
                }
                is AssetViewModel.AssetUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadAssets() }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

fun getAssetTypeDisplayName(typeDomain: String): String {
    return when (typeDomain) {
        "computer" -> "Компьютеры"
        "mes_equipment" -> "MES оборудование"
        "supplies" -> "Расходные материалы"
        "power_adapter" -> "Блоки питания"
        "data_collection_equipment" -> "Терминалы сбора данных"
        "Accessories" -> "Комплектующие"
        "network_equipment" -> "Сетевое оборудование"
        "printing_equipment" -> "Печатающее оборудование"
        "server_hardware" -> "Серверное оборудование"
        else -> typeDomain
    }
}

@Composable
fun AssetCard(
    asset: AssetShortDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = asset.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Инв. номер: ${asset.inventoryId}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!asset.serialNumber.isNullOrBlank()) {
                Text(
                    text = "Серийный номер: ${asset.serialNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Показываем тип актива, если он есть
            if (!asset.typeAsset.isNullOrBlank()) {
                Text(
                    text = "Тип: ${getAssetTypeDisplayName(asset.typeAsset)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Статус: ${asset.assetStatus}",
                    style = MaterialTheme.typography.labelMedium,
                    color = when (asset.assetStatus.lowercase()) {
                        "active", "активен", "в эксплуатации" -> MaterialTheme.colorScheme.primary
                        "inactive", "неактивен", "списан" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Подробнее"
                )
            }
        }
    }
}


// ================
// PREVIEW ФУНКЦИИ
@Preview(showBackground = true, name = "Карточка актива (Заполненная)")
@Composable
fun AssetCardPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetCard(
                asset = AssetShortDto(
                    assetId = 1,
                    name = "ПК на Arch Linux",
                    inventoryId = "инвентарный номер",
                    serialNumber = "серийный номер",
                    assetStatus = "В эксплуатации",
                    modelId = 1,
                    typeAsset = "computer",
                    warehouseId = 1,
                    parentId = null,
                    softwareId = 3,
                    manufacturerId = 1,
                    vendorId = 1
                ),
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (Минимальная / Другие)")
@Composable
fun AssetCardPreview_Minimal() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetCard(
                asset = AssetShortDto(
                    assetId = 2,
                    name = "Куртка лето 88-92/182-188",
                    inventoryId = "110000004635",
                    serialNumber = null,
                    assetStatus = "Приемка",
                    modelId = null,
                    typeAsset = null,
                    warehouseId = null,
                    parentId = null,
                    softwareId = null,
                    manufacturerId = null,
                    vendorId = null
                ),
                onClick = { }
            )
        }
    }
}