package com.gps.warehouse.ui.assets_screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.NotificationDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ==========================================
// МОДЕЛИ ФИЛЬТРОВ
// ==========================================
enum class DirectionFilter(val title: String, val apiValue: String?) {
    ALL("Все", null),
    INCOMING("Входящие", "incoming"),
    OUTGOING("Исходящие", "outgoing")
}

enum class StatusFilter(val title: String, val apiValue: String?) {
    ALL("Все", null),
    UNREAD("Не прочитанные", "unread"),
    READ("Прочитанные", "read"),
    DECLINED("Отклоненные", "declined")
}

data class NotificationFilterState(
    val searchQuery: String = "",
    val direction: DirectionFilter = DirectionFilter.INCOMING,
    val status: StatusFilter = StatusFilter.UNREAD
)

// ==========================================
// SCREEN: Управление состоянием и навигацией
// ==========================================
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    highlightNotificationId: Int? = null,
    onHighlightHandled: () -> Unit = {},
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Состояние фильтров храним на уровне экрана
    var filterState by remember { mutableStateOf(NotificationFilterState()) }
    // Состояние раскрытия шторки фильтров
    var isFiltersExpanded by remember { mutableStateOf(false) }
    // Состояние списка для скролла
    val listState = rememberLazyListState()
    var highlightedId by remember { mutableStateOf<Int?>(null) }

    // Загружаем уведомления при первом открытии экрана
    LaunchedEffect(Unit) {
        if (uiState !is AssetViewModel.AssetUiState.NotificationsLoaded &&
            uiState !is AssetViewModel.AssetUiState.Loading) {
            viewModel.loadNotifications()
        }
    }

    // Обработка прокрутки и включения подсветки
    LaunchedEffect(highlightNotificationId, uiState) {
        if (highlightNotificationId != null &&
            uiState is AssetViewModel.AssetUiState.NotificationsLoaded &&
            highlightedId != highlightNotificationId) {

            // Сбрасываем фильтры, чтобы элемент гарантированно попал в видимый список
            filterState = NotificationFilterState()

            // Короткая пауза, чтобы UI успел перерисовать список без фильтров
            delay(100)

            val currentList = (uiState as AssetViewModel.AssetUiState.NotificationsLoaded).notifications
            val index = currentList.indexOfFirst { it.notificationId == highlightNotificationId }

            if (index != -1) {
                listState.animateScrollToItem(index)
                highlightedId = highlightNotificationId
                onHighlightHandled() // Сбрасываем ID в ViewModel
            }
        }
    }

    // Таймер для снятия подсветки
    LaunchedEffect(highlightedId) {
        if (highlightedId != null) {
            delay(3000)
            highlightedId = null // Через 3 секунды цвет плавно вернется к исходному
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
            listState = listState,
            highlightedId = highlightedId,
            filterState = filterState,
            isFiltersExpanded = isFiltersExpanded,
            onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
            onFilterStateChange = { filterState = it },
            onRetry = { viewModel.loadNotifications() },
            onNotificationClick = { notification ->
                when {
                    notification.assetId != null -> {
                        navController.navigate("asset_details/${notification.assetId}")
                    }
                    notification.sessionId != null -> {
                        navController.navigate("inventorization_items/${notification.sessionId}/false")
                    }
                }
            }
        )
    }
}

// ==========================================
// CONTENT: Чистый UI с логикой фильтрации
// ==========================================
@Composable
fun NotificationsContent(
    modifier: Modifier = Modifier,
    uiState: AssetViewModel.AssetUiState,
    listState: LazyListState,
    highlightedId: Int? = null,
    filterState: NotificationFilterState,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onFilterStateChange: (NotificationFilterState) -> Unit,
    onRetry: () -> Unit,
    onNotificationClick: (NotificationDto) -> Unit
) {
    when (uiState) {
        is AssetViewModel.AssetUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AssetViewModel.AssetUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ошибка: ${uiState.message}",
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
            // Применяем фильтрацию
            val filteredList = uiState.notifications.filter { notification ->
                val matchesDirection = filterState.direction.apiValue == null ||
                        notification.direction == filterState.direction.apiValue
                val matchesStatus = filterState.status.apiValue == null ||
                        notification.status == filterState.status.apiValue

                val query = filterState.searchQuery.trim()
                val matchesSearch = query.isBlank() ||
                        notification.eventTypeRu.contains(query, ignoreCase = true) ||
                        (notification.assetName?.contains(query, ignoreCase = true) == true) ||
                        (notification.initiatorFullName?.contains(query, ignoreCase = true) == true) ||
                        (notification.sessionId?.toString()?.contains(query) == true)

                matchesDirection && matchesStatus && matchesSearch
            }.sortedByDescending { it.createdAt }

            Column(modifier = modifier.fillMaxSize()) {
                // Панель фильтров
                NotificationFilterBar(
                    filterState = filterState,
                    isFiltersExpanded = isFiltersExpanded,
                    onToggleFilters = onToggleFilters,
                    onFilterStateChange = onFilterStateChange
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                // Список
                if (filteredList.isEmpty() && uiState.notifications.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ничего не найдено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Уведомлений нет",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.notificationId }) { notification ->
                            NotificationItem(
                                notification = notification,
                                isHighlighted = notification.notificationId == highlightedId,
                                onClick = { onNotificationClick(notification) }
                            )
                        }
                    }
                }
            }
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// ==========================================
// UI КОМПОНЕНТЫ: Панель фильтров
// ==========================================
@Composable
fun NotificationFilterBar(
    filterState: NotificationFilterState,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onFilterStateChange: (NotificationFilterState) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { onFilterStateChange(filterState.copy(searchQuery = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onFilterStateChange(filterState.copy(searchQuery = ""))
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Кнопка сворачивания/разворачивания фильтров
            IconButton(onClick = onToggleFilters) {
                Icon(
                    imageVector = if (isFiltersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isFiltersExpanded) "Скрыть фильтры" else "Показать фильтры"
                )
            }
        }

        // Анимированное раскрытие фильтров
        AnimatedVisibility(visible = isFiltersExpanded) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                // Фильтр по направлению
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DirectionFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = filterState.direction == filter,
                            onClick = { onFilterStateChange(filterState.copy(direction = filter)) },
                            label = { Text(filter.title, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Фильтр по статусу (горизонтальная прокрутка для экономии места на ТСД)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = filterState.status == filter,
                            onClick = { onFilterStateChange(filterState.copy(status = filter)) },
                            label = { Text(filter.title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// UI КОМПОНЕНТЫ: Элемент списка
// ==========================================
@Composable
fun NotificationItem(notification: NotificationDto, isHighlighted: Boolean = false, onClick: () -> Unit) {
    val highlightColor = when (notification.eventType) {
        "write_off_approved" -> Color(0xFF4CAF50)
        "write_off_rejected", "user_declined" -> Color(0xFFE53935)
        "inventory_started" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.primary
    }

    val bgColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> highlightColor.copy(alpha = 0.15f)
            notification.status == "unread" -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(800),
        label = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else Color.Transparent,
        animationSpec = tween(800),
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isHighlighted) 2.dp else 0.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.dp)
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
                    text = "id = ${notification.notificationId} | ",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatDateTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Инициатор: ${notification.initiatorFullName ?: "Неизвестно"}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!notification.assetName.isNullOrEmpty() || notification.sessionId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (notification.assetId != null) Icons.Default.Link else Icons.Default.RequestPage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (notification.assetName != null) "Актив: ${notification.assetName}" else "Сессия инвентаризации: №${notification.sessionId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    containerColor = when (notification.status) {
                        "unread" -> MaterialTheme.colorScheme.primary
                        "declined" -> MaterialTheme.colorScheme.error
                        else -> Color.Gray
                    }
                ) {
                    Text(
                        text = notification.statusRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.takeIf { notification.status != "read" } ?: Color.White
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
            notificationId = 111, employeeId = "0000010680", employeeFullName = "Малых Андрей Владимирович",
            assetId = null, sessionId = 12, eventType = "inventory_started", initiatorId = "0000015370",
            status = "unread", respondedAt = null, createdAt = "2026-08-27T16:01:39.042276",
            assetName = null, assetInventoryId = null, initiatorFullName = "Малышев Тимур Максимович",
            direction = "outgoing", directionRu = "Исходящее", eventTypeRu = "Запущена новая сессия", statusRu = "Не прочитано"
        ),
        NotificationDto(
            notificationId = 74, employeeId = "0000012657", employeeFullName = "Евсиков Константин Александрович",
            assetId = 49, sessionId = null, eventType = "user_declined", initiatorId = "0000015370",
            status = "declined", respondedAt = null, createdAt = "2026-08-26T12:29:14.756052",
            assetName = "Актив №3", assetInventoryId = "3333", initiatorFullName = "Малышев Тимур Максимович",
            direction = "outgoing", directionRu = "Входящее", eventTypeRu = "Сотрудник отклонил назначение", statusRu = "Отклонено"
        ),
        NotificationDto(
            notificationId = 62, employeeId = "0000015370", employeeFullName = "Малышев Тимур Максимович",
            assetId = 48, sessionId = null, eventType = "write_off_approved", initiatorId = "0000012657",
            status = "read", respondedAt = null, createdAt = "2026-08-26T12:26:04.199824",
            assetName = "Актив №2", assetInventoryId = "INV_NUMBER_48", initiatorFullName = "Евсиков Константин Александрович",
            direction = "incoming", directionRu = "Входящее", eventTypeRu = "Заявка на списание утверждена", statusRu = "Прочитано"
        )
    )

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                MyCustomActionBar(text = "Уведомления", onBackClick = {})
                NotificationsContent(
                    uiState = AssetViewModel.AssetUiState.NotificationsLoaded(mockNotifications),
                    listState = LazyListState(),
                    filterState = NotificationFilterState(),
                    isFiltersExpanded = false,
                    onToggleFilters = {},
                    onFilterStateChange = {},
                    onRetry = {},
                    onNotificationClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun NotificationsContentEmptyPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                MyCustomActionBar(text = "Уведомления", onBackClick = {})
                NotificationsContent(
                    uiState = AssetViewModel.AssetUiState.NotificationsLoaded(emptyList()),
                    listState = LazyListState(),
                    filterState = NotificationFilterState(),
                    isFiltersExpanded = false,
                    onToggleFilters = {},
                    onFilterStateChange = {},
                    onRetry = {},
                    onNotificationClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp,height=870dp")
@Composable
private fun NotificationsContentErrorPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                MyCustomActionBar(text = "Уведомления", onBackClick = {})
                NotificationsContent(
                    uiState = AssetViewModel.AssetUiState.Error("Ошибка сети. Проверьте подключение."),
                    listState = LazyListState(),
                    filterState = NotificationFilterState(),
                    isFiltersExpanded = false,
                    onToggleFilters = {},
                    onFilterStateChange = {},
                    onRetry = {},
                    onNotificationClick = {}
                )
            }
        }
    }
}