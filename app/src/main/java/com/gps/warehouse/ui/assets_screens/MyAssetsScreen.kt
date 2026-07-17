package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.MyAssetDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAssetsScreen(
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState() 

    LaunchedEffect(Unit) {
        viewModel.loadMyAssets()
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Мои активы",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.MyAssetsLoaded -> {
                if (state.assets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Laptop,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "За вами не закреплено активов",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.assets) { asset ->
                            MyAssetCard(
                                asset = asset,
                                onClick = {
                                    navController.navigate("my_asset_details/${asset.assetId}")
                                }
                            )
                        }
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMyAssets() }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

// Замените MyAssetCard на этот вариант:
@Composable
fun MyAssetCard(
    asset: MyAssetDto,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
fun MyAssetCardPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyAssetCard(
                asset = getSampleFullMyAssetDto(),
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (Минимальная / Без типа)")
@Composable
fun MyAssetCardPreview_Minimal() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyAssetCard(
                asset = getSampleMinimalMyAssetDto(),
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (С родительским активом)")
@Composable
fun MyAssetCardPreview_WithParent() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyAssetCard(
                asset = getSampleFullMyAssetDto().copy(
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

private fun getSampleFullMyAssetDto(): MyAssetDto {
    return MyAssetDto(
        assetId = 2,
        name = "Ноутбук Lenovo ThinkPad X1",
        inventoryId = "INV-2024-00158",
        serialNumber = "SN-9876543210",
        assetStatus = "В эксплуатации",
        assetTypeName = "Компьютер",
        modelName = "ThinkPad X1 Carbon Gen 11",
        comment = "Выдан системному администратору",
        dateIssue = "2024-01-15",
        datePurchasing = "2024-01-10",
        parentName = "Рабочая станция №12",
        location = null,
        users = null,
        parent = null,
        createdAt = "",
        updatedAt = ""
    )
}

private fun getSampleMinimalMyAssetDto(): MyAssetDto {
    return MyAssetDto(
        assetId = 99,
        name = "Тестовый актив",
        inventoryId = "TEST-001",
        serialNumber = null,
        assetStatus = "Приемка",
        assetTypeName = null,
        modelName = null,
        comment = null,
        dateIssue = null,
        datePurchasing = null,
        parentName = null,
        location = null,
        users = null,
        parent = null,
        createdAt = "",
        updatedAt = ""
    )
}