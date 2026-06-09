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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gps.warehouse.ui.assets_screens.assets.AssetDetailsScreen
import com.gps.warehouse.ui.assets_screens.assets.AssetListScreen
import com.gps.warehouse.ui.assets_screens.assets_types.AssetTypeListScreen
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
import com.gps.warehouse.utils.AppThemeMode
import com.gps.warehouse.utils.Constants
import com.gps.warehouse.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            // Проверка обновлений после успешного логина (вместо старого кода)
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
                    val viewModel: MainViewModel = hiltViewModel()

                    // Добавим observer для отслеживания изменений состояния ViewModel
                    LaunchedEffect(Unit) {
                        viewModel.uiState.collect { state ->
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
                                    // === ЗАПУСК ПРОВЕРКИ ОБНОВЛЕНИЙ ПОСЛЕ ВХОДА ===
//                                    checkForAppUpdate(navController, viewModel)
                                    checkForAppUpdate()
                                },
                                viewModel = viewModel
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                navController = navController,
                            )

                        }
                        composable("orders") {
                            OrdersScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("order_details/{orderNumber}") { backStackEntry ->
                            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
                            OrderDetailsScreen(
                                orderNumber = orderNumber,
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("receive/{orderNumber}") { backStackEntry ->
                            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
                            ReceiveMaterialsScreen(
                                orderNumber = orderNumber,
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("packaging") {
                            PackagingScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("pack_to_warehouse") {
                            PackToWarehouseScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("inventory") {
                            InventoryListScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("inventory_check/{orderNumber}") { backStackEntry ->
                            val orderNumber = backStackEntry.arguments?.getString("orderNumber") ?: ""
                            InventoryCheckScreen(
                                orderNumber = orderNumber,
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("wms") {
                            WmsScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("wms_requests") {
                            WmsRequestsScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("warehouse") {
                            WarehouseMaterialsScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("wms_receive") {
                            WmsReceiveScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("archive") {
                            ArchiveScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }

                        // В NavHost:
                        composable("assets") {
                            val assetViewModel: AssetViewModel = hiltViewModel()  // ← Отдельный ViewModel!
                            AssetListScreen(
                                navController = navController,
                                viewModel = assetViewModel
                            )
                        }

                        composable("asset_details/{assetId}") { backStackEntry ->
                            val assetId = backStackEntry.arguments?.getString("assetId")?.toIntOrNull() ?: 0
                            val assetViewModel: AssetViewModel = hiltViewModel()
                            AssetDetailsScreen(
                                assetId = assetId,
                                navController = navController,
                                viewModel = assetViewModel
                            )
                        }

                        // Типы активов
                        composable("asset_types") {
                            AssetTypeListScreen(
                                navController = navController
                            )
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
                                        onProgress = { /* опционально: показать уведомление */ }
                                    )
                                    if (success) {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Обновление загружено! Установите в Настройках.",
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