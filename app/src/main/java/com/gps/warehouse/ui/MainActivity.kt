package com.gps.warehouse.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gps.warehouse.ui.assets_screens.AssetDetailsScreen
import com.gps.warehouse.ui.assets_screens.AssetTypeListScreen
import com.gps.warehouse.ui.assets_screens.AssetsByTypeScreen
import com.gps.warehouse.ui.assets_screens.MobileDeviceDetailScreen
import com.gps.warehouse.ui.assets_screens.MobileDevicesScreen
import com.gps.warehouse.ui.assets_screens.MyAssetDetailScreen
import com.gps.warehouse.ui.assets_screens.MyAssetsScreen
import com.gps.warehouse.ui.assets_screens.MyPcDetailsScreen
import com.gps.warehouse.ui.assets_screens.MyPcsScreen
import com.gps.warehouse.ui.assets_screens.InventorizationSessionsScreen
import com.gps.warehouse.ui.assets_screens.InventorizationItemsScreen
import com.gps.warehouse.ui.assets_screens.map.AssetMapWebViewScreen
import com.gps.warehouse.ui.components.UpdateDialog
import com.gps.warehouse.ui.gps_screens.archive.ArchiveScreen
import com.gps.warehouse.ui.home.HomeScreen
import com.gps.warehouse.ui.gps_screens.inventory.InventoryCheckScreen
import com.gps.warehouse.ui.gps_screens.inventory.InventoryListScreen
import com.gps.warehouse.ui.login.LoginScreen
import com.gps.warehouse.ui.gps_screens.archive.OrderDetailsScreen
import com.gps.warehouse.ui.gps_screens.orders.OrdersScreen
import com.gps.warehouse.ui.gps_screens.packaging.PackagingScreen
import com.gps.warehouse.ui.gps_screens.packtowarehouse.PackToWarehouseScreen
import com.gps.warehouse.ui.gps_screens.profile.ProfileScreen
import com.gps.warehouse.ui.gps_screens.orders.ReceiveMaterialsScreen
import com.gps.warehouse.ui.gps_screens.settings.SettingsScreen
import com.gps.warehouse.ui.gps_screens.warehouse.WarehouseMaterialsScreen
import com.gps.warehouse.ui.gps_screens.warehouse.WmsReceiveScreen
import com.gps.warehouse.ui.gps_screens.warehouse.WmsRequestsScreen
import com.gps.warehouse.ui.gps_screens.warehouse.WmsScreen
import com.gps.warehouse.ui.gps_screens.warehouse.WmsWriteOffScreen
import com.gps.warehouse.utils.AppThemeMode
import com.gps.warehouse.utils.Constants
import com.gps.warehouse.utils.DataWedgeProfileManager
import com.gps.warehouse.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Автоматическая настройка DataWedge для Zebra-устройств
        DataWedgeProfileManager.ensureProfileExists(this)

        setContent {
            // Наблюдаем за темой из ViewModel
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            var showUpdateDialog by remember { mutableStateOf<UpdateManager.VersionInfo?>(null) }
            var currentVersionName by remember { mutableStateOf("1.0.0") }
            val updateManager = remember { UpdateManager(this) }
            val scope = rememberCoroutineScope()

            // Определяем DarkTheme на основе выбранного режима
            val darkTheme = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> {
                    // Используем системную настройку
                    val config = LocalConfiguration.current
                    config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                }
            }

            // Получение текущей версии
            LaunchedEffect(Unit) {
                try {
                    val info = packageManager.getPackageInfo(packageName, 0)
                    currentVersionName = info.versionName ?: "1.0.0"
                } catch (e: Exception) { /* ignore */ }
            }

            // Проверка обновлений после успешного логина
            fun checkForAppUpdate() {
                scope.launch {
                    val remote = updateManager.checkForUpdates(Constants.BASE_URL_UPDATE)
                    val currentCode = try {
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    } catch (e: Exception) { 0 }

                    if (remote != null && remote.versionCode > currentCode) {
                        showUpdateDialog = remote // Показываем диалог через Compose
                    }
                }
            }

            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
                typography = Typography()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val mainViewModel: MainViewModel = hiltViewModel()

                    // Добавим observer для отслеживания изменений состояния ViewModel
                    LaunchedEffect(Unit) {
                        mainViewModel.uiState.collect { state ->
                            if (state is MainViewModel.UiState.SessionExpired) {
                                val currentRoute = navController.currentBackStackEntry?.destination?.route
                                if (currentRoute != "login") {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                    // ЗАПУСК ПРОВЕРКИ ОБНОВЛЕНИЙ ПОСЛЕ ВХОДА
                                    checkForAppUpdate()
                                },
                                viewModel = mainViewModel
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("orders") {
                            OrdersScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("order_details/{orderNumber}") { backStackEntry ->
                            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
                            OrderDetailsScreen(
                                orderNumber = orderNumber,
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("receive/{orderNumber}") { backStackEntry ->
                            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
                            ReceiveMaterialsScreen(
                                orderNumber = orderNumber,
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("packaging") {
                            PackagingScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("pack_to_warehouse") {
                            PackToWarehouseScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("inventory") {
                            InventoryListScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("inventory_check/{orderNumber}") { backStackEntry ->
                            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
                            InventoryCheckScreen(
                                orderNumber = orderNumber,
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("wms") {
                            WmsScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("wms_requests") {
                            WmsRequestsScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("warehouse") {
                            WarehouseMaterialsScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("wms_receive") {
                            WmsReceiveScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("wms_write_off") {
                            WmsWriteOffScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("archive") {
                            ArchiveScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                navController = navController,
                                viewModel = mainViewModel
                            )
                        }


                        // АКТИВЫ
                        composable("asset_types") {
                            val assetViewModel: AssetViewModel = hiltViewModel()
                            AssetTypeListScreen(
                                navController = navController,
                                assetViewModel = assetViewModel,
                            )
                        }

                        composable("assets_list/{assetTypeId}/{assetTypeName}") {backStackEntry ->
                            val assetTypeId = backStackEntry.arguments?.getString("assetTypeId")?.toIntOrNull() ?: 0
                            val assetTypeName = backStackEntry.arguments?.getString("assetTypeName").toString()
                            val assetViewModel: AssetViewModel = hiltViewModel()
                            AssetsByTypeScreen(
                                assetTypeId = assetTypeId,
                                assetTypeName = assetTypeName,
                                navController = navController,
                                viewModel = assetViewModel
                            )
                        }

                        composable("mobile_devices") {
                            MobileDevicesScreen(
                                onDeviceClick = { serialNumber ->
                                    navController.navigate("mobile_device_detail/$serialNumber")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "mobile_device_detail/{serialNumber}",
                            arguments = listOf(navArgument("serialNumber") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val serialNumber = backStackEntry.arguments?.getString("serialNumber") ?: return@composable
                            MobileDeviceDetailScreen(
                                serialNumber = serialNumber,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("my_assets_list") {
                            val assetViewModel: AssetViewModel = hiltViewModel()
                            MyAssetsScreen(
                                navController = navController,
                                viewModel = assetViewModel,
                            )
                        }

                        composable("my_asset_details/{assetId}") { backStackEntry ->
                            val assetId = backStackEntry.arguments?.getString("assetId")?.toIntOrNull() ?: 0
                            val assetViewModel: AssetViewModel = hiltViewModel()
                            MyAssetDetailScreen(
                                assetId = assetId,
                                navController = navController,
                                viewModel = assetViewModel
                            )
                        }


                        composable("asset_details/{assetId}") { backStackEntry ->
                            val assetId = backStackEntry.arguments?.getString("assetId")?.toIntOrNull() ?: return@composable
                            AssetDetailsScreen(
                                assetId = assetId,
                                navController = navController,
                            )
                        }

                        composable("my_pcs") {
                            MyPcsScreen(
                                navController = navController,
                            )
                        }

                        composable("my_pc_details/{pcId}") { backStackEntry ->
                            val pcId = backStackEntry.arguments?.getString("pcId")?.toIntOrNull() ?: 0
                            MyPcDetailsScreen(
                                pcId = pcId,
                                navController = navController,
                            )
                        }

                        // Инвентаризация активов
                        composable("inventorization_sessions") {
                            InventorizationSessionsScreen(
                                navController = navController,
                            )
                        }

                        composable("inventorization_items/{sessionId}/{isCompleted}") { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toIntOrNull() ?: 0
                            val isCompleted = backStackEntry.arguments?.getString("isCompleted")?.toBoolean() ?: false

                            InventorizationItemsScreen(
                                sessionId = sessionId,
                                isCompleted = isCompleted,
                                navController = navController,
                            )
                        }

                        composable("assets_map_web") {
                            AssetMapWebViewScreen(navController = navController)
                        }
                    }

                    // Показ диалога поверх всего контента (после NavHost):
                    if (showUpdateDialog != null) {
                        UpdateDialog(
                            currentVersionName = currentVersionName,
                            remoteVersion = showUpdateDialog!!,
                            onDownload = {
                                scope.launch {
                                    val success = updateManager.downloadUpdate(
                                        baseUrl = Constants.BASE_URL_UPDATE,
                                        onProgress = { /* Прогресс теперь отображается только в системном Notification */ }
                                    )
                                    if (success) {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Обновление загружено! Запуск установки...",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Ошибка загрузки",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onDismiss = { showUpdateDialog = null }
                        )
                    }
                }
            }
        }
    }
}