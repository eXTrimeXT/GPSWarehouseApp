package com.gps.warehouse.ui.assets_screens.assets

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.PermissionDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    navController: NavHostController,
    viewModel: AssetViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val assetTypes by viewModel.assetTypes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
        viewModel.loadAssetTypes()
        viewModel.loadAssets() // Загружаем все активы для подсчёта количества
    }

    // Получаем список всех активов из состояния
    val allAssets = when (val state = uiState) {
        is AssetViewModel.AssetUiState.AssetsLoaded -> state.assets
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Активы",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (uiState) {
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
            is AssetViewModel.AssetUiState.AssetsLoaded,
            is AssetViewModel.AssetUiState.AssetTypesLoaded,
            is AssetViewModel.AssetUiState.UserProfileLoaded,
            is AssetViewModel.AssetUiState.Idle -> {
                userProfile?.let { profile ->
                    val availableTypes = getAvailableAssetTypes(profile.permissions, assetTypes)
                    val othersCount = allAssets.count { it.typeAsset == null }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Карточки типов активов
                        items(availableTypes) { typeInfo ->
                            val count = allAssets.count { it.typeAsset == typeInfo.enName }
                            AssetTypeCard(
                                typeInfo = typeInfo,
                                assetsCount = count,
                                onClick = {
                                    navController.navigate("assets_by_type/${typeInfo.enName}")
                                }
                            )
                        }

                        // Карточка "Другие" (активы без типа)
                        item {
                            AssetTypeCard(
                                typeInfo = AssetTypeInfo(
                                    key = "others",
                                    displayName = "Другие",
                                    icon = Icons.Default.MoreHoriz,
                                    description = "Активы без категории",
                                    enName = null
                                ),
                                assetsCount = othersCount,
                                onClick = {
                                    navController.navigate("assets_by_type/others")
                                }
                            )
                        }
                    }
                } ?: run {
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
            is AssetViewModel.AssetUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as AssetViewModel.AssetUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.loadUserProfile()
                            viewModel.loadAssetTypes()
                            viewModel.loadAssets()
                        }) {
                            Text("Повторить")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

data class AssetTypeInfo(
    val key: String,
    val displayName: String,
    val icon: ImageVector,
    val description: String,
    val enName: String?
)

@Composable
fun AssetTypeCard(
    typeInfo: AssetTypeInfo,
    assetsCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = typeInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeInfo.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = typeInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Бейдж с количеством активов
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$assetsCount шт.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun getAvailableAssetTypes(
    permissions: Map<String, PermissionDto>?,
    assetTypes: List<AssetTypeDto>
): List<AssetTypeInfo> {
    if (permissions == null) return emptyList()

    val iconMap = mapOf(
        "computer" to Icons.Default.Computer,
        "mes_equipment" to Icons.Default.Memory,
        "supplies" to Icons.Default.Inventory2,
        "power_adapter" to Icons.Default.Bolt,
        "data_collection_equipment" to Icons.Default.QrCodeScanner,
        "Accessories" to Icons.Default.Cable,
        "network_equipment" to Icons.Default.Wifi,
        "printing_equipment" to Icons.Default.Print,
        "server_hardware" to Icons.Default.Dns
    )

    return assetTypes
        .filter { type ->
            val permissionKey = type.enName
            permissions[permissionKey]?.read == true
        }
        .map { type ->
            AssetTypeInfo(
                key = type.enName,
                displayName = type.name,
                icon = iconMap[type.enName] ?: Icons.Default.Category,
                description = "Тип актива: ${type.name}",
                enName = type.enName
            )
        }
        .sortedBy { it.displayName }
}


// ================
// PREVIEW ФУНКЦИИ
@Preview(showBackground = true, name = "Карточка типа (Компьютеры)")
@Composable
fun AssetTypeCardPreview_Computer() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetTypeCard(
                typeInfo = AssetTypeInfo(
                    key = "computer",
                    displayName = "Компьютеры",
                    icon = Icons.Default.Computer,
                    description = "Настольные компьютеры и рабочие станции",
                    enName = "computer"
                ),
                assetsCount = 15,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка типа (Другие)")
@Composable
fun AssetTypeCardPreview_Others() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetTypeCard(
                typeInfo = AssetTypeInfo(
                    key = "others",
                    displayName = "Другие",
                    icon = Icons.Default.MoreHoriz,
                    description = "Активы без категории",
                    enName = null
                ),
                assetsCount = 42,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, name = "Карточка типа (Сетевое оборудование)")
@Composable
fun AssetTypeCardPreview_Network() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AssetTypeCard(
                typeInfo = AssetTypeInfo(
                    key = "network_equipment",
                    displayName = "Сетевое оборудование",
                    icon = Icons.Default.Wifi,
                    description = "Роутеры, коммутаторы, точки доступа",
                    enName = "network_equipment"
                ),
                assetsCount = 8,
                onClick = { }
            )
        }
    }
}