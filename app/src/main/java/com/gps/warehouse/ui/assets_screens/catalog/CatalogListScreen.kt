package com.gps.warehouse.ui.assets_screens.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AndroidDataDto
import com.gps.warehouse.data.remote.assets_dto.AssetCatalogDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

// ==================== 1. РЕАЛЬНЫЙ ЭКРАН ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogListScreen(
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val catalogState by viewModel.catalogUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCatalog()
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Каталог активов",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        CatalogListContent(
            catalogState = catalogState,
            onItemClick = { catalogId ->
                navController.navigate("catalog_details/$catalogId")
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

// ==================== 2. ЧИСТЫЙ UI КОМПОНЕНТ ====================
@Composable
fun CatalogListContent(
    catalogState: AssetViewModel.CatalogUiState,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (val state = catalogState) {
        is AssetViewModel.CatalogUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AssetViewModel.CatalogUiState.Loaded -> {
            if (state.items.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Book,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Каталог пуст",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items) { item ->
                        CatalogItemCard(
                            catalogItem = item,
                            onClick = { onItemClick(item.catalogId) }
                        )
                    }
                }
            }
        }
        is AssetViewModel.CatalogUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Retry callback should be passed */ }) {
                        Text("Повторить")
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun CatalogItemCard(
    catalogItem: AssetCatalogDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка меняется в зависимости от типа записи
            val isAndroid = catalogItem.androidData != null && catalogItem.asset == null
            val icon = if (isAndroid) Icons.Default.Android else Icons.Default.Book
            val iconTint = if (isAndroid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer

            Surface(
                shape = MaterialTheme.shapes.small,
                color = (if (isAndroid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer).copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Умное определение заголовка
                val title = when {
                    catalogItem.asset != null -> catalogItem.asset.name ?: "Актив #${catalogItem.assetId}"
                    catalogItem.androidData?.device?.name != null -> catalogItem.androidData.device.name ?: "Android устройство"
                    else -> "Запись каталога #${catalogItem.catalogId}"
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Серийный номер теперь берётся из корневого объекта
                catalogItem.serialNumber?.let { sn ->
                    Text(
                        text = "S/N: $sn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Подзаголовок: Инв. номер или Модель
                val subtitle = when {
                    catalogItem.asset != null -> "Инв. номер: ${catalogItem.asset.inventoryId ?: "Не указан"}"
                    catalogItem.androidData != null -> "Модель: ${catalogItem.androidData.device?.model ?: "Не указана"}"
                    else -> "ID: ${catalogItem.catalogId}"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                catalogItem.owner?.let { owner ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Владелец: ${owner.owner}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== 3. РАБОЧИЕ ПРЕВЬЮ ====================

@Preview(showBackground = true, name = "Catalog - Empty")
@Composable
fun CatalogListPreviewEmpty() {
    MaterialTheme {
        Surface {
            CatalogListContent(
                catalogState = AssetViewModel.CatalogUiState.Loaded(emptyList()),
                onItemClick = {},
                modifier = Modifier
            )
        }
    }
}

@Preview(showBackground = true, name = "Catalog - Loading")
@Composable
fun CatalogListPreviewLoading() {
    MaterialTheme {
        Surface {
            CatalogListContent(
                catalogState = AssetViewModel.CatalogUiState.Loading,
                onItemClick = {},
                modifier = Modifier
            )
        }
    }
}

@Preview(showBackground = true, name = "Catalog - Error")
@Composable
fun CatalogListPreviewError() {
    MaterialTheme {
        Surface {
            CatalogListContent(
                catalogState = AssetViewModel.CatalogUiState.Error("Не удалось загрузить каталог"),
                onItemClick = {},
                modifier = Modifier
            )
        }
    }
}

@Preview(showBackground = true, name = "Catalog - With Items")
@Composable
fun CatalogListPreviewWithItems() {
    // Создаём минимальные моковые объекты
    // Все поля, которые могут быть null — передаём null
    val mockItems = listOf(
        AssetCatalogDto(
            catalogId = 1,
            assetId = 101,
            serialNumber = "SN-001-ABC",
            asset = null,
            androidData = null,
            owner = null,
            createdAt = "2026-05-20 10:00:00",
            ownerId = 5,
            creator = null
        ),
        AssetCatalogDto(
            catalogId = 2,
            assetId = null,
            serialNumber = "SN-002-XYZ",
            asset = null,
            androidData = AndroidDataDto(
                id = 1,
                device = null,
                system = null,
                hardware = null,
                network = null,
                battery = null
            ),
            owner = null,
            createdAt = "2026-05-20 11:00:00",
            ownerId = null,
            creator = null
        )
    )

    MaterialTheme {
        Surface {
            CatalogListContent(
                catalogState = AssetViewModel.CatalogUiState.Loaded(mockItems),
                onItemClick = {},
                modifier = Modifier
            )
        }
    }
}