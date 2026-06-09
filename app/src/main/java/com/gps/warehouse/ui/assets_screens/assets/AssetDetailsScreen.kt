package com.gps.warehouse.ui.assets_screens.assets

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
import com.gps.warehouse.data.remote.assets_dto.AssetClassDto
import com.gps.warehouse.data.remote.assets_dto.AssetDto
import com.gps.warehouse.data.remote.assets_dto.AssetModelDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.CompanyDto
import com.gps.warehouse.data.remote.assets_dto.LocationDto
import com.gps.warehouse.data.remote.assets_dto.SoftwareDto
import com.gps.warehouse.data.remote.assets_dto.VendorClassDto
import com.gps.warehouse.data.remote.assets_dto.VendorDto
import com.gps.warehouse.data.remote.assets_dto.WarehouseDto
import com.gps.warehouse.ui.AssetViewModel
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
        // ==================== 1. Основная информация ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Название", asset.name)
                DetailRow("Инвентарный номер", asset.inventoryId)
                DetailRow("Серийный номер", asset.serialNumber)
                DetailRow("Статус", asset.assetStatus)
                DetailRow("Домен типа", asset.typeDomain)
                DetailRow("Прикреплённый инв. номер", asset.affixedInventoryId?.toString())
                DetailRow("Место хранения", asset.infoStorageLocation)
                DetailRow("Комментарий", asset.comment)
            }
        }

        // ==================== 2. Модель ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Модель", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Название модели", asset.model?.modelName)
                DetailRow("Активна", asset.model?.let { if (it.isActive) "Да" else "Нет" })
                DetailRow("Серийный номер обязателен", asset.model?.let { if (it.isSerialRequired) "Да" else "Нет" })
                DetailRow("Описание модели", asset.model?.description)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Класс", asset.model?.assetClass?.className)
                DetailRow("Описание класса", asset.model?.assetClass?.description)
                DetailRow("Тип актива", asset.model?.assetClass?.assetType?.name)
                DetailRow("Тип актива (EN)", asset.model?.assetClass?.assetType?.enName)
            }
        }

        // ==================== 3. Расположение и склад ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Расположение", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Склад", asset.warehouse?.name)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Страна", asset.warehouse?.location?.country)
                DetailRow("Город", asset.warehouse?.location?.city)
                DetailRow("Адрес", asset.warehouse?.location?.address)
                DetailRow("Помещение / Кабинет", asset.warehouse?.location?.room)
                DetailRow("Этаж", asset.warehouse?.location?.floor)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Место хранения (комментарий)", asset.infoStorageLocation)
            }
        }

        // ==================== 4. Ответственные лица ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ответственные лица", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Подготовил (ФИО)", asset.preparer?.owner)
                DetailRow("Должность (подготовил)", asset.preparer?.userPosition)
                DetailRow("Email (подготовил)", asset.preparer?.email)
                DetailRow("Телефон (подготовил)", asset.preparer?.phone)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Проверил (ФИО)", asset.checker?.owner)
                DetailRow("Должность (проверил)", asset.checker?.userPosition)
                DetailRow("Email (проверил)", asset.checker?.email)
                DetailRow("Телефон (проверил)", asset.checker?.phone)
            }
        }

        // ==================== 5. Поставщик и производитель ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Поставщик и производитель", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Производитель", asset.manufacturer?.name)
                DetailRow("Класс производителя", asset.manufacturer?.vendorClass?.name)
                DetailRow("Компания производителя", asset.manufacturer?.company?.name)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Поставщик", asset.vendor?.name)
                DetailRow("Класс поставщика", asset.vendor?.vendorClass?.name)
                DetailRow("Компания поставщика", asset.vendor?.company?.name)
            }
        }

        // ==================== 6. Программное обеспечение ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Программное обеспечение", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Тип ОС", asset.software?.osType)
                DetailRow("Ключ ОС", asset.software?.osKey)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Тип офисного ПО", asset.software?.officeType)
                DetailRow("Ключ офисного ПО", asset.software?.officeKey)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("ПО удалённого управления", asset.software?.remoteControl)
                DetailRow("Административные права", asset.software?.adminPermission?.let { if (it) "Да" else "Нет" })
                DetailRow("ID установившего", asset.software?.whoInstalled?.toString())
                DetailRow("Дата установки", asset.software?.installedAt)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Комментарий (ПО)", asset.software?.comment)
                DetailRow("Создано (ПО)", asset.software?.createdAt)
                DetailRow("Обновлено (ПО)", asset.software?.updatedAt)
            }
        }

        // ==================== 7. Даты ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Даты", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("Дата ввода в эксплуатацию", asset.dateIssue)
                DetailRow("Дата покупки", asset.datePurchasing)
                DetailRow("Создано", asset.createdAt)
                DetailRow("Обновлено", asset.updatedAt)
                DetailRow("Удалено", asset.deletedAt)
            }
        }

        // ==================== 8. Технические ID ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Техническая информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow("ID актива", asset.assetId.toString())
                DetailRow("ID модели", asset.modelId?.toString())
                DetailRow("ID склада", asset.warehouseId?.toString())
                DetailRow("ID родителя", asset.parentId?.toString())
                DetailRow("ID ПО", asset.softwareId?.toString())
                DetailRow("ID производителя", asset.manufacturerId?.toString())
                DetailRow("ID поставщика", asset.vendorId?.toString())
            }
        }
    }
}

/**
 * Строка детальной информации.
 * Всегда отображает label. Если value == null, выводится пустая строка "".
 */
@Composable
fun DetailRow(label: String, value: String?) {
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
            text = value ?: "",
            style = MaterialTheme.typography.bodyMedium,
            // Если значение пустое, делаем его чуть бледнее для визуального отличия, но текст остаётся ""
            color = if (value.isNullOrBlank())
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}



// ============================================================================
// PREVIEW ФУНКЦИИ
// ============================================================================
private fun getSampleFullAssetDto(): AssetDto {
    return AssetDto(
        assetId = 1042,
        name = "Ноутбук Lenovo ThinkPad X1 Carbon",
        inventoryId = "INV-2024-00158",
        serialNumber = "SN-9876543210",
        assetStatus = "В эксплуатации",
        typeDomain = "IT_Hardware",
        affixedInventoryId = "true",
        infoStorageLocation = "Шкаф №2, полка 3",
        dateIssue = "2024-01-15",
        datePurchasing = "2024-01-10",
        comment = "Выдан новому системному администратору",
        modelId = 55,
        warehouseId = 3,
        parentId = null,
        softwareId = 12,
        manufacturerId = 101,
        vendorId = 202,
        createdAt = "2024-01-01T10:00:00Z",
        updatedAt = "2024-01-15T12:30:00Z",
        deletedAt = null,

        model = AssetModelDto(
            modelId = 55,
            modelName = "ThinkPad X1 Carbon Gen 11",
            classId = 10,
            description = "Легкий бизнес-ноутбук с процессором Intel i7",
            isActive = true,
            isSerialRequired = true,
            createdAt = "2023-12-01T00:00:00Z",
            updatedAt = null,
            createdBy = 1,
            updatedBy = 1,
            assetClass = AssetClassDto(
                classId = 10,
                className = "Портативные компьютеры",
                classTypeId = 1,
                description = "Мобильные вычислительные устройства",
                createdAt = "2023-01-01T00:00:00Z",
                updatedAt = null,
                assetType = AssetTypeDto(
                    assetTypeId = 1,
                    name = "Компьютерная техника",
                    enName = "Computer Hardware"
                ),
                createdByUser = null,
                updatedByUser = null
            ),
            creator = null,
            updater = null
        ),

        warehouse = WarehouseDto(
            warehouseId = 3,
            name = "Центральный склад IT-оборудования",
            locationId = 7,
            preparedBy = 5,
            location = LocationDto(
                locationId = 7,
                country = "Россия",
                city = "Москва",
                address = "ул. Ленина, д. 10, стр. 2",
                room = "Кабинет 305",
                floor = "3"
            ),
            preparer = null
        ),
        preparer = null,
        checker = null,
        manufacturer = VendorDto(
            vendorId = 101,
            name = "Lenovo",
            vendorClassId = 1,
            companyId = 50,
            createdAt = "2020-01-01T00:00:00Z",
            createdBy = 1,
            vendorClass = VendorClassDto(
                vendorClassId = 1,
                name = "Глобальный производитель",
                description = null
            ),
            company = CompanyDto(
                companyId = 50,
                name = "Lenovo Russia LLC",
                inn = "7701234567",
                kpp = "770101001"
            ),
            creator = null
        ),

        vendor = VendorDto(
            vendorId = 202,
            name = "ООО 'ТехноПоставка'",
            vendorClassId = 2,
            companyId = 51,
            createdAt = "2021-05-10T00:00:00Z",
            createdBy = 1,
            vendorClass = VendorClassDto(vendorClassId = 2, name = "Дистрибьютор", description = null),
            company = CompanyDto(companyId = 51, name = "ООО 'ТехноПоставка'", inn = "7709876543", kpp = "770901001"),
            creator = null
        ),

        software = SoftwareDto(
            softwareId = 12,
            officeType = "Microsoft Office 2021 Professional Plus",
            officeKey = "XXXXX-XXXXX-XXXXX-XXXXX-XXXXX",
            osType = "Windows 11 Pro",
            osKey = "YYYYY-YYYYY-YYYYY-YYYYY",
            remoteControl = "TeamViewer Host",
            adminPermission = true,
            whoInstalled = 5,
            installedAt = "2024-01-12T09:00:00Z",
            comment = "Стандартный пакет для разработчиков",
            createdAt = "2024-01-12T09:00:00Z",
            updatedAt = null
        )
    )
}

@Preview(showBackground = true, showSystemUi = true, name = "Полностью заполненный актив")
@Composable
fun AssetDetailsContentPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetDetailsContent(asset = getSampleFullAssetDto())
        }
    }
}