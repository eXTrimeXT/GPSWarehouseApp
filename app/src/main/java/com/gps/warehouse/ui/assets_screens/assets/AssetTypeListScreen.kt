package com.gps.warehouse.ui.assets_screens.assets

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.gps_dto.GpsPermissionDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CameraScannerDialog
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.InventoryQrParser
import com.gps.warehouse.utils.ScannerManager
import kotlinx.coroutines.launch

private const val TAG = "AssetTypeListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetTypeListScreen(
    navController: NavHostController,
    assetViewModel: AssetViewModel,
    mainViewModel: MainViewModel // Добавляем для получения прав доступа
) {
    val context = LocalContext.current
    val scannerManager = remember { ScannerManager(context) }

    // Для поиска по серийному или инвентарному номеру
    val scope = rememberCoroutineScope()

    var serialNumber by remember { mutableStateOf("") }
    var inventoryId by remember { mutableStateOf("") }

    // Собираем состояния из assetTypesUiState
    val assetTypesUiState by assetViewModel.assetTypesUiState.collectAsState()
    val assetTypes by assetViewModel.assetTypes.collectAsState()
    val gpsPermissions by mainViewModel.gpsPermissions.collectAsState()

    // Сканирование с помощью камеры
    val cameraScanEnabled by mainViewModel.cameraScanEnabled.collectAsState()
    var showCameraDialog by remember { mutableStateOf(false) }

    // Загружаем данные при открытии экрана
    LaunchedEffect(Unit) {
        mainViewModel.loadPermissions()
        assetViewModel.loadAssetTypes()
    }

    fun processScannedData(scannedData: String) {
        if (scannedData.isEmpty()) return
        Log.d(TAG, "processScannedData: $scannedData")

        val scannedValue = InventoryQrParser.parseAny(scannedData)
        if (scannedValue == null) {
            Toast.makeText(context, "Не удалось распознать код", Toast.LENGTH_SHORT).show()
            return
        }

        // Запускаем серверный поиск в корутине
        scope.launch {
//            isSearching = true
            try {
                val foundAsset = assetViewModel.findAssetByScanValue(scannedValue)

                if (foundAsset != null) {
                    Log.d(TAG, "Scanned asset found: id=${foundAsset.assetId}, navigating")

                    serialNumber = if (foundAsset.serialNumber?.isNotEmpty() == true) {
                        foundAsset.serialNumber
                    } else { "" }

                    inventoryId = if (foundAsset.inventoryId?.isNotEmpty() == true) {
                        foundAsset.inventoryId
                    } else { "" }

                    navController.navigate("assets_list/${foundAsset.assetTypeId}/${foundAsset.assetTypeName}/${serialNumber}/${inventoryId}")


                } else {
                    Toast.makeText(
                        context,
                        "Актив '$scannedValue' не найден (проверено как серийник и инвентарник)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка поиска: ${e.message}")
                Toast.makeText(context, "Ошибка поиска: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
//                isSearching = false
            }
        }
    }

    LaunchedEffect(Unit) {
        scannerManager.barcodeFlow.collect { scannedData -> processScannedData(scannedData) }
    }

    DisposableEffect(Unit) {
        scannerManager.init()
        onDispose { scannerManager.release() }
    }

    if (showCameraDialog) {
        CameraScannerDialog(
            onDismiss = { showCameraDialog = false },
            onBarcodeDetected = { scannedCode ->
                processScannedData(scannedCode)
                showCameraDialog = false
            }
        )
    }

    // Делегируем отрисовку чистому UI-компоненту
    AssetTypeListScreenContent(
        uiState = assetTypesUiState,
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanClick = { showCameraDialog = true },
        assetTypes = assetTypes,
        gpsPermissions = gpsPermissions,
        onNavigate = { route -> navController.navigate(route) },
        onBackClick = { navController.popBackStack() },
        onRetry = { assetViewModel.loadAssetTypes() }
    )
}

// ============================================================================
// UI КОМПОНЕНТЫ
// ============================================================================
@Composable
fun AssetTypeListScreenContent(
    uiState: AssetViewModel.AssetTypesUiState,
    cameraScanEnabled: Boolean,
    onCameraScanClick: () -> Unit,
    assetTypes: List<AssetTypeDto>,
    gpsPermissions: List<GpsPermissionDto>?,
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Типы активов",
                onBackClick = onBackClick,
                actionButton = {
                    if (cameraScanEnabled) {
                        IconButton(onClick = onCameraScanClick) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Сканировать камерой",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is AssetViewModel.AssetTypesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetTypesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) { Text("Повторить") }
                    }
                }
            }
            is AssetViewModel.AssetTypesUiState.Loaded,
            is AssetViewModel.AssetTypesUiState.Idle-> {
                val availableTypes = assetTypes.filter { type ->
                    gpsPermissions?.any { permission ->
                        permission.nameGroup.equals(type.enName, ignoreCase = true) && permission.read
                    } == true
                }.sortedBy { it.assetTypeId }

                if (availableTypes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Нет доступных типов активов", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Проверьте ваши права доступа", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTypes) { type ->
                            AssetTypeCard(
                                type = type,
                                onClick = { onNavigate("assets_list/${type.assetTypeId}/${type.name}") }
                            )
                        }
                        // Карточка "Другие типы" всегда внизу списка
                        item {
                            OtherTypesCard(
                                onClick = {
                                    onNavigate("assets_list/null/Все активы")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Карточка для активов, у которых есть тип
@Composable
fun AssetTypeCard(
    type: AssetTypeDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка на цветном фоне
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconForType(type.enName),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = type.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Подробнее",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// Карточка для активов без типа
@Composable
fun OtherTypesCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Все типы активов",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Активы без указанного типа",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Подробнее",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Возвращает соответствующую Material Icon для типа актива по его en_name.
 */
fun getIconForType(enName: String): ImageVector {
    return when (enName.lowercase()) {
        "computer" -> Icons.Default.Computer
        "mes_equipment" -> Icons.Default.Memory
        "supplies" -> Icons.Default.Inventory2
        "power_adapter" -> Icons.Default.Bolt
        "data_collection_equipment" -> Icons.Default.QrCodeScanner
        "accessories" -> Icons.Default.Cable
        "network_equipment" -> Icons.Default.Wifi
        "printing_equipment" -> Icons.Default.Print
        "server_hardware" -> Icons.Default.Dns
        "assetsmu" -> Icons.Default.Build
        else -> Icons.Default.Category
    }
}


// ============================================================================
// PREVIEW ФУНКЦИИ ДЛЯ ВСЕГО ЭКРАНА
// ============================================================================
@Preview(showBackground = true, name = "Экран: Список типов (Заполненный)")
@Composable
fun AssetTypeListScreenPreview_Loaded() {
    val mockAssetTypes = listOf(
        AssetTypeDto(assetTypeId = 1, name = "Компьютер", enName = "computer", createdBy = null, createdAt = "2026-07-06T07:18:41.873769", updatedAt = null),
        AssetTypeDto(assetTypeId = 5, name = "Оборудование сбора данных", enName = "data_collection_equipment", createdBy = null, createdAt = "2026-07-06T07:20:23.134850", updatedAt = null),
        AssetTypeDto(assetTypeId = 7, name = "Сетевое оборудование", enName = "network_equipment", createdBy = null, createdAt = "2026-07-06T07:21:39.334371", updatedAt = null)
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetTypeListScreenContent(
                uiState = AssetViewModel.AssetTypesUiState.Loaded(mockAssetTypes),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                assetTypes = mockAssetTypes,
                gpsPermissions = listOf(
                    GpsPermissionDto(nameGroup = "computer", read = true, write = true),
                    GpsPermissionDto(nameGroup = "data_collection_equipment", read = true, write = false),
                    GpsPermissionDto(nameGroup = "network_equipment", read = true, write = true)
                ),
                onNavigate = {},
                onBackClick = {},
                onRetry = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Экран: Нет прав доступа")
@Composable
fun AssetTypeListScreenPreview_Empty() {
    val mockAssetTypes = listOf(
        AssetTypeDto(assetTypeId = 1, name = "Компьютер", enName = "computer", createdBy = null, createdAt = "2026-07-06T07:18:41.873769", updatedAt = null)
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AssetTypeListScreenContent(
                uiState = AssetViewModel.AssetTypesUiState.Loaded(mockAssetTypes),
                cameraScanEnabled = true,
                onCameraScanClick = {},
                assetTypes = mockAssetTypes,
                gpsPermissions = listOf(
                    GpsPermissionDto(nameGroup = "computer", read = false, write = false)
                ),
                onNavigate = {},
                onBackClick = {},
                onRetry = {}
            )
        }
    }
}