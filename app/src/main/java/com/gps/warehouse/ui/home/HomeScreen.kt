package com.gps.warehouse.ui.home

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.BmListDto
import com.gps.warehouse.data.remote.gps_dto.GpsPermissionDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.utils.AppPreferences

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
    viewModel: MainViewModel
) {
    val tabs = HomeTab.entries.toTypedArray()
    val gpsPermissions by viewModel.gpsPermissions.collectAsState()
    val isUserAssetsAdmin by viewModel.userIsAssetsAdmin.collectAsState()
    val bmList by viewModel.bmList.collectAsState()

    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(AppPreferences.getDefaultTab(context))
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Флаг прав, есть ли хотя бы 1 элемент доступа
    val isPermissions = gpsPermissions.any { it.read } || isUserAssetsAdmin
    Log.d("isPermissions", isPermissions.toString())

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    if (isTabFilter(bmList, tab)) {
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { route -> navController.navigate(route) },
            permissions = gpsPermissions,
            isUserAssetsAdmin = isUserAssetsAdmin,
            bmList = bmList
        )
    }
}

@Composable
fun isTabFilter(bmList: List<BmListDto>, tab: HomeTab): Boolean {
//    return (
//        (tab.title == HomeTab.SETTINGS.title) || // Всегда показываем настройки
//        (tab.title == HomeTab.ASSETS.title && bmList.any { it.name == "Assets" }) ||
//        (tab.title == HomeTab.ORDERS.title && bmList.any { it.name == "AfterSales" }) ||
//        (tab.title == HomeTab.WAREHOUSE.title && bmList.any { it.name == "Warehouse management" })
//    )
    // Чтобы отображать все NavigationBar возвращаем true
     return true
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    onNavigate: (String) -> Unit,
    permissions: List<GpsPermissionDto>,
    isUserAssetsAdmin: Boolean,
    bmList: List<BmListDto>
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
                    onClick = { onNavigate("packaging") },
                    bmList = bmList,
                    nameRule = "ps_makeorder"
                )

                MenuButton(
                    title = "Список заказов",
                    subtitle = "Активные заказы и приемка",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    onClick = { onNavigate("orders") },
                    bmList = bmList,
                    nameRule = "qrcode_pda"
                )

                MenuButton(
                    title = "Архив заказов",
                    subtitle = "История выполненных операций",
                    icon = Icons.Default.Archive,
                    onClick = { onNavigate("archive") },
                    bmList = bmList,
                    nameRule = "qrcode_msk"
                )

                MenuButton(
                    title = "Упаковка на склад",
                    subtitle = "Оприходование и маркировка",
                    icon = Icons.Default.Inventory,
                    onClick = { onNavigate("pack_to_warehouse") },
                    bmList = bmList,
                    nameRule = "ps_pack_materials"
                )

                MenuButton(
                    title = "Склад деталей",
                    subtitle = "Остатки и наличие",
                    icon = Icons.Default.Warehouse,
                    onClick = { onNavigate("warehouse") },
                    bmList = bmList,
                    nameRule = "qrcode_ps"
                )
            }
            // Вкладка "Склад"
            1 -> {
                MenuButton(
                    title = "Склады",
                    subtitle = "Перемещение материалов",
                    icon = Icons.Default.Warehouse,
                    onClick = { onNavigate("wms") },
                    bmList = bmList,
                    nameRule = "wh_read_remains"
                )

                MenuButton(
                    title = "Инвентаризация",
                    subtitle = "Проведение инвентаризации склада",
                    icon = Icons.Default.RequestPage,
                    onClick = { onNavigate("inventory") },
                    bmList = bmList,
                    nameRule = "wh_inv_read"
                )

                MenuButton(
                    title = "Приемка",
                    subtitle = "Приемка материалов по заказу",
                    icon = Icons.Default.AssignmentReturned,
                    onClick = { onNavigate("wms_receive") },
                    bmList = bmList,
                    nameRule = "wh_accept_store"
                )

                MenuButton(
                    title = "Списание",
                    subtitle = "Списание материалов со склада",
                    icon = Icons.Default.DeleteForever,
                    onClick = { onNavigate("wms_write_off") },
                    bmList = bmList,
                    nameRule = "wms_write_off"
                )
            }
            // Вкладка "Активы"
            2 -> {
                val isAllPermissionsFalse = permissions.any { !it.read }
                if (isAllPermissionsFalse || isUserAssetsAdmin)
                    MenuButton(
                        title = "Типы активов",
                        subtitle = "Доступные типы активов",
                        icon = Icons.Default.Category,
                        onClick = { onNavigate("asset_types") },
                        isVisible = true
                    )

                val isReadAndroid = permissions.any { it.nameGroup == "android_data" && it.read }
                if (isReadAndroid || isUserAssetsAdmin)
                    MenuButton(
                        title = "Android устройства",
                        subtitle = "Мобильные устройства Android\nHoneywell, Zebra",
                        icon = Icons.Default.PhoneAndroid,
                        onClick = { onNavigate("mobile_devices") },
                        isVisible = true
                    )

                MenuButton(
                    title = "Мои активы",
                    subtitle = "Список вашего оборудования",
                    icon = Icons.Default.AutoAwesomeMosaic,
                    onClick = { onNavigate("my_assets_list") },
                    isVisible = true
                )

                val isReadComputers = permissions.any { it.nameGroup == "computer" && it.read }
                if (isReadComputers || isUserAssetsAdmin)
                    MenuButton(
                        title = "Мои ПК",
                        subtitle = "Компьютеры и их конфигурация",
                        icon = Icons.Default.Computer,
                        onClick = { onNavigate("my_pcs") },
                        isVisible = true
                    )

                MenuButton(
                    title = "Инвентаризация активов",
                    subtitle = "Сессии инвентаризации по типам",
                    icon = Icons.Default.RequestPage,
                    onClick = { onNavigate("inventorization_sessions") },
                    isVisible = true
                )
                MenuButton(
                    title = "Карта активов",
                    subtitle = "Карта цехов и активов",
                    icon = Icons.Default.Map,
                    onClick = { onNavigate("assets_map_web") },
                    isVisible = true
                )
            }
            // Вкладка "Настройки"
            3 -> {
                MenuButton(
                    title = "Профиль",
                    subtitle = "Информация о пользователе",
                    icon = Icons.Default.Person,
                    onClick = { onNavigate("profile") },
                    isVisible = true
                )
                MenuButton(
                    title = "Настройки",
                    subtitle = "Обновления и параметры",
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate("settings") },
                    isVisible = true
                )
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
    containerColor: Color? = null,
    bmList: List<BmListDto> = emptyList(),
    nameRule: String = "",
    // если true, тогда показываем все вкладки без учета прав,
    // чтобы увидеть все вкладки надо изменить (isTabFilter(bmList, tab) на true
    isVisible: Boolean = true // true - только для тестов
) {
    if (bmList.any { it.nameRule == nameRule || it.name1 == nameRule } || isVisible) {

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
                            color = if (isDangerous) MaterialTheme.colorScheme.onErrorContainer.copy(
                                alpha = 0.7f
                            )
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
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun HomeScreenPreview() {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Заказы (по умолчанию)
    val tabs = HomeTab.entries.toTypedArray()
    val permissions = listOf(
        GpsPermissionDto(nameGroup = "computer", read = false, write = false),
        GpsPermissionDto(nameGroup = "mes_equipment", read = false, write = true),
        GpsPermissionDto(nameGroup = "power_adapter", read = false, write = true),
        GpsPermissionDto(nameGroup = "android_data", read = true, write = true),
    )
    val bmList: List<BmListDto> = emptyList()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    if (
                        // Всегда показываем настройки
                        (tab.title == HomeTab.SETTINGS.title) ||

                        // Если вкладка Активы и есть права, то показываем
                        (tab.title == HomeTab.ASSETS.title && bmList.any { it.name == "Assets" }) ||

                        // Если вкладка Заказы и есть доступ, то показываем
                        (tab.title == HomeTab.ORDERS.title && bmList.any { it.name == "AfterSales" }) ||

                        // Если вкладка Склад и есть доступ, то показываем
                        (tab.title == HomeTab.WAREHOUSE.title && bmList.any { it.name == "Warehouse management" })
                    ) {
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            selectedTabIndex = selectedTabIndex,
            onNavigate = { },
            permissions = permissions,
            isUserAssetsAdmin = false,
            bmList = listOf(
                BmListDto(
                    icon = "warehouse",
                    icon1 = "history",
                    idMenu1 = "18",
                    idMenu2 = "45",
                    link = "warehouse_store_history",
                    mainLevel = "18",
                    name = "Warehouse  management",
                    name1 = "warehouse_store_history",
                    nameRule = "qrcode_pda",
                )
            )
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
            onNavigate = { },
            permissions = emptyList(),
            isUserAssetsAdmin = true,
            bmList = emptyList()
        )
    }
}