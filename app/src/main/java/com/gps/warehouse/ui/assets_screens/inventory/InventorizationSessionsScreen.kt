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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.data.remote.assets_dto.SapAssetClassDto
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
    val assetClasses by assetViewModel.assetClasses.collectAsState()
    val assetClassesUiState by assetViewModel.assetClassesUiState.collectAsState()
    val inventorizationUiState by assetViewModel.inventorizationUiState.collectAsState()
    val permissionDepartments by mainViewModel.permissionDepartments.collectAsState()

    LaunchedEffect(Unit) {
        assetViewModel.loadAssetTypes()
        assetViewModel.loadAssetClasses()
        assetViewModel.loadInventorizationSessions()
        mainViewModel.loadPermissionDepartments()
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    // === НОВЫЕ множественные выборы ===
    var selectedAssetTypeIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedDepartmentCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedMaterialClasses by remember { mutableStateOf<Set<String>>(emptySet()) }

    // === Состояние создания сессии (для спиннера/ошибки) ===
    var isCreating by remember { mutableStateOf(false) }

    InventorizationSessionsContent(
        sessions = inventorizationSessions,
        assetTypes = assetTypes,
        assetClasses = assetClasses,
        assetClassesUiState = assetClassesUiState,
        permissionDepartments = permissionDepartments,
        uiState = inventorizationUiState,
        showCreateDialog = showCreateDialog,
        selectedAssetTypeIds = selectedAssetTypeIds,
        selectedDepartmentCodes = selectedDepartmentCodes,
        selectedMaterialClasses = selectedMaterialClasses,
        isCreating = isCreating,
        onSessionClick = { sessionId, isCompleted ->
            navController.navigate("inventorization_items/$sessionId/$isCompleted")
        },
        onShowCreateDialogChange = { showCreateDialog = it },
        onSelectedAssetTypeIdsChange = { selectedAssetTypeIds = it },
        onSelectedDepartmentCodesChange = { selectedDepartmentCodes = it },
        onSelectedMaterialClassesChange = { selectedMaterialClasses = it },
        onCreateSession = { assetTypeIds, departmentCodes, materialClasses, startDate, endDate ->
            isCreating = true
            assetViewModel.startInventorizationSession(
                assetTypeIds = assetTypeIds,
                departmentCodes = departmentCodes,
                materialClasses = materialClasses,
                startDate = startDate,
                endDate = endDate,
                onSuccess = {
                    isCreating = false
                    showCreateDialog = false
                    // Сбрасываем выбор
                    selectedAssetTypeIds = emptySet()
                    selectedDepartmentCodes = emptySet()
                    selectedMaterialClasses = emptySet()
                },
                onError = {
                    isCreating = false
                    // Ошибку покажет ViewModel через inventorizationUiState,
                    // но диалог оставим открытым, чтобы можно было поправить
                }
            )
        },
        onRetry = {
            assetViewModel.loadInventorizationSessions()
            assetViewModel.loadAssetClasses()
        },
        onBackClick = { navController.popBackStack() }
    )
}

// ==================== CONTENT: UI ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsContent(
    sessions: List<InventorizationSessionDto>?,
    assetTypes: List<AssetTypeDto>,
    assetClasses: List<SapAssetClassDto>,
    assetClassesUiState: AssetViewModel.AssetClassesUiState,
    permissionDepartments: List<PermissionDepartmentDto>,
    uiState: AssetViewModel.InventorizationUiState,
    showCreateDialog: Boolean,
    selectedAssetTypeIds: Set<Int>,
    selectedDepartmentCodes: Set<String>,
    selectedMaterialClasses: Set<String>,
    isCreating: Boolean,
    onSessionClick: (sessionId: Int, isCompleted: Boolean) -> Unit,
    onShowCreateDialogChange: (Boolean) -> Unit,
    onSelectedAssetTypeIdsChange: (Set<Int>) -> Unit,
    onSelectedDepartmentCodesChange: (Set<String>) -> Unit,
    onSelectedMaterialClassesChange: (Set<String>) -> Unit,
    onCreateSession: (
        assetTypeIds: List<Int>?,
        departmentCodes: List<String>?,
        materialClasses: List<String>?,
        startDate: String?,
        endDate: String?
    ) -> Unit,
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
            selectedAssetTypeIds = selectedAssetTypeIds,
            assetClasses = assetClasses,
            assetClassesUiState = assetClassesUiState,
            permissionDepartments = permissionDepartments,
            selectedDepartmentCodes = selectedDepartmentCodes,
            selectedMaterialClasses = selectedMaterialClasses,
            isCreating = isCreating,
            onDismiss = { if (!isCreating) onShowCreateDialogChange(false) },
            onSelectedAssetTypeIdsChange = onSelectedAssetTypeIdsChange,
            onSelectedDepartmentCodesChange = onSelectedDepartmentCodesChange,
            onSelectedMaterialClassesChange = onSelectedMaterialClassesChange,
            onCreateSession = { assetTypeIds, departmentCodes, materialClasses, start, end ->
                onCreateSession(assetTypeIds, departmentCodes, materialClasses, start, end)
            },
            onRetry = { onRetry }
        )
    }
}

// ==================== ДИАЛОГ ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInventorySessionDialog(
    assetTypes: List<AssetTypeDto>,
    selectedAssetTypeIds: Set<Int>,
    assetClasses: List<SapAssetClassDto>,
    assetClassesUiState: AssetViewModel.AssetClassesUiState,
    permissionDepartments: List<PermissionDepartmentDto>,
    selectedDepartmentCodes: Set<String>,
    selectedMaterialClasses: Set<String>,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onSelectedAssetTypeIdsChange: (Set<Int>) -> Unit,
    onSelectedDepartmentCodesChange: (Set<String>) -> Unit,
    onSelectedMaterialClassesChange: (Set<String>) -> Unit,
    onCreateSession: (
        assetTypeIds: List<Int>?,
        departmentCodes: List<String>?,
        materialClasses: List<String>?,
        startDate: String?,
        endDate: String?
    ) -> Unit,
    onRetry: () -> Unit,
) {
    // Активная вкладка (0 = Типы активов, 1 = Департаменты)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Типы активов", "Департаменты", "Классы SAP")

    // Даты/время
    var startDateTimeMillis by remember { mutableStateOf<Long?>(null) }
    var endDateTimeMillis by remember { mutableStateOf<Long?>(null) }
    val startTimePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

    // Видимость пикеров
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Только департаменты с правом записи
    val writableDepartments = permissionDepartments.filter { it.write }

    @SuppressLint("DefaultLocale")
    fun formatToIso(dateMillis: Long?, hour: Int, minute: Int): String? {
        if (dateMillis == null) return null
        val instant = Instant.ofEpochMilli(dateMillis)
        val zdt = instant.atZone(ZoneOffset.UTC)
        return String.format(
            "%04d-%02d-%02dT%02d:%02d",
            zdt.year, zdt.monthValue, zdt.dayOfMonth, hour, minute
        )
    }

    @SuppressLint("DefaultLocale")
    fun getDisplayText(dateMillis: Long?, timeState: TimePickerState): String {
        if (dateMillis == null) return "Не выбрано"
        val instant = Instant.ofEpochMilli(dateMillis)
        val zdt = instant.atZone(ZoneOffset.UTC)
        val dateStr = zdt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val timeStr = String.format("%02d:%02d", timeState.hour, timeState.minute)
        return "$dateStr, $timeStr"
    }

    // Валидация дат
    val startFullMillis = remember(startDateTimeMillis, startTimePickerState.hour, startTimePickerState.minute) {
        toDateTimeMillis(startDateTimeMillis, startTimePickerState.hour, startTimePickerState.minute)
    }
    val endFullMillis = remember(endDateTimeMillis, endTimePickerState.hour, endTimePickerState.minute) {
        toDateTimeMillis(endDateTimeMillis, endTimePickerState.hour, endTimePickerState.minute)
    }
    val isEndBeforeStart = startFullMillis != null && endFullMillis != null && endFullMillis < startFullMillis
    val isDateRangeValid = startFullMillis != null && endFullMillis != null && !isEndBeforeStart

    // Что выбрано — хотя бы что-то одно должно быть
    val hasAnySelection = selectedAssetTypeIds.isNotEmpty() ||
            selectedDepartmentCodes.isNotEmpty() ||
            selectedMaterialClasses.isNotEmpty()

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onDismissRequest = { if (!isCreating) onDismiss() },
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
                            onClick = { if (!isCreating) selectedTabIndex = index },
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

                    // ============ 0: ТИПЫ АКТИВОВ ============
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (assetTypes.isEmpty()) {
                                Text(
                                    "Нет доступных типов активов!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                // «Выбрать всё / Снять всё»
                                SelectionButtonsRow(
                                    total = assetTypes.size,
                                    selected = selectedAssetTypeIds.size,
                                    enabled = !isCreating,
                                    onSelectAll = {
                                        onSelectedAssetTypeIdsChange(assetTypes.map { it.assetTypeId }.toSet())
                                    },
                                    onClearAll = {
                                        onSelectedAssetTypeIdsChange(emptySet())
                                    }
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    assetTypes.forEach { type ->
                                        val isSelected = selectedAssetTypeIds.contains(type.assetTypeId)
                                        FilterChip(
                                            selected = isSelected,
                                            enabled = !isCreating,
                                            onClick = {
                                                val newSet = if (isSelected) {
                                                    selectedAssetTypeIds - type.assetTypeId
                                                } else {
                                                    selectedAssetTypeIds + type.assetTypeId
                                                }
                                                onSelectedAssetTypeIdsChange(newSet)
                                            },
                                            label = { Text(type.name, softWrap = false) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ============ 1: ДЕПАРТАМЕНТЫ ============
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (writableDepartments.isEmpty()) {
                                Text(
                                    "Нет доступных департаментов с правом записи",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                val allDeptCodes = writableDepartments
                                    .mapNotNull { it.departmentCode }
                                    .toSet()

                                SelectionButtonsRow(
                                    total = allDeptCodes.size,
                                    selected = selectedDepartmentCodes.size,
                                    enabled = !isCreating,
                                    onSelectAll = {
                                        onSelectedDepartmentCodesChange(allDeptCodes)
                                    },
                                    onClearAll = {
                                        onSelectedDepartmentCodesChange(emptySet())
                                    }
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    writableDepartments.forEach { dept ->
                                        val code = dept.departmentCode ?: return@forEach
                                        val isSelected = selectedDepartmentCodes.contains(code)
                                        FilterChip(
                                            selected = isSelected,
                                            enabled = !isCreating,
                                            onClick = {
                                                val newSet = if (isSelected) {
                                                    selectedDepartmentCodes - code
                                                } else {
                                                    selectedDepartmentCodes + code
                                                }
                                                onSelectedDepartmentCodesChange(newSet)
                                            },
                                            label = { Text(dept.nameGroup ?: dept.department ?: code) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ============ 2: КЛАССЫ SAP ============
                    2 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (assetClassesUiState) {
                                is AssetViewModel.AssetClassesUiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                                is AssetViewModel.AssetClassesUiState.Error -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Ошибка загрузки классов: ${assetClassesUiState.message}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        OutlinedButton(
                                            onClick = { onRetry() },
                                            enabled = !isCreating
                                        ) {
                                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Повторить")
                                        }
                                    }
                                }
                                else -> {
                                    if (assetClasses.isEmpty()) {
                                        Text(
                                            "Классы SAP не найдены",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        val allClassCodes = assetClasses.map { it.materialClass }.toSet()

                                        SelectionButtonsRow(
                                            total = allClassCodes.size,
                                            selected = selectedMaterialClasses.size,
                                            enabled = !isCreating,
                                            onSelectAll = {
                                                onSelectedMaterialClassesChange(allClassCodes)
                                            },
                                            onClearAll = {
                                                onSelectedMaterialClassesChange(emptySet())
                                            }
                                        )

                                        Text(
                                            "Если выбрать и типы, и классы — сессии создадутся на их пересечение.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            assetClasses.forEach { cls ->
                                                val isSelected = selectedMaterialClasses.contains(cls.materialClass)
                                                FilterChip(
                                                    selected = isSelected,
                                                    enabled = !isCreating,
                                                    onClick = {
                                                        val newSet = if (isSelected) {
                                                            selectedMaterialClasses - cls.materialClass
                                                        } else {
                                                            selectedMaterialClasses + cls.materialClass
                                                        }
                                                        onSelectedMaterialClassesChange(newSet)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = cls.materialClassShort ?: cls.materialClassName,
                                                            softWrap = false
                                                        )
                                                    },
                                                    leadingIcon = if (isSelected) {
                                                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // ================= ДАТЫ И ВРЕМЯ =================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Период инвентаризации:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = getDisplayText(startDateTimeMillis, startTimePickerState),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isCreating,
                        label = { Text("Начало") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showStartDatePicker = true },
                                enabled = !isCreating
                            ) {
                                Icon(Icons.Default.CalendarToday, null, Modifier.size(20.dp))
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
                        enabled = !isCreating,
                        label = { Text("Окончание") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showEndDatePicker = true },
                                enabled = !isCreating
                            ) {
                                Icon(Icons.Default.CalendarToday, null, Modifier.size(20.dp))
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
                    if (!isDateRangeValid || !hasAnySelection || isCreating) return@Button
                    onCreateSession(
                        selectedAssetTypeIds.toList().takeIf { it.isNotEmpty() },
                        selectedDepartmentCodes.toList().takeIf { it.isNotEmpty() },
                        selectedMaterialClasses.toList().takeIf { it.isNotEmpty() },   // ← новое
                        formatToIso(startDateTimeMillis, startTimePickerState.hour, startTimePickerState.minute),
                        formatToIso(endDateTimeMillis, endTimePickerState.hour, endTimePickerState.minute)
                    )
                },
                enabled = isDateRangeValid && hasAnySelection && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Создание…")
                } else {
                    Text("Создать")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) { Text("Отмена") }
        }
    )

    // ================= ПИКЕРЫ ДАТ И ВРЕМЕНИ =================
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

/**
 * Строка с кнопкой "Выбрать всё" / "Снять всё" + счётчик выбранных.
 *
 * Показывает:
 *  - "Выбрать всё (N)" — если выбрано не всё
 *  - "Снять всё (N)"   — если выбрано всё
 *  - "Выбрано: X из N" — справа от кнопки, мелким шрифтом
 */
@Composable
private fun SelectionButtonsRow(
    total: Int,
    selected: Int,
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    val isAllSelected = total > 0 && selected == total

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = {
                if (isAllSelected) onClearAll() else onSelectAll()
            },
            enabled = enabled && total > 0,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = if (isAllSelected) Icons.Default.ClearAll else Icons.Default.SelectAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isAllSelected) "Снять всё ($total)" else "Выбрать всё ($total)",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Text(
            text = "Выбрано: $selected из $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            assetTypeName = "Департамент: RU10050099 | Тип: Компьютеры",
            assetTypeEnName = "computers",
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
            assetTypeName = "Департамент: RU10050020 | Тип: Без типа",
            assetTypeEnName = "without_type",
            status = "in_progress",
            statusRu = "В работе",
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
                    AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
                    AssetTypeDto(13, "Без типа", "without_type", null, "2026-07-06T07:18:41.873769", null)
                ),
                permissionDepartments = listOf(
                    PermissionDepartmentDto(
                        department = "RDC",
                        nameGroup = "Активы RDC",
                        departmentCode = "RU10050099",
                        read = true,
                        write = true
                    ),
                    PermissionDepartmentDto(
                        department = "MSG",
                        nameGroup = "Активы MSG",
                        departmentCode = "RU10050020",
                        read = true,
                        write = true
                    )
                ),
                uiState = AssetViewModel.InventorizationUiState.SessionsLoaded(mockSessions),
                showCreateDialog = false,
                assetClasses = emptyList(),
                assetClassesUiState = AssetViewModel.AssetClassesUiState.Idle,
                selectedMaterialClasses = emptySet(),
                onSelectedMaterialClassesChange = {},
                selectedAssetTypeIds = emptySet(),
                selectedDepartmentCodes = emptySet(),
                isCreating = false,
                onSessionClick = { _, _ -> },
                onShowCreateDialogChange = {},
                onSelectedAssetTypeIdsChange = {},
                onSelectedDepartmentCodesChange = {},
                onCreateSession = { _, _, _, _, _ -> },
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
                AssetTypeDto(0, "Без типа", "without_type", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(2, "MES оборудование", "mes", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(3, "Расходные материалы", "supplies", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(7, "Сетевое оборудование", "network", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(8, "Печатное оборудование", "printing", null, "2026-07-06T07:21:39.334371", null),
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
                    nameGroup = "Активы MSG",
                    departmentCode = "RU01050020",
                    read = true,
                    write = true
                ),
                PermissionDepartmentDto(
                    department = null,
                    nameGroup = "Активы (только чтение)",
                    departmentCode = "RU01000098",
                    read = true,
                    write = false
                )
            ),
            selectedAssetTypeIds = setOf(1, 2),
            selectedDepartmentCodes = setOf("RU01050099", "RU01050020"),
            isCreating = false,
            onDismiss = {},
            assetClasses = emptyList(),
            assetClassesUiState = AssetViewModel.AssetClassesUiState.Idle,
            selectedMaterialClasses = emptySet(),
            onSelectedMaterialClassesChange = {},
            onSelectedAssetTypeIdsChange = {},
            onSelectedDepartmentCodesChange = {},
            onCreateSession = { _, _, _, _, _ -> },
            onRetry = {}
        )
    }
}