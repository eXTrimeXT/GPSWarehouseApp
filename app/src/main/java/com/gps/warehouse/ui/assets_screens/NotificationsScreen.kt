package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.NotificationDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ==========================================
// SCREEN: Управление состоянием и навигацией
// ==========================================
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Загружаем уведомления при первом открытии экрана
    LaunchedEffect(Unit) {
        if (uiState !is AssetViewModel.AssetUiState.NotificationsLoaded &&
            uiState !is AssetViewModel.AssetUiState.Loading) {
            viewModel.loadNotifications()
        }
    }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Уведомления",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        NotificationsContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onRetry = { viewModel.loadNotifications() },
            onNotificationClick = { notification ->
                // Логика навигации согласно ТЗ:
                when {
                    notification.assetId != null -> {
                        navController.navigate("asset_details/${notification.assetId}")
                    }
                    notification.sessionId != null -> {
                        // isCompleted = false по умолчанию для активных сессий
                        navController.navigate("inventorization_items/${notification.sessionId}/false")
                    }
                }
            }
        )
    }
}

// ==========================================
// CONTENT: Чистый UI без побочных эффектов
// ==========================================
@Composable
fun NotificationsContent(
    modifier: Modifier = Modifier,
    uiState: AssetViewModel.AssetUiState,
    onRetry: () -> Unit,
    onNotificationClick: (NotificationDto) -> Unit
) {
    when (val state = uiState) {
        is AssetViewModel.AssetUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AssetViewModel.AssetUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ошибка: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("Повторить")
                    }
                }
            }
        }
        is AssetViewModel.AssetUiState.NotificationsLoaded -> {
            if (state.notifications.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Уведомлений нет",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.notifications, key = { it.notificationId }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
        else -> {
            // Idle или другие состояния: показываем пустой экран или лоадер
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// ==========================================
// UI КОМПОНЕНТЫ
// ==========================================
@Composable
fun NotificationItem(notification: NotificationDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.status == "unread")
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.eventTypeRu,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDateTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Инициатор: ${notification.initiatorFullName ?: "Неизвестно"}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!notification.assetName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Актив: ${notification.assetName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    containerColor = if (notification.status == "unread")
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Gray
                ) {
                    Text(
                        text = notification.statusRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (notification.status == "unread")
                            MaterialTheme.colorScheme.onPrimary
                        else
                            Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = notification.directionRu,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Безопасное форматирование даты для Android 11+ (java.time доступен с API 26)
fun formatDateTime(isoString: String): String {
    return try {
        // Берем первые 19 символов: "2026-08-27T16:01:39" (игнорируем доли секунды)
        val dateTime = LocalDateTime.parse(isoString.take(19))
        val outputFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm", Locale.getDefault())
        dateTime.format(outputFormatter)
    } catch (e: Exception) {
        isoString
    }
}

// ==========================================
// PREVIEW
// ==========================================
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun NotificationsContentPreview() {
    val mockNotifications = listOf(
        NotificationDto(
            notificationId = 101,
            employeeId = "0000010680",
            employeeFullName = "Малых Андрей Владимирович",
            assetId = null,
            sessionId = 12,
            eventType = "inventory_started",
            initiatorId = "0000015370",
            status = "unread",
            respondedAt = null,
            createdAt = "2026-08-27T16:01:39.042276",
            assetName = null,
            assetInventoryId = null,
            initiatorFullName = "Малышев Тимур Максимович",
            direction = "outgoing",
            directionRu = "Исходящее",
            eventTypeRu = "Вы запустили новую сессию инвентаризации",
            statusRu = "Не прочитано"
        ),
        NotificationDto(
            notificationId = 64,
            employeeId = "0000012657",
            employeeFullName = "Евсиков Константин Александрович",
            assetId = 49,
            sessionId = null,
            eventType = "user_declined",
            initiatorId = "0000015370",
            status = "read",
            respondedAt = null,
            createdAt = "2026-08-26T12:29:14.756052",
            assetName = "Актив №3",
            assetInventoryId = "3333",
            initiatorFullName = "Малышев Тимур Максимович",
            direction = "outgoing",
            directionRu = "Исходящее",
            eventTypeRu = "Сотрудник отклонил ваше назначение пользователем",
            statusRu = "Прочитано"
        ),
        NotificationDto(
            notificationId = 62,
            employeeId = "0000015370",
            employeeFullName = "Малышев Тимур Максимович",
            assetId = 48,
            sessionId = null,
            eventType = "write_off_approved",
            initiatorId = "0000012657",
            status = "unread",
            respondedAt = null,
            createdAt = "2026-08-26T12:26:04.199824",
            assetName = "Актив №2",
            assetInventoryId = "INV_NUMBER_48",
            initiatorFullName = "Евсиков Константин Александрович",
            direction = "incoming",
            directionRu = "Входящее",
            eventTypeRu = "Ваша заявка на списание утверждена",
            statusRu = "Не прочитано"
        )
    )

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MyCustomActionBar(
                text = "Уведомления",
                onBackClick = {}
            )
            NotificationsContent(
                uiState = AssetViewModel.AssetUiState.NotificationsLoaded(mockNotifications),
                onRetry = {},
                onNotificationClick = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun NotificationsContentEmptyPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationsContent(
                uiState = AssetViewModel.AssetUiState.NotificationsLoaded(emptyList()),
                onRetry = {},
                onNotificationClick = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun NotificationsContentErrorPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotificationsContent(
                uiState = AssetViewModel.AssetUiState.Error("Ошибка сети. Проверьте подключение."),
                onRetry = {},
                onNotificationClick = {}
            )
        }
    }
}