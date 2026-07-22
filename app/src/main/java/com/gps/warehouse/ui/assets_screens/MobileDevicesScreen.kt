package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gps.warehouse.data.remote.assets_dto.BatteryInfo
import com.gps.warehouse.data.remote.assets_dto.DeviceInfo
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import com.gps.warehouse.data.remote.assets_dto.HardwareInfo
import com.gps.warehouse.data.remote.assets_dto.NetworkInfo
import com.gps.warehouse.data.remote.assets_dto.SystemInfo
import com.gps.warehouse.ui.MobileDevicesViewModel
import com.gps.warehouse.ui.UiState
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import kotlinx.coroutines.delay

// SCREEN: Обертка с ViewModel и бизнес-логикой
@Composable
fun MobileDevicesScreen(
    viewModel: MobileDevicesViewModel = hiltViewModel(),
    onDeviceClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.mobileUiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var debounceQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(500)
        debounceQuery = searchQuery
    }

    LaunchedEffect(debounceQuery) {
        val query = debounceQuery.takeIf { it.isNotBlank() }
        viewModel.loadDevices(serialNumber = query)
    }

    // Делегируем отрисовку чистому UI-компоненту
    MobileDevicesContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onSearch = { debounceQuery = searchQuery },
        onRetry = { viewModel.loadDevices(serialNumber = debounceQuery.takeIf { it.isNotBlank() }) },
        onDeviceClick = onDeviceClick,
        onNavigateBack = onNavigateBack
    )
}

// CONTENT: Чистый UI
@Composable
fun MobileDevicesContent(
    uiState: UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            text = "Android устройства",
            onBackClick = onNavigateBack
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Поиск по серийному номеру...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        when (uiState) {
            is UiState.Loading -> {
                CustomLoadingView()
            }
            is UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is UiState.Success -> {
                if (uiState.devices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Устройства не найдены", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(uiState.devices) { device ->
                            DeviceListItem(
                                device = device,
                                onClick = { onDeviceClick(device.serial_number) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceListItem(device: DeviceResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.device?.model ?: "Неизвестная модель",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Серийный номер: ${device.serial_number}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "🔋 ${device.battery?.level ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                Text(text = "🌐 ${device.network?.ip_addresses ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                Text(text = "🕒 ${device.request_time}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// MOCK DATA ДЛЯ ПРЕВЬЮ
val mockDeviceForPreview = DeviceResponse(
    serial_number = "25010524202340",
    request_time = "2026-07-22 14:15:50",
    device = DeviceInfo(model = "Zebra Technologies TC52", name = "TC52"),
    system = SystemInfo(
        android_version = "Android 13",
        android_api_version = "API 33",
        build_number = "13-34-31.00-TN-U00-STD-HEL-04",
        language = "ru-RU",
        timezone = "Europe/Moscow",
        uptime = "8d 05:02:00"
    ),
    hardware = HardwareInfo(
        processor = "8 cores",
        processor_architecture = "arm64-v8a",
        ram_total = "3,6 GB",
        ram_free = "1,6 GB",
        storage_total = "17,1 GB",
        storage_free = "16,4 GB",
        cameras = "2",
        screen_resolution = "720 x 1280"
    ),
    network = NetworkInfo(
        connection_type = "Wi-Fi",
        wifi_ssid = "GPS_Warehouse",
        wifi_bssid = "00:11:22:33:44:55",
        wifi_gateway = "10.168.135.254",
        mac_address = "Hidden",
        ip_addresses = "10.168.135.89",
        bluetooth = "TC52 (Hidden)"
    ),
    battery = BatteryInfo(
        level = "100%",
        status = "Заряжен",
        temperature = "31.0 °C"
    ),
    id = 2
)

// 4. PREVIEW ФУНКЦИИ
@Preview(showBackground = true, name = "Список устройств (успех)", device = Devices.PIXEL_4)
@Composable
private fun MobileDevicesContentSuccessPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDevicesContent(
                uiState = UiState.Success(listOf(mockDeviceForPreview)),
                searchQuery = "",
                onSearchQueryChange = {},
                onSearch = {},
                onRetry = {},
                onDeviceClick = { println("Preview: Клик по $it") },
                onNavigateBack = { println("Preview: Назад") }
            )
        }
    }
}

@Preview(showBackground = true, name = "Список устройств (поиск)", device = Devices.PIXEL_4)
@Composable
private fun MobileDevicesContentSearchPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDevicesContent(
                uiState = UiState.Success(listOf(mockDeviceForPreview)),
                searchQuery = "2501",
                onSearchQueryChange = {},
                onSearch = {},
                onRetry = {},
                onDeviceClick = {},
                onNavigateBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Список устройств (загрузка)", device = Devices.PIXEL_4)
@Composable
private fun MobileDevicesContentLoadingPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDevicesContent(
                uiState = UiState.Loading,
                searchQuery = "",
                onSearchQueryChange = {},
                onSearch = {},
                onRetry = {},
                onDeviceClick = {},
                onNavigateBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Список устройств (ошибка)", device = Devices.PIXEL_4)
@Composable
private fun MobileDevicesContentErrorPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDevicesContent(
                uiState = UiState.Error("Нет подключения к сети"),
                searchQuery = "",
                onSearchQueryChange = {},
                onSearch = {},
                onRetry = {},
                onDeviceClick = {},
                onNavigateBack = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Список устройств (пусто)", device = Devices.PIXEL_4)
@Composable
private fun MobileDevicesContentEmptyPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDevicesContent(
                uiState = UiState.Success(emptyList()),
                searchQuery = "не найдено",
                onSearchQueryChange = {},
                onSearch = {},
                onRetry = {},
                onDeviceClick = {},
                onNavigateBack = {}
            )
        }
    }
}