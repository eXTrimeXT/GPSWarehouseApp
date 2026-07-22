package com.gps.warehouse.ui.gps_screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.UpdateDialog
import com.gps.warehouse.ui.home.HomeTab
import com.gps.warehouse.utils.AppPreferences
import com.gps.warehouse.utils.Constants
import com.gps.warehouse.utils.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }

    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var remoteVersion by remember { mutableStateOf<UpdateManager.VersionInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    val gpsPermissions by viewModel.gpsPermissions.collectAsState()
    val isUserAssetsAdmin by viewModel.userIsAssetsAdmin.collectAsState()

    // Флаг прав, есть ли хотя бы 1 элемент доступа
    val isPermissions = gpsPermissions.any{ it.read } || isUserAssetsAdmin

    val (currentVersionCode, currentVersionName) = remember {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionCode to (info.versionName ?: "1.0.0")
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        MyCustomActionBar(onBackClick = { navController.popBackStack() }, text = "Настройки")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================== НАСТРОЙКА ВКЛАДКИ ПО УМОЛЧАНИЮ ==================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Интерфейс", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Вкладка, открываемая при запуске", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    var expanded by remember { mutableStateOf(false) }

                    if (AppPreferences.getDefaultTab(context) == 2 && !isPermissions){
                        AppPreferences.setDefaultTab(context, 1)
                    }
                    var savedTabIndex by remember { mutableIntStateOf(AppPreferences.getDefaultTab(context)) }

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
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            HomeTab.entries.forEachIndexed { index, tab ->
                                if (
                                    tab.title != HomeTab.ASSETS.title ||
                                    (tab.title == HomeTab.ASSETS.title && isPermissions)
                                )
                                    DropdownMenuItem(
                                        text = { Text(tab.title) },
                                    onClick = {
                                        // Сохраняем выбор в SharedPreferences
                                        savedTabIndex = index
                                        AppPreferences.setDefaultTab(context, index)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (savedTabIndex == index) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Кнопка проверки обновлений
            Button(
                onClick = {
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
                                Toast.makeText(
                                    context,
                                    "Установлена последняя версия",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            errorMessage = "Ошибка подключения к серверу обновлений."
                        }
                    }
                },
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

            // Карточка с информацией (О приложении)
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
                        Text(
                            "Версия: ${remote.versionName}",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Версия кода: ${remote.versionCode}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Диалог подтверждения скачивания
        if (showUpdateDialog && remoteVersion != null) {
            UpdateDialog(
                currentVersionName = currentVersionName,
                remoteVersion = UpdateManager.VersionInfo(
                    jobId = remoteVersion!!.jobId,
                    version = remoteVersion!!.version,
                    versionCode = remoteVersion!!.versionCode,
                    versionName = remoteVersion!!.versionName
                ),
                onDownload = {
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
                        if (success) {
                            /* ничего не делает потому что запускается системный установщик */
                        } else {
                            errorMessage = "Ошибка загрузки APK. Попробуйте позже."
                        }
                    }
                },
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            navController = rememberNavController()
        )
    }
}