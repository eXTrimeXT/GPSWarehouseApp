package com.gps.warehouse.ui.assets_screens.catalog

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
import com.gps.warehouse.data.remote.assets_dto.AndroidDataDto
import com.gps.warehouse.data.remote.assets_dto.AssetCatalogDto
import com.gps.warehouse.data.remote.assets_dto.AssetClassDto
import com.gps.warehouse.data.remote.assets_dto.AssetDto
import com.gps.warehouse.data.remote.assets_dto.AssetModelDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.BatteryInfoDto
import com.gps.warehouse.data.remote.assets_dto.DeviceInfoDto
import com.gps.warehouse.data.remote.assets_dto.HardwareInfoDto
import com.gps.warehouse.data.remote.assets_dto.LocationDto
import com.gps.warehouse.data.remote.assets_dto.NetworkInfoDto
import com.gps.warehouse.data.remote.assets_dto.SoftwareDto
import com.gps.warehouse.data.remote.assets_dto.SystemInfoDto
import com.gps.warehouse.data.remote.assets_dto.UserDto
import com.gps.warehouse.data.remote.assets_dto.UserShortDto
import com.gps.warehouse.data.remote.assets_dto.VendorDto
import com.gps.warehouse.data.remote.assets_dto.WarehouseDto
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

                // ✅ Серийный номер теперь здесь (на верхнем уровне)
                catalogItem.serialNumber?.let { DetailRow("Серийный номер", it) }

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
                    DetailRow("Статус", asset.assetStatus)
//                    asset.storageLocation?.let { DetailRow("Место хранения", it) }
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


// ================== PREVIEW ==================
@Preview(
    showBackground = true,
    name = "Catalog Details - Full Data",
    device = "spec:width=360dp,height=2100dp,dpi=480"
)
@Composable
fun CatalogDetailsPreviewFull() {
    // 1. Mock Владелец / Создатель (UserDto)
    val mockOwner = UserDto(
        userId = 42,
        userTabId = "ivanov_aa",
        owner = "Иванов Алексей Александрович",
        userEnName = "Alexey Ivanov",
        permissions = null,
        userPosition = "Системный администратор",
        departmentId = 5,
        email = "a.ivanov@company.ru",
        phone = "+7 (999) 123-45-67",
        isActive = true,
        createdAt = "2024-01-15 09:00:00",
        updatedAt = "2026-05-20 10:30:00"
    )

    val mockCreator = UserDto(
        userId = 1,
        userTabId = "admin",
        owner = "Администратор системы",
        userEnName = "System Admin",
        permissions = null,
        userPosition = "IT Manager",
        departmentId = 1,
        email = "admin@company.ru",
        phone = null,
        isActive = true,
        createdAt = "2020-05-10 08:00:00",
        updatedAt = null
    )

    // 2. Mock Модель актива и Класс
    // Если эти классы находятся в других пакетах, IDE подтянет их автоматически
    val mockAssetType = AssetTypeDto(assetTypeId = 1, name = "Моб. Терминал", enName = "Mobile Terminal")
    val mockAssetClass = AssetClassDto(
        classId = 12,
        className = "IT Equipment",
        classTypeId = 1,
        description = "Оборудование для сбора данных",
        createdAt = "2024-01-15 09:00:00",
        updatedAt = "2026-03-20 14:30:00",
        createdBy = 42,
        updatedBy = 42,
        assetType = mockAssetType,
        creator = null,  // Можно заполнить, если нужно
        updater = null
    )

    val mockModel = AssetModelDto(
        modelId = 45,
        modelName = "EDA52K-2",
        classId = 12,
        description = "Honeywell Mobile Computer",
        isActive = true,
        isSerialRequired = true,
        createdAt = "2024-01-15 09:00:00",
        updatedAt = null,
        createdBy = null,
        updatedBy = null,
        assetClass = mockAssetClass,  // Можно оставить для отображения класса
        creator = null,               // Можно null для превью
        updater = null
    )

    // 3. Mock Склад и Локация
    val mockLocation =
        LocationDto(locationId = 1, country = "Россия", city = "Москва", address = "ул. Складская, д. 15", room = "A-102", floor = "3")
    val mockWarehouse = WarehouseDto(
        warehouseId = 1,
        name = "Основной склад электроники",
        locationId = 1, location = mockLocation,
        preparedBy = 3, preparer = UserShortDto(
            userId = 1, userTabId = "324", userPosition = "staff", owner = "Test Test", departmentId = 1, email = "email@test"
        ))

    // Упрощённый VendorDto для превью
    val mockVendor = VendorDto(
        vendorId = 7,
        name = "Honeywell Russia (ООО)",
        vendorClassId = 5,
        companyId = null,
        createdAt = "2024-01-15 09:00:00",
        createdBy = 1,
        vendorClass = null,  // Можно null для превью
        company = null,      // Можно null для превью
        creator = null       // Можно null для превью
    )

// Упрощённый SoftwareDto для превью
    val mockSoftware = SoftwareDto(
        softwareId = 12,
        officeType = "Microsoft Office 2021",
        officeKey = null,
        osType = "Windows 10 Pro",
        osKey = null,
        remoteControl = null,
        adminPermission = null,
        whoInstalled = null,
        installedAt = null,
        comment = null,
        createdAt = null,
        updatedAt = null
    )

    // 4. Mock Актив (AssetDto)
    val mockAsset = AssetDto(
        assetId = 1024,
        name = "Терминал сбора данных Honeywell EDA52",
        inventoryId = "INV-2026-05-884",
        affixedInventoryId = "AF-9921",
        assetStatus = "active",
        modelId = 45,
        warehouseId = 3,
        parentId = null,
        softwareId = 12,
        manufacturerId = 7,
        vendorId = 7,
        typeDomain = "hardware",
        infoStorageLocation = "Стеллаж 4, Полка 2",
        dateIssue = "2026-04-01",
        datePurchasing = "2026-03-15",
        comment = "Выдан сотруднику для работы на зоне приемки",
        deletedAt = null,
        createdAt = "2026-03-20 14:00:00",
        updatedAt = "2026-04-01 09:15:00",
        model = mockModel,
        warehouse = mockWarehouse,
        preparer = mockCreator,
        checker = mockOwner,
        software = mockSoftware,
        manufacturer = mockVendor,
        vendor = mockVendor
    )

    // 5. Mock Данные Android устройства
    val mockAndroidData = AndroidDataDto(
        id = 505,
        device = DeviceInfoDto(model = "EDA52K-2", name = "Honeywell EDA52"),
        system = SystemInfoDto(
            androidVersion = "11",
            androidApiVersion = "30",
            buildNumber = "RP1A.200720.011",
            language = "ru_RU",
            timezone = "Europe/Moscow",
            uptime = "3d 14h 22m",
            requestTime = "2026-05-20 11:00:00"
        ),
        hardware = HardwareInfoDto(
            processor = "Qualcomm SDM660",
            processorArchitecture = "arm64-v8a",
            ramTotal = "4 GB",
            ramFree = "1.2 GB",
            storageTotal = "64 GB",
            storageFree = "22.5 GB",
            cameras = "13 MP Rear, 5 MP Front",
            screenResolution = "1080 x 1920"
        ),
        network = NetworkInfoDto(
            connectionType = "Wi-Fi",
            wifiSsid = "Warehouse_Secure_5G",
            wifiBssid = "AA:BB:CC:DD:EE:FF",
            wifiGateway = "192.168.10.1",
            macAddress = "00:1A:2B:3C:4D:5E",
            ipAddresses = "192.168.10.45",
            bluetooth = "Enabled"
        ),
        battery = BatteryInfoDto(
            level = "87%",
            status = "Charging",
            temperature = "34°C"
        )
    )

    // 6. Финальный объект каталога
    val fullCatalogItem = AssetCatalogDto(
        catalogId = 1542,
        assetId = 1024,
        serialNumber = "SN-HW-2026-991234",
        ownerId = 42,
        createdAt = "2026-03-20 14:05:30",
        owner = mockOwner,
        creator = mockCreator,
        asset = mockAsset,
        androidData = mockAndroidData
    )

    // Рендер превью
    MaterialTheme {
        Surface {
            CatalogDetailsContent(
                catalogItem = fullCatalogItem,
                modifier = Modifier
            )
        }
    }
}