package com.gps.warehouse.ui.assets_screens.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val allAssets = when (val state = uiState) {
        is AssetViewModel.AssetUiState.AssetsLoaded -> state.assets
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Категории активов",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (uiState as AssetViewModel.AssetUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
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
            else -> {
                userProfile?.let { profile ->
                    val availableTypes = getAvailableAssetTypes(profile.permissions, assetTypes)
                    val othersCount = allAssets.count { it.typeAsset == null }

                    if (availableTypes.isEmpty() && othersCount == 0) {
                        // Красивый пустой экран, если нет прав и нет "других" активов
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Нет доступных активов",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "У вас нет прав просмотра или активы отсутствуют",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp) // Более компактный интервал
                        ) {
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

                            // Карточка "Другие" (показываем всегда, если есть права на просмотр в целом)
                            item {
                                AssetTypeCard(
                                    typeInfo = AssetTypeInfo(
                                        key = "others",
                                        displayName = "Прочие активы",
                                        icon = Icons.Default.MoreHoriz,
                                        description = "Активы без присвоенной категории",
                                        enName = null
                                    ),
                                    assetsCount = othersCount,
                                    onClick = {
                                        navController.navigate("assets_by_type/others")
                                    }
                                )
                            }
                        }
                    }
                } ?: run {
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
        shape = MaterialTheme.shapes.medium, // Более современные скругления (12.dp)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // Легкая, ненавязчивая тень
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Компактные внутренние отступы
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка в цветном контейнере
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeInfo.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = typeInfo.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Компактный бейдж количества
                    if (assetsCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall, // Форма "таблетки"
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "$assetsCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
//                Spacer(modifier = Modifier.height(2.dp))
//
//                Text(
//                    text = typeInfo.description,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
            }

            // Современная стрелка
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
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
                description = "Тип: ${type.name}", // Сделали описание короче и информативнее
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