package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsScreen(
    assetId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(assetId) {
        viewModel.loadAssetDetails(assetId)
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Детали актива",
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
            is AssetViewModel.AssetUiState.AssetDetailsLoaded -> {
                AssetDetailsContent(
                    asset = state.asset,
                    modifier = Modifier.padding(paddingValues)
                )
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
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAssetDetails(assetId) }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AssetDetailsContent(
    asset: AssetDto,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Основная информация
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Основная информация",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Название", asset.name)
                DetailRow("Инвентарный номер", asset.inventoryId)
                DetailRow("Серийный номер", asset.serialNumber)
                DetailRow("Статус", asset.assetStatus)
                asset.typeDomain?.let { DetailRow("Домен типа", it) }
                asset.comment?.let { DetailRow("Комментарий", it) }
            }
        }

        // Модель
        asset.model?.let { model ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Модель",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("Название модели", model.modelName)
                    DetailRow("Активна", if (model.isActive) "Да" else "Нет")
                    DetailRow("Серийный номер обязателен", if (model.isSerialRequired) "Да" else "Нет")
                    model.description?.let { DetailRow("Описание", it) }

                    model.assetClass?.let { assetClass ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Класс: ${assetClass.className}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        assetClass.assetType?.let { type ->
                            Text(
                                text = "Тип: ${type.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Расположение и склад
        asset.warehouse?.let { warehouse ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Расположение",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("Склад", warehouse.name)
                    warehouse.location?.let { location ->
                        DetailRow("Местоположение", location.name)
                        location.address?.let { DetailRow("Адрес", it) }
                    }
                    asset.infoStorageLocation?.let { DetailRow("Место хранения", it) }
                }
            }
        }

        // Ответственные лица
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Ответственные лица",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                asset.preparer?.let { preparer ->
                    DetailRow("Подготовил", preparer.owner)
                    preparer.userPosition?.let { DetailRow("Должность", it) }
                    DetailRow("Email", preparer.email)
                }
                asset.checker?.let { checker ->
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Проверил", checker.owner)
                    checker.userPosition?.let { DetailRow("Должность", it) }
                    DetailRow("Email", checker.email)
                }
            }
        }

        // Поставщик и производитель
        if (asset.manufacturer != null || asset.vendor != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Поставщик и производитель",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    asset.manufacturer?.let { manufacturer ->
                        DetailRow("Производитель", manufacturer.name)
                    }
                    asset.vendor?.let { vendor ->
                        DetailRow("Поставщик", vendor.name)
                    }
                }
            }
        }

        // Даты
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Даты",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                asset.dateIssue?.let { DetailRow("Дата ввода в эксплуатацию", it) }
                asset.datePurchasing?.let { DetailRow("Дата покупки", it) }
                DetailRow("Создано", asset.createdAt)
                asset.updatedAt?.let { DetailRow("Обновлено", it) }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}