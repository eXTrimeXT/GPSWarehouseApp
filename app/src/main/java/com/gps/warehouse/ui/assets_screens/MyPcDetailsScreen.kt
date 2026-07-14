package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
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
fun MyPcDetailsScreen(
    pcId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val myPcsList by viewModel.myPcsList.collectAsState()

    // Находим ПК по ID из загруженного списка (чтобы не делать лишний запрос)
//    val pc = (uiState as? AssetViewModel.AssetUiState.MyPcsLoaded)?.pcs?.find { it.id == 2 }

    LaunchedEffect(Unit) {
        if (myPcsList.isEmpty()) {
            viewModel.loadMyPcs()
        }
    }

    // Ищем нужный ПК
    val pc = myPcsList.find { it.id == pcId }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = pc?.os?.pcName ?: "Детали ПК",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.MyPcsLoaded -> {
                if (pc != null) {
                    MyPcDetailsContent(pc = pc, modifier = Modifier.padding(paddingValues))
                } else {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("ПК не найден", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = (uiState as AssetViewModel.AssetUiState.Error).message, color = MaterialTheme.colorScheme.error)
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
fun MyPcDetailsContent(pc: MyPcDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ОС и устройство
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Операционная система",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Имя ПК", pc.os?.pcName)
                DetailRow("ОС", "${pc.os?.os} ${pc.os?.osRelease}")
                DetailRow("Сборка", pc.os?.osVersion)
                DetailRow("Архитектура", pc.os?.pcArch)
                DetailRow("Тип устройства", pc.os?.deviceType)
                DetailRow("Серийный номер", pc.os?.serialNumber)
            }
        }

        // Пользователь
        pc.user?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Пользователь",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("Логин", user.username)
                    DetailRow("SID", user.sid)
                    DetailRow("Путь профиля", user.userpath)
                }
            }
        }

        // Сеть
        pc.network?.let { net ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Сеть",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow("IPv4 адрес", net.ipv4Address)
                    DetailRow("Шлюз", net.defaultGatewayIpv4)
                    DetailRow("MAC адрес", net.macAddress)
                    DetailRow("Скорость", net.lineSpeedMbps)
                    DetailRow("Адаптер", net.description)
                    net.dnsServersIpv4?.let { DetailRow("DNS", it.joinToString(", ")) }
                }
            }
        }

        // Компоненты
        pc.components?.let { comp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Компоненты",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    comp.cpu?.let {
                        DetailRow("Процессор", it.name)
                        DetailRow("Ядра / Потоки", "${it.cores} / ${it.processors}")
                        DetailRow("Частота", it.speed)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Материнская плата", comp.motherboard)
                    Spacer(modifier = Modifier.height(8.dp))

                    comp.ram?.let {
                        DetailRow("ОЗУ (Всего)", it.total)
                        it.sticks?.let { sticks -> DetailRow("Планки", sticks.joinToString(", ")) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    comp.gpu?.forEachIndexed { index, gpu ->
                        DetailRow("Видеокарта ${index + 1}", gpu.name)
                        DetailRow("VRAM", gpu.vram)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    comp.disks?.forEachIndexed { index, disk ->
                        DetailRow("Диск ${index + 1}", "${disk.model} (${disk.size})")
                    }
                }
            }
        }

        // Программы
        var isProgramsExpanded by remember { mutableStateOf(false) }

        if (!pc.programs.isNullOrEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Установленные программы (${pc.programs.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Логика отображения: если развернуто - берем все, иначе - первые 15
                    val programsToShow = if (isProgramsExpanded) {
                        pc.programs
                    } else {
                        pc.programs.take(15)
                    }

                    programsToShow.forEach { program ->
                        Text(
                            text = "• $program",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }

                    // Кликабельная кнопка для переключения (появляется только если программ больше 15)
                    if (pc.programs.size > 15) {
                        Text(
                            text = if (isProgramsExpanded) "Свернуть" else "... и еще ${pc.programs.size - 15} программ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary, // Цвет подсказывает, что текст кликабельный
                            modifier = Modifier
                                .padding(start = 8.dp, top = 8.dp)
                                .clickable { isProgramsExpanded = !isProgramsExpanded }
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true, name = "Детали ПК (Полные)")
@Composable
fun MyPcDetailsContentPreview_Full() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MyPcDetailsContent(
                pc = getSampleFullMyPcDto(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, name = "Детали ПК (Минимальные)")
@Composable
fun MyPcDetailsContentPreview_Minimal() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MyPcDetailsContent(
                pc = getSampleMinimalMyPcDto(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

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