package com.gps.warehouse.ui.home

import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.assets_screens.assets.AssetTypeCard
import com.gps.warehouse.ui.assets_screens.assets.AssetTypeInfo
import com.gps.warehouse.ui.assets_screens.assets.getAvailableAssetTypes

// Перечисление вкладок нижней навигации
enum class HomeTab(val title: String, val icon: ImageVector) {
    ORDERS("Заказы", Icons.AutoMirrored.Filled.ListAlt),
    WAREHOUSE("Склад", Icons.Default.Warehouse),
    ASSETS("Активы", Icons.Default.AutoAwesomeMotion),
    SETTINGS("Настройки", Icons.Default.Settings),
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    assetViewModel: AssetViewModel
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = Заказы (по умолчанию)
    val tabs = HomeTab.entries.toTypedArray()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { route -> navController.navigate(route) },
            assetViewModel = assetViewModel
        )
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    onNavigate: (String) -> Unit,
    assetViewModel: AssetViewModel? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when (selectedTabIndex) {
            // Вкладка "Заказы"
            0 -> {
                MenuButton(
                    title = "Создание заказа",
                    subtitle = "Создание нового заказа на перемещение",
                    icon = Icons.Default.AddShoppingCart,
                    onClick = { onNavigate("packaging") }
                )
                MenuButton(
                    title = "Список заказов",
                    subtitle = "Активные заказы и приемка",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    onClick = { onNavigate("orders") }
                )
                MenuButton(
                    title = "Архив заказов",
                    subtitle = "История выполненных операций",
                    icon = Icons.Default.Archive,
                    onClick = { onNavigate("archive") }
                )
            }
            // Вкладка "Склад"
            1 -> {
                MenuButton(
                    title = "Упаковка на склад",
                    subtitle = "Оприходование и маркировка",
                    icon = Icons.Default.Inventory,
                    onClick = { onNavigate("pack_to_warehouse") }
                )
                MenuButton(
                    title = "Склады",
                    subtitle = "Перемещение материалов",
                    icon = Icons.Default.Warehouse,
                    onClick = { onNavigate("wms") }
                )
                MenuButton(
                    title = "Склад деталей",
                    subtitle = "Остатки и наличие",
                    icon = Icons.Default.Warehouse,
                    onClick = { onNavigate("warehouse") }
                )
                MenuButton(
                    title = "Инвентаризация",
                    subtitle = "Проведение инвентаризации склада",
                    icon = Icons.Default.RequestPage,
                    onClick = { onNavigate("inventory") }
                )
                MenuButton(
                    title = "Приемка",
                    subtitle = "Приемка материалов по заказу",
                    icon = Icons.Default.AssignmentReturned,
                    onClick = { onNavigate("wms_receive") }
                )
            }
            // Вкладка "Активы"
            2 -> {
                AssetsTabContent(
                    assetViewModel = assetViewModel,
                    onNavigate = onNavigate
                )
            }
            // Вкладка "Настройки"
            3 -> {
                MenuButton(
                    title = "Профиль",
                    subtitle = "Информация о пользователе",
                    icon = Icons.Default.Person,
                    onClick = { onNavigate("profile") }
                )
                MenuButton(
                    title = "Настройки",
                    subtitle = "Обновления и параметры",
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate("settings") }
                )
            }
        }
    }
}

/**
 * Содержимое вкладки "Активы" — карточки типов активов
 */
@Composable
fun AssetsTabContent(
    assetViewModel: AssetViewModel?,
    onNavigate: (String) -> Unit
) {
    if (assetViewModel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("AssetViewModel не передан")
        }
        return
    }

    val uiState by assetViewModel.uiState.collectAsState()
    val userProfile by assetViewModel.userProfile.collectAsState()
    val assetTypes by assetViewModel.assetTypes.collectAsState()
    val catalogItems by assetViewModel.catalog.collectAsState()

    LaunchedEffect(Unit) {
        assetViewModel.loadUserProfile()
        assetViewModel.loadAssetTypes()
        assetViewModel.loadAssets()       // Для подсчета типов активов
        assetViewModel.loadCatalog()      // Загружаем каталог
    }

    // Получаем количество активов
    val allAssets = when (val state = uiState) {
        is AssetViewModel.AssetUiState.AssetsLoaded -> state.assets
        else -> emptyList()
    }


    when (uiState) {
        is AssetViewModel.AssetUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AssetViewModel.AssetUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (uiState as AssetViewModel.AssetUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        assetViewModel.loadUserProfile()
                        assetViewModel.loadAssetTypes()
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
                val catalogCount = catalogItems.size

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Карточка "Мои активы"
                    AssetTypeCard(
                        typeInfo = AssetTypeInfo(
                            key = "my_assets",
                            displayName = "Мои активы",
                            icon = Icons.Default.Badge, // Иконка бейджа/пользователя
                            description = "Активы, закрепленные за вами",
                            enName = null
                        ),
                        // Считаем количество активов, где владелец = текущий пользователь
                        assetsCount = profile.userId?.let { currentId ->
                            catalogItems.count { it.owner?.userId == currentId }
                        } ?: 0,
                        onClick = {
                            onNavigate("my_assets")
                        }
                    )

                    // Карточка каталога (связь пользователя и актива)
                    AssetTypeCard(
                        typeInfo = AssetTypeInfo(
                            key = "catalog",
                            displayName = "Каталог активов",
                            icon = Icons.Default.Book,
                            description = "Учёт активов с гарантией и владельцами",
                            enName = null
                        ),
                        onClick = {
                            onNavigate("catalog")
                        },
                        assetsCount = catalogCount
                    )

                    HorizontalDivider()

                    availableTypes.forEach { typeInfo ->
                        val count = allAssets.count { it.typeAsset == typeInfo.enName }
                        AssetTypeCard(
                            typeInfo = typeInfo,
                            onClick = {
                                onNavigate("assets_by_type/${typeInfo.enName}")
                            },
                            assetsCount = count
                        )
                    }

                    // Карточка "Другие"
                    AssetTypeCard(
                        typeInfo = AssetTypeInfo(
                            key = "others",
                            displayName = "Другие",
                            icon = Icons.Default.MoreHoriz,
                            description = "Активы без категории",
                            enName = null
                        ),
                        onClick = {
                            onNavigate("assets_by_type/others")
                        },
                        assetsCount = othersCount
                    )
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// Оригинальный компонент кнопки
@Composable
fun MenuButton(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    isDangerous: Boolean = false,
    containerColor: Color? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDangerous) MaterialTheme.colorScheme.errorContainer
            else containerColor ?: MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isDangerous) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDangerous) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDangerous) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDangerous) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun HomeScreenPreview() {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Заказы (по умолчанию)
    val tabs = HomeTab.entries.toTypedArray()
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun HomeScreenPreview1() {
    var selectedTabIndex by remember { mutableIntStateOf(1) } // 0 = Заказы (по умолчанию)
    val tabs = HomeTab.entries.toTypedArray()
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun HomeScreenPreview2() {
    var selectedTabIndex by remember { mutableIntStateOf(2) } // 0 = Заказы (по умолчанию)
    val tabs = HomeTab.entries.toTypedArray()
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun HomeScreenPreview3() {
    var selectedTabIndex by remember { mutableIntStateOf(3) } // 0 = Заказы (по умолчанию)
    val tabs = HomeTab.entries.toTypedArray()
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { }
        )
    }
}