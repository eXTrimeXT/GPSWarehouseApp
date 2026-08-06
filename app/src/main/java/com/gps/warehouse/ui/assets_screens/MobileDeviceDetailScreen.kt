package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import com.gps.warehouse.ui.viewmodels.MobileDevicesViewModel
import com.gps.warehouse.ui.viewmodels.UiState
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDeviceDetailScreen(
    serialNumber: String,
    viewModel: MobileDevicesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.detailUiState.collectAsState()

    LaunchedEffect(serialNumber) {
        viewModel.loadDeviceDetails(serialNumber)
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            CustomLoadingView()
        }

        is UiState.Error -> {
            ErrorStateView(
                message = state.message,
                onRetry = { viewModel.loadDeviceDetails(serialNumber) },
                modifier = Modifier.fillMaxSize()
            )
        }

        is UiState.Success -> {
            val device = state.devices.firstOrNull()
            if (device != null) {
                MobileDeviceDetailContent(
                    device = device,
                    onNavigateBack = onNavigateBack,
                    onPlaySoundClick = { viewModel.playDeviceSound(device.serial_number) }
                )
            } else {
                ErrorStateView(
                    message = "Устройство не найдено",
                    onRetry = { onNavigateBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDeviceDetailContent(
    device: DeviceResponse,
    onNavigateBack: () -> Unit,
    onPlaySoundClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = device.device?.name.toString(),
                onBackClick = onNavigateBack,
                actionButton = {
                    IconButton(onClick = onPlaySoundClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Воспроизвести звук"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Используем ваш кастомный скролл
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            InfoSection(
                title = "Устройство",
                icon = Icons.Default.PhoneAndroid,
                {
                    InfoRow("Модель", device.device?.model)
                    InfoRow("Имя", device.device?.name)
                    InfoRow("Серийный номер", device.serial_number)
                    InfoRow("Был в сети", device.request_time)
                },
            )

            InfoSection(
                title = "Система",
                icon = Icons.Default.Settings,
                {
                    InfoRow("Версия Android", device.system?.android_version)
                    InfoRow("API Level", device.system?.android_api_version)
                    InfoRow("Сборка", device.system?.build_number)
                    InfoRow("Язык", device.system?.language)
                    InfoRow("Часовой пояс", device.system?.timezone)
                    InfoRow("Аптайм", device.system?.uptime)
                },
            )

            InfoSection(
                title = "Железо",
                icon = Icons.Default.Handyman,
                {
                    InfoRow("Процессор", device.hardware?.processor)
                    InfoRow("Архитектура", device.hardware?.processor_architecture)
                    InfoRow(
                        "ОЗУ (Всего / Свободно)",
                        "${device.hardware?.ram_total} / ${device.hardware?.ram_free}"
                    )
                    InfoRow(
                        "Память (Всего / Свободно)",
                        "${device.hardware?.storage_total} / ${device.hardware?.storage_free}"
                    )
                    InfoRow("Камеры", device.hardware?.cameras)
                    InfoRow("Разрешение экрана", device.hardware?.screen_resolution)
                },
            )

            InfoSection(
                title = "Сеть",
                icon = Icons.Default.WifiTethering,
                {
                    InfoRow("Тип подключения", device.network?.connection_type)
                    InfoRow("Wi-Fi SSID", device.network?.wifi_ssid ?: "Скрыт/Нет")
                    InfoRow("Шлюз", device.network?.wifi_gateway)
                    InfoRow("MAC-адрес", device.network?.mac_address)
                    InfoRow("IP-адреса", device.network?.ip_addresses)
                    InfoRow("Bluetooth", device.network?.bluetooth)
                },
            )

            InfoSection(title = "Батарея", icon = Icons.Default.BatteryChargingFull) {
                InfoRow("Уровень", device.battery?.level)
                InfoRow("Статус", device.battery?.status)
                InfoRow("Температура", device.battery?.temperature)
            }
        }
    }
}

@Composable
fun InfoSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier) {
                Icon(imageVector = icon, contentDescription = title)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value ?: "N/A", style = MaterialTheme.typography.bodyMedium)
    }
}


// PREVIEW
@Preview(showBackground = true, name = "Детали устройства", device = "spec:width=380dp,height=1200dp")
@Composable
private fun MobileDeviceDetailContentPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDeviceDetailContent(
                device = mockDeviceForPreview,
                onNavigateBack = { println("Preview: Назад") },
                onPlaySoundClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Детали устройства (не найдено)", device = "spec:width=380dp,height=1200dp")
@Composable
private fun MobileDeviceDetailContentNotFoundPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MobileDeviceDetailContent(
                device = mockDeviceForPreview,
                onNavigateBack = { println("Preview: Назад") },
                onPlaySoundClick = {}
            )
        }
    }
}