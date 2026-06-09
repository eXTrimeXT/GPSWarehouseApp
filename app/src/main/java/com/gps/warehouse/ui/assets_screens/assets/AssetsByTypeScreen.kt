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

    LaunchedEffect(typeDomain) {
        // Если typeDomain == "others", загружаем активы без типа
        val typeAsset = if (typeDomain == "others") null else typeDomain
        viewModel.loadAssetsByType(typeAsset)
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
                placeholder = { Text("Поиск по имени или инвентарному номеру") },
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
                    val filteredAssets = state.assets.filter {
                        searchQuery.isEmpty() ||
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.inventoryId.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredAssets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Активы не найдены",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                            Button(onClick = {
                                val typeAsset = if (typeDomain == "others") null else typeDomain
                                viewModel.loadAssetsByType(typeAsset)
                            }) {
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