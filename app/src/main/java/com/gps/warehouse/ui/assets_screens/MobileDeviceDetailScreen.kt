package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import com.gps.warehouse.ui.MobileDevicesViewModel
import com.gps.warehouse.ui.UiState
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDeviceDetailScreen(
    serialNumber: String,
    viewModel: MobileDevicesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // 1. Наблюдаем за состоянием детального запроса
    val uiState by viewModel.detailUiState.collectAsState()

    // 2. При открытии экрана запускаем запрос на сервер с этим serialNumber
    LaunchedEffect(serialNumber) {
        viewModel.loadDeviceDetails(serialNumber)
    }

    // 3. Отображаем результат в зависимости от состояния
    when (val state = uiState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ошибка: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Назад")
                    }
                }
            }
        }
        is UiState.Success -> {
            val device = state.devices.firstOrNull()
            if (device != null) {
                MobileDeviceDetailContent(device = device, onNavigateBack = onNavigateBack)
            } else {
                onNavigateBack() // На всякий случай, если список пуст
            }
        }
    }
}

// Content оставляем без изменений (он у вас уже правильный)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDeviceDetailContent(
    device: DeviceResponse,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SN: ${device.serial_number}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            InfoSection(title = "📱 Устройство") {
                InfoRow("Модель", device.info.model)
                InfoRow("Имя", device.info.name)
            }
            // ... (остальные секции Система, Железо, Сеть, Батарея остаются без изменений) ...
            InfoSection(title = "⚙️ Система") {
                InfoRow("Версия Android", device.system.android_version)
                InfoRow("API Level", device.system.android_api_version)
                InfoRow("Сборка", device.system.build_number)
                InfoRow("Язык", device.system.language)
                InfoRow("Часовой пояс", device.system.timezone)
                InfoRow("Аптайм", device.system.uptime)
            }
            InfoSection(title = "🛠️ Железо") {
                InfoRow("Процессор", device.hardware.processor)
                InfoRow("Архитектура", device.hardware.processor_architecture)
                InfoRow("ОЗУ (Всего / Свободно)", "${device.hardware.ram_total} / ${device.hardware.ram_free}")
                InfoRow("Память (Всего / Свободно)", "${device.hardware.storage_total} / ${device.hardware.storage_free}")
                InfoRow("Камеры", device.hardware.cameras)
                InfoRow("Разрешение экрана", device.hardware.screen_resolution)
            }
            InfoSection(title = "🌐 Сеть") {
                InfoRow("Тип подключения", device.network.connection_type)
                InfoRow("Wi-Fi SSID", device.network.wifi_ssid ?: "Скрыт/Нет")
                InfoRow("Шлюз", device.network.wifi_gateway)
                InfoRow("MAC-адрес", device.network.mac_address)
                InfoRow("IP-адреса", device.network.ip_addresses)
                InfoRow("Bluetooth", device.network.bluetooth)
            }
            InfoSection(title = "🔋 Батарея") {
                InfoRow("Уровень", device.battery.level)
                InfoRow("Статус", device.battery.status)
                InfoRow("Температура", device.battery.temperature)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Время запроса: ${device.request_time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value ?: "N/A", style = MaterialTheme.typography.bodyMedium)
    }
}


// PREVIEW
@Preview(showBackground = true, name = "Детали устройства", device = Devices.PIXEL_4)
@Composable
private fun MobileDeviceDetailContentPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDeviceDetailContent(
                device = mockDeviceForPreview,
                onNavigateBack = { println("Preview: Назад") }
            )
        }
    }
}

@Preview(showBackground = true, name = "Детали устройства (не найдено)", device = Devices.PIXEL_4)
@Composable
private fun MobileDeviceDetailContentNotFoundPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDeviceDetailContent(
                device = mockDeviceForPreview,
                onNavigateBack = { println("Preview: Назад") }
            )
        }
    }
}