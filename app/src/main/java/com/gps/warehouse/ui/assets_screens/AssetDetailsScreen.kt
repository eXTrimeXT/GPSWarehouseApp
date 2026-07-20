package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetParentResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetUserResponseDto
import com.gps.warehouse.data.remote.assets_dto.LocationResponseDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsScreen(
    assetId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
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
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
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
                ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadAssetDetails(assetId) }
                )
            }
            else -> {}
        }
    }
}

@Composable
fun AssetDetailsContent(asset: AssetResponseDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Основная информация
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Название", asset.name)
                DetailRow("Инвентарный номер", asset.inventoryId)
                DetailRow("Серийный номер", asset.serialNumber)
                DetailRow("Статус", asset.assetStatus)
                DetailRow("Тип актива", asset.assetTypeName)
                DetailRow("Модель", asset.modelName)
                asset.comment?.let { DetailRow("Комментарий", it) }
            }
        }

        // Даты и создание
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Информация о создании", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Дата ввода в эксплуатацию", asset.dateIssue)
                DetailRow("Дата покупки", asset.datePurchasing)
                DetailRow("Создано", asset.createdAt)
                DetailRow("Обновлено", asset.updatedAt)
                DetailRow("Создал", asset.createdBy)
                DetailRow("Обновил", asset.updatedBy)
            }
        }

        // Локация (если есть)
        asset.location?.let { loc ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Локация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("Название", loc.name)
                    DetailRow("Адрес", loc.address)
                }
            }
        }

        // Пользователи (если есть)
        if (!asset.users.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Закреплённые пользователи", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    asset.users.forEach { user ->
                        DetailRow("Сотрудник", user.fullNameRu)
                        DetailRow("Табельный номер", user.employeeId)
                        DetailRow("Дата начала", user.startDate)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        // Родительский актив (если есть)
        asset.parent?.let { parent ->
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Родительский актив", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("Название", parent.name)
                    DetailRow("Инвентарный номер", parent.inventoryId)
                    DetailRow("Серийный номер", parent.serialNumber)
                    DetailRow("Тип", parent.assetTypeName)
                    DetailRow("Статус", parent.assetStatus)
                }
            }
        }
    }
}


// PREVIEW ФУНКЦИИ И MOCK ДАННЫЕ
@Preview(showBackground = true, name = "Детали актива (Полные)", device = "spec:width=380dp,height=1270dp")
@Composable
fun AssetDetailsContentPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetDetailsContent(
                asset = getSampleFullAssetResponseDto(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// MOCK ДАННЫЕ
private fun getSampleFullAssetResponseDto(): AssetResponseDto {
    return AssetResponseDto(
        assetId = 1,
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
        parentId = 2,
        locationId = 1,
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
        location = LocationResponseDto(
            locationId = 1,
            name = "Центральный склад",
            address = "г. Тула, ул. Ленина, д. 10"
        ),
        users = listOf(
            AssetUserResponseDto(
                guid = "14ba77ab-2d91-11f1-a3cb-000c290ca5c4",
                employeeId = "0000015370",
                fullNameRu = "Малышев Тимур Максимович",
                fullNameEn = "Malyshev Timur Maksimovich",
                startDate = "2024-01-15",
                endDate = null
            )
        ),
        parent = AssetParentResponseDto(
            assetId = 2,
            name = "Рабочая станция №12",
            inventoryId = "INV-WS-12",
            serialNumber = "SN-WS-12",
            assetStatus = "В эксплуатации",
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
            assetTypeName = "Рабочая станция"
        )
    )
}