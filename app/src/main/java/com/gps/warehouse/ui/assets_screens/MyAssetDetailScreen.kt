package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetLocationResponse
import com.gps.warehouse.data.remote.assets_dto.AssetUserFullResponse
import com.gps.warehouse.data.remote.assets_dto.MyAssetDto
import com.gps.warehouse.data.remote.assets_dto.PositionResponse
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAssetDetailScreen(
    assetId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(assetId) {
        viewModel.loadMyAssetDetails(assetId)
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
            is AssetViewModel.AssetUiState.MyAssetDetailsLoaded -> {
                MyAssetDetailContent(
                    asset = state.asset,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is AssetViewModel.AssetUiState.Error -> {
                ErrorStateView(
                    message = state.message,
                    onRetry = { viewModel.loadMyAssetDetails(assetId) }
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun MyAssetDetailContent(
    asset: MyAssetDto,
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Основная информация",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Название", asset.name)
                DetailRow("Инвентарный номер", asset.inventoryId)
                DetailRow("Серийный номер", asset.serialNumber)
                DetailRow("Тип актива", asset.assetTypeName)
                DetailRow("Модель", asset.modelName)
                DetailRow("Статус", asset.assetStatus)
                DetailRow("Комментарий", asset.comment)
                DetailRow("ID актива", asset.assetId.toString())
            }
        }

        // Даты
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Даты",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Дата ввода в эксплуатацию", asset.dateIssue)
                DetailRow("Дата покупки", asset.datePurchasing)
                DetailRow("Создано", asset.createdAt)
                DetailRow("Обновлено", asset.updatedAt)
            }
        }

        // Локация
        val location: AssetLocationResponse? = asset.location
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Локация",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

//                DetailRow("Город", location?.city)
//                DetailRow("Адрес", location?.address)
//                DetailRow("Помещение", location?.room)
                DetailRow("workshop_id", location?.workshopId.toString())
                DetailRow("Место", location?.place)
                DetailRow("Этаж", location?.level.toString())
            }
        }

        // Родительский актив
        asset.parent?.let { parent ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Родительский актив",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow("Название", parent.name)
                    DetailRow("Инвентарный номер", parent.inventoryId)
                    DetailRow("Серийный номер", parent.serialNumber)
                    DetailRow("Тип актива", parent.assetTypeName)
                    DetailRow("Модель", parent.modelName)
                    DetailRow("Статус", parent.assetStatus)
                }
            }
        }

        // Закреплённые пользователи
        if (!asset.users.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Закреплённые пользователи (${asset.users.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    asset.users.forEachIndexed { index, user ->
                        UserCard(user = user)
                        if (index < asset.users.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(user: AssetUserFullResponse) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = user.fullNameRu,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (user.fullNameEn.isNotBlank()) {
            Text(
                text = user.fullNameEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        DetailRow("Табельный номер", user.employeeId)
        DetailRow("Дата начала", user.startDate)
        DetailRow("Дата окончания", user.endDate)
    }
}

// ============================================================================
// PREVIEW ФУНКЦИИ
// ============================================================================

@Preview(showBackground = true, showSystemUi = true, name = "Детали актива (Полный)")
@Composable
fun MyAssetDetailContentPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MyAssetDetailContent(asset = getSampleFullMyAssetDto())
        }
    }
}

@Preview(showBackground = true, name = "Детали актива (Минимум)")
@Composable
fun MyAssetDetailContentPreview_Minimal() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MyAssetDetailContent(asset = getSampleMinimalMyAssetDto())
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (Полная)")
@Composable
fun MyAssetCardPreview_Full_Detail() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyAssetCard(asset = getSampleFullMyAssetDto())
        }
    }
}

@Preview(showBackground = true, name = "Карточка актива (Минимальная)")
@Composable
fun MyAssetCardPreview_Minimal_Detail() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyAssetCard(asset = getSampleMinimalMyAssetDto())
        }
    }
}

// ============================================================================
// MOCK ДАННЫЕ
// ============================================================================

fun getSampleFullMyAssetDto(): MyAssetDto {
    return MyAssetDto(
        assetId = 2,
        name = "Ноутбук",
        inventoryId = "инвентарный номер ноута",
        serialNumber = "серийный номер ноута",
        assetStatus = "Приемка",
        assetStatusId = 1,
        comment = "коммент для ноута",
        dateIssue = "2026-07-08",
        datePurchasing = "2026-07-08",
        modelName = "Xiaomi Redmi Notebook T",
        assetTypeName = "Компьютер",
        parentName = "Актив №1",
        createdAt = "2026-07-08T14:19:56.207098",
        updatedAt = "2026-07-14T14:09:30.775238",
        location = AssetLocationResponse(
            workshopId = 1,
            workshopName = "Цех сборки",
            place = "Место",
            level = 3,
            x = 0,
            y = 0
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
                position = PositionResponse(name = "Пользователь", nameEn = null),
                startDate = "2026-08-24",
                endDate = null,
                assignmentType = "user"
            ),
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
                position = PositionResponse(name = "Администратор", nameEn = null),
                startDate = "2026-08-26",
                endDate = null,
                assignmentType = "responsible"
            )
        ),
        parent = null
//        parent = ParentAssetDto(
//            assetId = 4,
//            name = "Актив №1",
//            inventoryId = "1111111111",
//            serialNumber = "001111",
//            assetStatus = "Приемка",
//            assetTypeName = "Оборудование MU",
//            modelName = "Модель 11"
//        )
    )
}

private fun getSampleMinimalMyAssetDto(): MyAssetDto {
    return MyAssetDto(
        assetId = 99,
        name = "Тестовый актив",
        inventoryId = "TEST-001",
        serialNumber = null,
        assetStatus = "Приемка",
        assetStatusId = 1,
        comment = null,
        dateIssue = null,
        datePurchasing = null,
        modelName = null,
        assetTypeName = null,
        parentName = null,
        createdAt = "2026-07-14T10:00:00Z",
        updatedAt = null,
        location = null,
        users = null,
        parent = null
    )
}