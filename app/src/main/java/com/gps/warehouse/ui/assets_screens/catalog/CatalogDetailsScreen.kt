package com.gps.warehouse.ui.assets_screens.catalog

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
import com.gps.warehouse.data.remote.assets_dto.AssetCatalogDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.assets_screens.assets.DetailRow
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogDetailsScreen(
    catalogId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(catalogId) {
        viewModel.loadCatalogItemDetails(catalogId)
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Детали каталога",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.CatalogItemDetailsLoaded -> {
                CatalogDetailsContent(
                    catalogItem = state.catalogItem,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadCatalogItemDetails(catalogId) }) {
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
fun CatalogDetailsContent(
    catalogItem: AssetCatalogDto,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Основная информация о записи каталога
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Информация о записи", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("ID записи", catalogItem.catalogId.toString())
                catalogItem.assetId?.let { DetailRow("ID актива", it.toString()) }
                catalogItem.androidId?.let { DetailRow("Android ID", it) }
                catalogItem.ownerId?.let { DetailRow("ID владельца", it.toString()) }
                DetailRow("Создано", catalogItem.createdAt)
            }
        }

        // 2. Владелец
        catalogItem.owner?.let { owner ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Владелец", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("ФИО", owner.owner)
                    DetailRow("Email", owner.email)
                    owner.userPosition?.let { DetailRow("Должность", it) }
                }
            }
        }

        // 3. Информация об активе (если есть)
        catalogItem.asset?.let { asset ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Информация об активе", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Название", asset.name)
                    DetailRow("Инвентарный номер", asset.inventoryId)
                    asset.serialNumber?.let { DetailRow("Серийный номер", it) }
                    DetailRow("Статус", asset.assetStatus)
                    asset.infoStorageLocation?.let { DetailRow("Место хранения", it) }
                    asset.dateIssue?.let { DetailRow("Дата ввода в эксплуатацию", it) }
                    asset.datePurchasing?.let { DetailRow("Дата покупки", it) }
                    asset.comment?.let { DetailRow("Комментарий", it) }

                    asset.model?.let { model ->
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Модель", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))

                        DetailRow("Название модели", model.modelName)
                        model.description?.let { DetailRow("Описание", it) }

                        model.assetClass?.let { assetClass ->
                            DetailRow("Класс", assetClass.className)
                            assetClass.assetType?.let { type -> DetailRow("Тип", type.name) }
                        }
                    }

                    asset.warehouse?.let { warehouse ->
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Склад", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))

                        DetailRow("Название", warehouse.name)
                        warehouse.location?.let { location ->
                            DetailRow("Город", location.city)
                            DetailRow("Адрес", location.address)
                            location.room?.let { DetailRow("Помещение", it) }
                        }
                    }
                }
            }
        }

        // 4. Данные Android устройства (если есть)
        catalogItem.androidData?.let { androidData ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Данные Android устройства", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Устройство
                    Text("Устройство", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Имя", androidData.device?.name)
                    DetailRow("Модель", androidData.device?.model)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Система
                    Text("Система", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Версия Android", androidData.system?.androidVersion)
                    DetailRow("API уровень", androidData.system?.androidApiVersion)
                    DetailRow("Номер сборки", androidData.system?.buildNumber)
                    DetailRow("Язык", androidData.system?.language)
                    DetailRow("Часовой пояс", androidData.system?.timezone)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Железо
                    Text("Железо", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Процессор", androidData.hardware?.processor)
                    DetailRow("Архитектура", androidData.hardware?.processorArchitecture)
                    DetailRow("ОЗУ (Всего)", androidData.hardware?.ramTotal)
                    DetailRow("ОЗУ (Свободно)", androidData.hardware?.ramFree)
                    DetailRow("Хранилище (Всего)", androidData.hardware?.storageTotal)
                    DetailRow("Хранилище (Свободно)", androidData.hardware?.storageFree)
                    DetailRow("Разрешение экрана", androidData.hardware?.screenResolution)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Сеть
                    Text("Сеть", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Тип подключения", androidData.network?.connectionType)
                    DetailRow("Wi-Fi SSID", androidData.network?.wifiSsid)
                    DetailRow("MAC адрес", androidData.network?.macAddress)
                    DetailRow("IP адреса", androidData.network?.ipAddresses)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Батарея
                    Text("Батарея", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Уровень заряда", androidData.battery?.level)
                    DetailRow("Статус", androidData.battery?.status)
                    DetailRow("Температура", androidData.battery?.temperature)
                }
            }
        }
    }
}