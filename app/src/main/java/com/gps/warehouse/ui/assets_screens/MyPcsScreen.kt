package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.ComponentsInfoDto
import com.gps.warehouse.data.remote.assets_dto.CpuInfoDto
import com.gps.warehouse.data.remote.assets_dto.DiskInfoDto
import com.gps.warehouse.data.remote.assets_dto.GpuInfoDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import com.gps.warehouse.data.remote.assets_dto.MyPcNetworkInfoDto
import com.gps.warehouse.data.remote.assets_dto.MyPcOsInfoDto
import com.gps.warehouse.data.remote.assets_dto.RamInfoDto
import com.gps.warehouse.data.remote.assets_dto.UserInfoDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPcsScreen(
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyPcs()
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Мои ПК",
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
            is AssetViewModel.AssetUiState.MyPcsLoaded -> {
                if (state.pcs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Computer, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("За вами не закреплены ПК", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.pcs) { pc ->
                            MyPcCard(
                                pc = pc,
                                onClick = { navController.navigate("my_pc_details/${pc.id}") }
                            )
                        }
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMyPcs() }) { Text("Повторить") }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun MyPcCard(pc: MyPcDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Computer, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pc.os?.pcName ?: "Неизвестный ПК",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Пользователь: ${pc.user?.username ?: "Не указан"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${pc.os?.os ?: "ОС"} ${pc.os?.osRelease ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Подробнее",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
// PREVIEW ФУНКЦИИ И MOCK ДАННЫЕ
// ============================================================================

@Preview(showBackground = true, name = "Карточка ПК (Полная)")
@Composable
fun MyPcCardPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyPcCard(
                pc = getSampleFullMyPcDto(),
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка ПК (Минимальная)")
@Composable
fun MyPcCardPreview_Minimal() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            MyPcCard(
                pc = getSampleMinimalMyPcDto(),
                onClick = { }
            )
        }
    }
}

// ============================================================================
// MOCK ДАННЫЕ
// ============================================================================

private fun getSampleFullMyPcDto(): MyPcDto {
    return MyPcDto(
        id = 2,
        userTabId = "gw07015370",
        updatedAt = "2026-07-13T17:25:32.534789",
        user = UserInfoDto(
            username = "gw07015370",
            userpath = "C:\\Users\\gw07015370",
            sid = "S-1-5-21-190646636-2133065394-4195261661-7756"
        ),
        network = MyPcNetworkInfoDto(
            lineSpeedMbps = "1000/1000",
            ipv6LinkLocal = "fe80::e3fd:6a89:22d6:86a9",
            ipv4Address = "10.168.154.42",
            defaultGatewayIpv4 = "10.168.154.254",
            dnsServersIpv4 = listOf("10.168.130.61", "10.168.130.62"),
            manufacturer = "Realtek",
            description = "Realtek PCIe 2.5GbE Family Controller",
            driverVersion = "1125.21.903.2024",
            macAddress = "30:56:0F:54:51:2A"
        ),
        os = MyPcOsInfoDto(
            os = "Windows",
            osRelease = "11",
            osVersion = "10.0.26200",
            pcArch = "AMD64",
            pcName = "RUTL-K0127439",
            deviceType = "ПК (Desktop)",
            productId = "00342-50791-14304-AAOEM",
            deviceId = "6dcd02be-037f-4325-86f6-1c8aa3d499c1",
            serialNumber = "Default string"
        ),
        components = ComponentsInfoDto(
            cpu = CpuInfoDto(
                name = "AMD Ryzen 7 7700X 8-Core Processor",
                cores = 8,
                processors = 16,
                speed = "4501 МГц"
            ),
            motherboard = "Gigabyte Technology Co., Ltd. B650M D3HP",
            ram = RamInfoDto(
                total = "32.0 ГБ",
                sticks = listOf("16ГБ 4800МГц", "16ГБ 4800МГц")
            ),
            gpu = listOf(
                GpuInfoDto(
                    name = "AMD Radeon(TM) Graphics",
                    vram = "0.5 ГБ",
                    driver = "32.0.11024.2"
                )
            ),
            disks = listOf(
                DiskInfoDto(
                    model = "ADATA LEGEND 900",
                    size = "1.02 ТБ",
                    diskInterface = "SCSI"
                )
            )
        ),
        officePackage = listOf("LibreOffice: LibreOffice 26.2.2.2"),
        programs = listOf(
            "1С:Предприятие 8 (x86-64) (8.3.20.1674)",
            "Android Studio",
            "Docker Desktop",
            "Git",
            "Google Chrome",
            "IntelliJ IDEA 2026.1",
            "Java(TM) SE Development Kit 21.0.9 (64-bit)",
            "Kaspersky Endpoint Security for Windows",
            "Microsoft Edge",
            "PostgreSQL 17",
            "WinSCP 6.5.5"
        )
    )
}

private fun getSampleMinimalMyPcDto(): MyPcDto {
    return MyPcDto(
        id = 99,
        userTabId = null,
        updatedAt = null,
        user = UserInfoDto(
            username = "unknown_user",
            userpath = null,
            sid = null
        ),
        network = null,
        os = MyPcOsInfoDto(
            os = "Windows",
            osRelease = "10",
            osVersion = null,
            pcArch = null,
            pcName = "UNKNOWN-PC",
            deviceType = null,
            productId = null,
            deviceId = null,
            serialNumber = null
        ),
        components = null,
        officePackage = null,
        programs = null
    )
}