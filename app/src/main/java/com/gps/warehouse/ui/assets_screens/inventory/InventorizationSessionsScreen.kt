package com.gps.warehouse.ui.assets_screens.inventory

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.data.remote.gps_dto.PermissionDepartmentDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SplashLoadingScreen
import com.gps.warehouse.utils.formatIsoToReadable
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsScreen(
    navController: NavHostController,
    assetViewModel: AssetViewModel,
    mainViewModel: MainViewModel
) {
    val inventorizationSessions by assetViewModel.inventorizationSessions.collectAsState()
    val assetTypes by assetViewModel.assetTypes.collectAsState()
    val inventorizationUiState by assetViewModel.inventorizationUiState.collectAsState()

    // Получаем права по департаментам из getinfouser (gps-api)
    val permissionDepartments by mainViewModel.permissionDepartments.collectAsState()

    LaunchedEffect(Unit) {
        assetViewModel.loadAssetTypes()
        assetViewModel.loadInventorizationSessions()
        mainViewModel.loadPermissionDepartments()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    // используем Int? чтобы null означал "Отсутствие типа"
    var selectedAssetTypeId by remember { mutableStateOf<Int?>(null) }

    InventorizationSessionsContent(
        sessions = inventorizationSessions,
        assetTypes = assetTypes,
        permissionDepartments = permissionDepartments,
        uiState = inventorizationUiState,
        showCreateDialog = showCreateDialog,
        selectedAssetTypeId = selectedAssetTypeId,
        onSessionClick = { sessionId, isCompleted ->
            navController.navigate("inventorization_items/$sessionId/$isCompleted")
        },
        onShowCreateDialogChange = { showCreateDialog = it },
        onSelectedAssetTypeIdChange = { selectedAssetTypeId = it },
        onCreateSession = { assetTypeId, departmentCode, startDate, endDate ->
            assetViewModel.startInventorizationSession(
                assetTypeId = assetTypeId,
                departmentCode = departmentCode,
                startDate = startDate,
                endDate = endDate
            )
            showCreateDialog = false
        },
        onRetry = { assetViewModel.loadInventorizationSessions() },
        onBackClick = { navController.popBackStack() }
    )
}

// ==================== CONTENT: UI ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsContent(
    sessions: List<InventorizationSessionDto>?,
    assetTypes: List<AssetTypeDto>,
    permissionDepartments: List<PermissionDepartmentDto>,
    uiState: AssetViewModel.InventorizationUiState,
    showCreateDialog: Boolean,
    selectedAssetTypeId: Int?, // ИЗМЕНЕНО на Int?
    onSessionClick: (sessionId: Int, isCompleted: Boolean) -> Unit,
    onShowCreateDialogChange: (Boolean) -> Unit,
    onSelectedAssetTypeIdChange: (Int?) -> Unit,
    onCreateSession: (assetTypeId: Int?, departmentCode: String?, startDate: String?, endDate: String?) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MyCustomActionBar(
                text = "Инвентаризация",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onShowCreateDialogChange(true) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новая сессия")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is AssetViewModel.InventorizationUiState.Loading -> {
                SplashLoadingScreen()
            }
            is AssetViewModel.InventorizationUiState.SessionsLoaded -> {
                val items = sessions.orEmpty()
                if (items.isEmpty()) {
                    EmptySessionsState(modifier = Modifier.padding(paddingValues))
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { session ->
                            SessionCard(
                                session = session,
                                onClick = {
                                    val isCompleted = session.status == "completed"
                                    onSessionClick(session.sessionId, isCompleted)
                                }
                            )
                        }
                    }
                }
            }
            is AssetViewModel.InventorizationUiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {}
        }
    }

    // Диалог создания сессии
    if (showCreateDialog) {
        CreateInventorySessionDialog(
            assetTypes = assetTypes,
            selectedAssetTypeId = selectedAssetTypeId,
            permissionDepartments = permissionDepartments,
            onDismiss = { onShowCreateDialogChange(false) },
            onSelectedTypeChange = { onSelectedAssetTypeIdChange(it) }, // Передаем Int? напрямую
            onCreateSession = { assetTypeId, deptCode, start, end ->
                onCreateSession(assetTypeId, deptCode, start, end)
                onShowCreateDialogChange(false)
            }
        )
    }
}

// ==================== ДИАЛОГ: Отдельный Composable ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInventorySessionDialog(
    assetTypes: List<AssetTypeDto>,
    selectedAssetTypeId: Int?,
    permissionDepartments: List<PermissionDepartmentDto>,
    onDismiss: () -> Unit,
    onSelectedTypeChange: (Int?) -> Unit,
    onCreateSession: (assetTypeId: Int?, departmentCode: String?, startDate: String?, endDate: String?) -> Unit
) {
    // Состояния выбора
    var selectedDepartmentCodes by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Состояние активной вкладки (0 = Тип актива, 1 = Департаменты)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Тип актива", "Департаменты")

    // Состояния для даты и времени
    var startDateTimeMillis by remember { mutableStateOf<Long?>(null) }
    var endDateTimeMillis by remember { mutableStateOf<Long?>(null) }
    val startTimePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

    // Флаги видимости пикеров
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // ФИЛЬТР: Только департаменты с правом записи
    val writableDepartments = permissionDepartments.filter { it.write }

    // Вспомогательная функция форматирования в ISO 8601
    @SuppressLint("DefaultLocale")
    fun formatToIso(dateMillis: Long?, hour: Int, minute: Int): String? {
        if (dateMillis == null) return null
        val instant = Instant.ofEpochMilli(dateMillis)
        val zdt = instant.atZone(ZoneOffset.UTC)
        return String.format("%04d-%02d-%02dT%02d:%02d", zdt.year, zdt.monthValue, zdt.dayOfMonth, hour, minute)
    }

    // Вспомогательная функция для отображения текста в поле (компактный формат)
    @SuppressLint("DefaultLocale")
    fun getDisplayText(dateMillis: Long?, timeState: TimePickerState): String {
        if (dateMillis == null) return "Не выбрано"
        val instant = Instant.ofEpochMilli(dateMillis)
        val zdt = instant.atZone(ZoneOffset.UTC)
        val dateStr = zdt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val timeStr = String.format("%02d:%02d", timeState.hour, timeState.minute)
        return "$dateStr, $timeStr"
    }

    // ===== НОВОЕ: проверка диапазона дат =====
    val startFullMillis = remember(
        startDateTimeMillis,
        startTimePickerState.hour,
        startTimePickerState.minute
    ) {
        toDateTimeMillis(
            startDateTimeMillis,
            startTimePickerState.hour,
            startTimePickerState.minute
        )
    }
    val endFullMillis = remember(
        endDateTimeMillis,
        endTimePickerState.hour,
        endTimePickerState.minute
    ) {
        toDateTimeMillis(
            endDateTimeMillis,
            endTimePickerState.hour,
            endTimePickerState.minute
        )
    }
    val isEndBeforeStart = startFullMillis != null
            && endFullMillis != null
            && endFullMillis < startFullMillis
    val isDateRangeValid = startFullMillis != null
            && endFullMillis != null
            && !isEndBeforeStart

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AddBox, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Новая инвентаризация", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ================= ВКЛАДКИ =================
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = TabRowDefaults.secondaryContentColor,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                // При переключении вкладки очищаем выбор в другой категории для гарантии
                                if (index == 0) {
                                    selectedDepartmentCodes = emptySet()
                                } else {
                                    onSelectedTypeChange(null)
                                }
                            },
                            text = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // ================= КОНТЕНТ ВКЛАДОК =================
                when (selectedTabIndex) {
                    0 -> {
                        // Вкладка: Тип актива
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                if (assetTypes.isEmpty()) {
                                    Text(
                                        "Нет доступных типов активов!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                    )
                                }
                                assetTypes.forEach { type ->
                                    FilterChip(
                                        modifier = Modifier.fillMaxWidth(),
                                        selected = selectedAssetTypeId == type.assetTypeId,
                                        onClick = {
                                            selectedDepartmentCodes = emptySet()
                                            onSelectedTypeChange(type.assetTypeId)
                                        },

                                        label = { Text(type.name, softWrap = false) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Вкладка: Департаменты
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (writableDepartments.isEmpty()) {
                                Text(
                                    "Нет доступных департаментов с правом записи",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    writableDepartments.forEach { dept ->
                                        val code = dept.departmentCode ?: return@forEach
                                        FilterChip(
                                            selected = selectedDepartmentCodes.contains(code),
                                            onClick = {
                                                onSelectedTypeChange(null) // Очищаем тип при выборе департамента
                                                if (selectedDepartmentCodes.contains(code)) {
                                                    selectedDepartmentCodes = selectedDepartmentCodes - code
                                                } else {
                                                    selectedDepartmentCodes = selectedDepartmentCodes + code
                                                }
                                            },
                                            label = { Text(dept.nameGroup ?: dept.department ?: code) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // ================= ДАТЫ И ВРЕМЯ =================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Период инвентаризации:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = getDisplayText(startDateTimeMillis, startTimePickerState),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Начало") },
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        isError = isEndBeforeStart,
                    )

                    OutlinedTextField(
                        value = getDisplayText(endDateTimeMillis, endTimePickerState),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Окончание") },
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        isError = isEndBeforeStart,
                    )

                    if (isEndBeforeStart) {
                        Text(
                            text = "Дата окончания не может быть раньше даты начала",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isDateRangeValid) return@Button
                    onCreateSession(
                        if (selectedTabIndex == 0) selectedAssetTypeId else null,
                        if (selectedTabIndex == 1 && selectedDepartmentCodes.isNotEmpty())
                            selectedDepartmentCodes.joinToString(";") else null,
                        formatToIso(
                            startDateTimeMillis,
                            startTimePickerState.hour,
                            startTimePickerState.minute
                        ),
                        formatToIso(
                            endDateTimeMillis,
                            endTimePickerState.hour,
                            endTimePickerState.minute
                        )
                    )
                },
                enabled = isDateRangeValid && when (selectedTabIndex) {
                    0 -> selectedAssetTypeId != null
                    1 -> selectedDepartmentCodes.isNotEmpty()
                    else -> false
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    // ================= ДИАЛОГИ ВЫБОРА ДАТЫ И ВРЕМЕНИ =================
    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateTimeMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDateTimeMillis = state.selectedDateMillis
                    showStartDatePicker = false
                    if (state.selectedDateMillis != null) showStartTimePicker = true
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Отмена") } }
        ) { TimePicker(state = startTimePickerState) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateTimeMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDateTimeMillis = state.selectedDateMillis
                    showEndDatePicker = false
                    if (state.selectedDateMillis != null) showEndTimePicker = true
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Отмена") } }
        ) { TimePicker(state = endTimePickerState) }
    }
}

/** Собирает дату + время в epoch millis (UTC). */
private fun toDateTimeMillis(
    dateMillis: Long?,
    hour: Int,
    minute: Int
): Long? {
    if (dateMillis == null) return null
    val instant = Instant.ofEpochMilli(dateMillis)
    val zdt = instant.atZone(ZoneOffset.UTC)
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
    return zdt.toInstant().toEpochMilli()
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ COMPOSABLES ====================
@Composable
private fun EmptySessionsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inventory2,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Нет активных сессий",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SessionCard(session: InventorizationSessionDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (session.status) {
                    "pending" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    "in_progress" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    "completed" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (session.status) {
                            "pending" -> Icons.Default.Pending
                            "in_progress" -> Icons.Default.Sync
                            "completed" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Inventory2
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = when (session.status) {
                            "pending" -> MaterialTheme.colorScheme.outline
                            "in_progress" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "completed" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.assetTypeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Сессия #${session.sessionId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Дата начала: ${session.startDate?.formatIsoToReadable() ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Дата завершения: ${session.endDate.formatIsoToReadable() ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = when (session.status) {
                    "pending" -> MaterialTheme.colorScheme.surfaceVariant
                    "in_progress" -> MaterialTheme.colorScheme.primaryContainer
                    "completed" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = session.statusRu,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (session.status) {
                        "pending" -> MaterialTheme.colorScheme.outline
                        "in_progress" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "completed" -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

// ==================== PREVIEWS ====================
@PreviewLightDark
@Composable
private fun InventorizationSessionsContentPreview_Loaded() {
    val mockSessions = listOf(
        InventorizationSessionDto(
            sessionId = 1,
            assetTypeId = null,
            departmentCodes = null,
            assetTypeName = "Без типа",
            assetTypeEnName = "without_type",
            status = "pending",
            statusRu = "В ожидании",
            createdAt = "2026-07-23T08:59:53.158615Z",
            startDate = "2026-07-23T08:59:53.158615Z",
            endDate = "2026-07-30T08:59:53.158615Z",
            createdBy = "0000015370",
            createdByFullName = "ФИО"
        ),
        InventorizationSessionDto(
            sessionId = 2,
            assetTypeId = null,
            departmentCodes = null,
            assetTypeName = "Без типа",
            assetTypeEnName = "without_type",
            status = "in_progress",
            statusRu = "В работе",
            createdAt = "2026-07-23T08:59:53.158615Z",
            startDate = "2026-07-23T08:59:53.158615Z",
            endDate = "2026-07-30T08:59:53.158615Z",
            createdBy = "0000015370",
            createdByFullName = "ФИО"
        ),
        InventorizationSessionDto(
            sessionId = 3,
            assetTypeId = null,
            departmentCodes = null,
            assetTypeName = "Без типа",
            assetTypeEnName = "without_type",
            status = "completed",
            statusRu = "Завершено",
            createdAt = "2026-07-23T08:59:53.158615Z",
            startDate = "2026-07-23T08:59:53.158615Z",
            endDate = "2026-07-30T08:59:53.158615Z",
            createdBy = "0000015370",
            createdByFullName = "ФИО"
        ),
    )
    MaterialTheme {
        Surface {
            InventorizationSessionsContent(
                sessions = mockSessions,
                assetTypes = listOf(
                    AssetTypeDto(assetTypeId = 1, name = "Компьютеры", enName = "computers", createdBy = null, createdAt = "2026-07-06T07:18:41.873769", updatedAt = null),
                    AssetTypeDto(assetTypeId = 13, name = "Без типа", enName = "without_type", createdBy = null, createdAt = "2026-07-06T07:18:41.873769", updatedAt = null)
                ),
                permissionDepartments = listOf(
                    PermissionDepartmentDto(
                        department = "RDC",
                        nameGroup = "Активы RDC",
                        departmentCode = "RU10050099",
                        read = true,
                        write = true
                    )
                ),
                uiState = AssetViewModel.InventorizationUiState.SessionsLoaded(mockSessions),
                showCreateDialog = false,
                selectedAssetTypeId = null, // null вместо 0
                onSessionClick = { _, _ -> },
                onShowCreateDialogChange = {},
                onSelectedAssetTypeIdChange = {},
                onCreateSession = { _, _, _, _ -> },
                onRetry = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(device = "spec:width=1080px,height=2740px,dpi=440")
@Composable
private fun CreateInventorySessionDialogPreview() {
    MaterialTheme {
        CreateInventorySessionDialog(
            assetTypes = listOf(
                AssetTypeDto(0, "Без типа", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(2, "MES оборудование", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(3, "Расходные материалы", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(4, "Адаптер питания", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(5, "Оборудование сбора данных", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(6, "Комплектующие", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(7, "Сетевое оборудование", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(8, "Печатное оборудование", "without_type", null, "2026-07-06T07:21:39.334371", null),
                AssetTypeDto(9, "Серверное оборудование", "without_type", null, "2026-07-06T07:21:39.334371", null),
                AssetTypeDto(10, "Оборудование M&U", "without_type", null, "2026-07-06T07:21:39.334371", null),
            ),
            permissionDepartments = listOf(
                PermissionDepartmentDto(
                    department = "RDC",
                    nameGroup = "Активы RDC",
                    departmentCode = "RU01050099",
                    read = true,
                    write = true
                ),
                PermissionDepartmentDto(
                    department = "MSG",
                    nameGroup = "Активы RDC",
                    departmentCode = "RU01050020",
                    read = true,
                    write = true
                ),
                PermissionDepartmentDto(
                    department = null,
                    nameGroup = "Активы ",
                    departmentCode = "RU01000098",
                    read = true,
                    write = false
                )
            ),
            selectedAssetTypeId = null, // null вместо 1
            onDismiss = {},
            onSelectedTypeChange = {},
            onCreateSession = { _, _, _, _ -> },
        )
    }
}