package com.gps.warehouse.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.dto.UserProfileResponse
import com.gps.warehouse.data.remote.dto.WarehousePermissionDto
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar

@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Состояние для диалога подтверждения выхода
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Диалог подтверждения выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Выход из системы") },
            text = {
                Text("Вы уверены, что хотите выйти? Вам потребуется снова ввести логин и пароль.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                        // Навигация на экран входа с очисткой стека
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    ProfileContent(
        uiState = uiState,
        onLogoutClick = {
            // Вместо немедленного выхода, показываем диалог
            showLogoutDialog = true
        },
        onBackClick = {
            navController.popBackStack()
        },
        onRetryClick = {
            viewModel.loadUserProfile()
        }
    )
}

@Composable
fun ProfileContent(
    uiState: MainViewModel.UiState,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок с кнопкой назад через кастомный ActionBar
        MyCustomActionBar(onBackClick = onBackClick, text = "Профиль")

        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }
            is MainViewModel.UiState.ProfileLoaded -> {
                val profile = uiState.profile

                // Основной контент с прокруткой
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Основная информация в карточке
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Логин
                            ProfileItem(
                                icon = Icons.Default.Person,
                                label = "Логин",
                                value = profile.login
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Отдел
                            ProfileItem(
                                icon = Icons.Default.Business,
                                label = "Отдел",
                                value = profile.section ?: "Не указан"
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Последний вход
                            ProfileItem(
                                icon = Icons.Default.AccessTime,
                                label = "Последний вход",
                                value = profile.lastTime ?: "Неизвестно"
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // IP адрес
                            ProfileItem(
                                icon = Icons.Default.Computer,
                                label = "IP адрес",
                                value = profile.lastIp ?: "Неизвестно"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Права на склады
                    if (!profile.warehousePermissions.isNullOrEmpty()) {
                        CollapsibleWarehouseCard(permissions = profile.warehousePermissions)
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Нет доступных складов",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Кнопка выхода
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выйти из системы", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            is MainViewModel.UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetryClick,
                    modifier = Modifier.weight(1f)
                )
            }
            else -> { CustomLoadingView() }
        }
    }
}

@Composable
fun ProfileItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CollapsibleWarehouseCard(permissions: List<WarehousePermissionDto>) {
    // Состояние: развернута ли карточка
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            // Переключаем состояние при клике на всю карточку
            isExpanded = !isExpanded
        }
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            // Заголовок карточки (всегда виден)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warehouse,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Доступные склады (${permissions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Иконка стрелочки, которая поворачивается
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Контент, который показывается только при развернутом состоянии
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Анимация появления (опционально)
                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        permissions.forEach { permission ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.HouseSiding,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Склад: ${permission.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (permission.isLeader == "1") {
                                        Text(
                                            text = "Полные права",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (permission.isVirtual == "1") {
                                        Text(
                                            text = "Виртуальный склад",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ПРЕВЬЮ ---
@Preview(showBackground = true, name = "Profile - Loaded")
@Composable
fun ProfilePreviewLoaded() {
    val testProfile = UserProfileResponse(
        id = "91",
        login = "ivanov_aa",
        section = "Складская логистика",
        lastTime = "14:33:23 27.04.2026",
        lastIp = "192.168.1.105",
        warehousePermissions = listOf(
            WarehousePermissionDto(id = "1", name = "3051", isLeader = "1", isVirtual = "0"),
            WarehousePermissionDto(id = "2", name = "4007", isLeader = "0", isVirtual = "0"),
            WarehousePermissionDto(id = "3", name = "Архив", isLeader = "0", isVirtual = "1")
        )
    )
    MaterialTheme {
        Surface {
            ProfileContent(
                uiState = MainViewModel.UiState.ProfileLoaded(testProfile),
                onLogoutClick = {},
                onBackClick = {},
                onRetryClick = {}
            )
        }
    }
}

//@Preview(showBackground = true, name = "Profile - Error")
//@Composable
//fun ProfilePreviewError() {
//    MaterialTheme {
//        Surface {
//            ProfileContent(
//                uiState = MainViewModel.UiState.Error("Сервер недоступен"),
//                onLogoutClick = {},
//                onBackClick = {},
//                onRetryClick = {}
//            )
//        }
//    }
//}