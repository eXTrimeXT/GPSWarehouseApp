package com.gps.warehouse.ui.assets_screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.PcDataDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.assets_screens.assets.DetailRow
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAssetsScreen(
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val pcData by viewModel.pcData.collectAsState()

    // Загружаем профиль, если он еще не загружен
    LaunchedEffect(Unit) {
        if (userProfile == null) {
            viewModel.loadUserProfile()
        }
    }

    // Загружаем данные, как только узнаем ID пользователя
    LaunchedEffect(userProfile?.userId) {
        userProfile?.let { profile ->
            profile.userId?.let { userId ->
                viewModel.loadMyAssets(userId)
            }
            // Загружаем данные ПК по username (userTabId)
            profile.userTabId?.let { username ->
                viewModel.loadPcData(username)
            }
        }
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.MyAssetsLoaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Секция данных ПК
                    pcData?.let { data ->
                        item {
                            PcDataCard(pcData = data)
                        }
                    }

                    // Секция активов пользователя
                    if (state.assets.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "У вас пока нет закрепленных активов",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Закрепленные активы",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(state.assets) { item ->
                            CatalogItemCard(
                                catalogItem = item,
                                onClick = {
                                    navController.navigate("catalog_details/${item.catalogId}")
                                }
                            )
                        }
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            userProfile?.userId?.let { viewModel.loadMyAssets(it) }
                            userProfile?.userTabId?.let { viewModel.loadPcData(it) }
                        }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PcDataCard(pcData: PcDataDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Информация о ПК",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Информация о пользователе
            pcData.user?.let { user ->
                Text(
                    text = "Пользователь",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Имя пользователя", user.username)
//                DetailRow("Путь профиля", user.userpath)
                DetailRow("SID", user.sid)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Информация об ОС
            pcData.os?.let { os ->
                Text(
                    text = "Операционная система",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("ОС", os.os)
                DetailRow("Версия", os.osRelease)
                DetailRow("Сборка", os.osVersion)
                DetailRow("Архитектура", os.pcArch)
                DetailRow("Имя компьютера", os.pcName)
                DetailRow("Тип устройства", os.deviceType)
                DetailRow("Product ID", os.productId)
                DetailRow("Device ID", os.deviceId)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Сетевые настройки
            pcData.network?.let { network ->
                Text(
                    text = "Сеть",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Скорость", network.lineSpeedMbps)
                DetailRow("IPv4 адрес", network.ipv4Address)
                DetailRow("Шлюз", network.defaultGatewayIpv4)
                DetailRow("MAC адрес", network.macAddress)
                DetailRow("Производитель", network.manufacturer)
                DetailRow("Описание", network.description)
                DetailRow("Версия драйвера", network.driverVersion)
                network.dnsServersIpv4?.let { dns ->
                    DetailRow("DNS серверы", dns.joinToString(", "))
                }
                network.ipv6LinkLocal?.let { ipv6 ->
                    DetailRow("IPv6 Link-Local", ipv6)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Компоненты ПК
            pcData.components?.let { components ->
                Text(
                    text = "Компоненты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                // CPU
                components.cpu?.let { cpu ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Процессор",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DetailRow("Модель", cpu.name)
                    DetailRow("Ядра / Потоки", "${cpu.cores} / ${cpu.processors}")
                    DetailRow("Частота", cpu.speed)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Материнская плата
                components.motherboard?.let { motherboard ->
                    DetailRow("Материнская плата", motherboard)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // RAM
                components.ram?.let { ram ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Оперативная память",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DetailRow("Объем", ram.total)
                    ram.sticks?.let { sticks ->
                        DetailRow("Планки", sticks.joinToString(", "))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // GPU
                components.gpu?.let { gpuList ->
                    if (gpuList.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VideogameAsset,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Видеокарты",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        gpuList.forEachIndexed { index, gpu ->
                            DetailRow("Модель ${index + 1}", gpu.name)
                            DetailRow("Память", gpu.vram)
                            DetailRow("Драйвер", gpu.driver)
                            if (index < gpuList.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Диски
                components.disks?.let { disks ->
                    if (disks.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Диски",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        disks.forEachIndexed { index, disk ->
                            DetailRow("Модель ${index + 1}", disk.model)
                            DetailRow("Размер", disk.size)
                            DetailRow("Интерфейс", disk.diskInterface)
                            if (index < disks.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Офисные пакеты
            pcData.officePackage?.let { packages ->
                if (packages.isNotEmpty()) {
                    Text(
                        text = "Офисные пакеты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    packages.forEach { pkg ->
                        Text(
                            text = "• $pkg",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Программы
            pcData.programs?.let { programs ->
                if (programs.isNotEmpty()) {
                    Text(
                        text = "Установленные программы (${programs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    programs.take(10).forEach { program ->
                        Text(
                            text = "• $program",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                    if (programs.size > 10) {
                        Text(
                            text = "... и еще ${programs.size - 10} программ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}