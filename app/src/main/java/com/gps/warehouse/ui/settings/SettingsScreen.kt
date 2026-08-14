package com.gps.warehouse.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.BmListDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.AppIconDisplay
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.UpdateDialog
import com.gps.warehouse.ui.home.HomeTab
import com.gps.warehouse.ui.home.isTabFilter
import com.gps.warehouse.utils.AppPreferences
import com.gps.warehouse.utils.Constants
import com.gps.warehouse.utils.UpdateManager
import kotlinx.coroutines.launch

// ====================== 1. SCREEN (Логика + ViewModel) ======================
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    // Состояния UI
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remoteVersion by remember { mutableStateOf<UpdateManager.VersionInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Данные из ViewModel
    val gpsPermissions by viewModel.gpsPermissions.collectAsState()
    val isUserAssetsAdmin by viewModel.userIsAssetsAdmin.collectAsState()
    val bmList by viewModel.bmList.collectAsState()

    // Для камеры
    val cameraScanEnabled by viewModel.cameraScanEnabled.collectAsState()

    // Флаг: есть ли права доступа
    val hasPermissions = gpsPermissions.any { it.read } || isUserAssetsAdmin

    // Текущая версия приложения
    val (currentVersionCode, currentVersionName) = remember {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionCode to (info.versionName ?: "1.0.0")
    }

    // Сохранённый индекс вкладки
    var savedTabIndex by remember { mutableIntStateOf(AppPreferences.getDefaultTab(context)) }

    // Корректировка вкладки если нет прав на Assets
    LaunchedEffect(hasPermissions, savedTabIndex) {
        if (savedTabIndex == 2 && !hasPermissions) {
            AppPreferences.setDefaultTab(context, 1)
            savedTabIndex = 1
        }
    }

    SettingsContent(
        cameraScanEnabled = cameraScanEnabled,
        onCameraScanToggle = { viewModel.setCameraScanEnabled(it) },
        currentVersionName = currentVersionName,
        currentVersionCode = currentVersionCode,
        savedTabIndex = savedTabIndex,
        bmList = bmList,
        isChecking = isChecking,
        isDownloading = isDownloading,
        downloadProgress = downloadProgress,
        errorMessage = errorMessage,
        remoteVersion = remoteVersion,
        showUpdateDialog = showUpdateDialog,
        onBackClick = { navController.popBackStack() },
        onTabSelected = { index ->
            savedTabIndex = index
            AppPreferences.setDefaultTab(context, index)
        },
        onCheckUpdates = {
            isChecking = true
            errorMessage = null
            scope.launch {
                val remote = updateManager.checkForUpdates(Constants.BASE_URL_UPDATE)
                isChecking = false
                if (remote != null) {
                    remoteVersion = remote
                    if (remote.versionCode > currentVersionCode) {
                        showUpdateDialog = true
                    } else {
                        Toast.makeText(context, "Установлена последняя версия", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    errorMessage = "Ошибка подключения к серверу обновлений."
                }
            }
        },
        onDownloadUpdate = {
            showUpdateDialog = false
            isDownloading = true
            downloadProgress = 0
            errorMessage = null
            scope.launch {
                val success = updateManager.downloadUpdate(
                    baseUrl = Constants.BASE_URL_UPDATE,
                    onProgress = { progress -> downloadProgress = progress }
                )
                isDownloading = false
                if (!success) {
                    errorMessage = "Ошибка загрузки APK. Попробуйте позже."
                }
            }
        },
        onDismissUpdateDialog = { showUpdateDialog = false }
    )
}

// ====================== 2. CONTENT (Чистый UI) ======================
@Composable
fun SettingsContent(
    cameraScanEnabled: Boolean,
    onCameraScanToggle: (Boolean) -> Unit,
    currentVersionName: String,
    currentVersionCode: Int,
    savedTabIndex: Int,
    bmList: List<BmListDto>,
    isChecking: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    errorMessage: String?,
    remoteVersion: UpdateManager.VersionInfo?,
    showUpdateDialog: Boolean,
    onBackClick: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onDismissUpdateDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(onBackClick = onBackClick, text = "Настройки")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Иконка приложения
            if (Constants.BASE_URL_API.contains("gps-rs")) {
                AppIconDisplay()
            }

            // ================== НАСТРОЙКА ВКЛАДКИ ПО УМОЛЧАНИЮ ==================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Интерфейс", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Вкладка, открываемая при запуске", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = HomeTab.entries[savedTabIndex].title,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = expanded, onDismissRequest = { expanded = false }) {
                            HomeTab.entries.forEachIndexed { index, tab ->
                                if (isTabFilter(bmList, tab)) {
                                    DropdownMenuItem(text = { Text(tab.title) }, onClick = {
                                        onTabSelected(index)
                                        expanded = false
                                    }, leadingIcon = {
                                        if (savedTabIndex == index) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }

            // === НОВЫЙ БЛОК: Сканирование камерой ===
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Сканирование камерой", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Использовать камеру телефона для сканирования",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cameraScanEnabled,
                        onCheckedChange = onCameraScanToggle
                    )
                }
            }

            // Кнопка проверки обновлений
            Button(
                onClick = onCheckUpdates,
                enabled = !isChecking && !isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Update, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Проверить обновления")
                }
            }

            // Прогресс загрузки
            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Загрузка: $downloadProgress%",
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Ошибка
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Карточка с информацией
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("О приложении", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Версия: $currentVersionName")
                    Text("Версия кода: $currentVersionCode")

                    remoteVersion?.let { remote ->
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )
                        Text("Доступна:", color = MaterialTheme.colorScheme.primary)
                        Text("Версия: ${remote.versionName}", color = MaterialTheme.colorScheme.primary)
                        Text("Версия кода: ${remote.versionCode}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Диалог обновления
        if (showUpdateDialog && remoteVersion != null) {
            UpdateDialog(
                currentVersionName = currentVersionName,
                remoteVersion = remoteVersion,
                onDownload = onDownloadUpdate,
                onDismiss = onDismissUpdateDialog
            )
        }
    }
}

// ====================== 3. ПРЕВЬЮ ======================
@Preview(showBackground = true, name = "Settings - Downloading")
@Composable
fun SettingsContentPreview_Downloading() {
    MaterialTheme {
        Surface {
            SettingsContent(
                cameraScanEnabled = false,
                onCameraScanToggle = {},
                currentVersionName = "1.2.3",
                currentVersionCode = 42,
                savedTabIndex = 1,
                bmList = emptyList(),
                isChecking = false,
                isDownloading = true,
                downloadProgress = 67,
                errorMessage = null,
                remoteVersion = null,
                showUpdateDialog = false,
                onBackClick = {},
                onTabSelected = {},
                onCheckUpdates = {},
                onDownloadUpdate = {},
                onDismissUpdateDialog = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings - Update Available")
@Composable
fun SettingsContentPreview_UpdateAvailable() {
    MaterialTheme {
        Surface {
            SettingsContent(
                cameraScanEnabled = true,
                onCameraScanToggle = {},
                currentVersionName = "1.2.3",
                currentVersionCode = 42,
                savedTabIndex = 1,
                bmList = emptyList(),
                isChecking = false,
                isDownloading = false,
                downloadProgress = 0,
                errorMessage = null,
                remoteVersion = UpdateManager.VersionInfo(
                    jobId = 123,
                    version = 1,
                    versionCode = 43,
                    versionName = "1.3.0"
                ),
                showUpdateDialog = false,
                onBackClick = {},
                onTabSelected = {},
                onCheckUpdates = {},
                onDownloadUpdate = {},
                onDismissUpdateDialog = {}
            )
        }
    }
}
