package com.gps.warehouse.ui.assets_screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.*
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.EmployeeSearchDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.gps_screens.warehouse.formatDate
import com.gps.warehouse.utils.formatIsoToReadable

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsScreen(
    assetId: Int,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assetStatuses by viewModel.assetStatuses.collectAsState()
    val assetTypes by viewModel.assetTypes.collectAsState()

    var showEmployeeSearchDialog by remember { mutableStateOf<UserType?>(null) }
    val employees by viewModel.employees.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showNextServiceDatePicker by remember { mutableStateOf(false) }

    var userPendingRemoval by remember { mutableStateOf<Pair<UserType, AssetUserFullResponse>?>(null) }

    // Единое состояние для редактирования
    var editState by remember { mutableStateOf<AssetEditState?>(null) }

    // Загружаем данные при открытии
    LaunchedEffect(assetId) {
        firstLoadData(viewModel = viewModel, assetId = assetId)
    }

    // Инициализируем editState ТОЛЬКО когда asset + статусы + типы загружены
    LaunchedEffect(uiState, assetStatuses, assetTypes) {
        if (uiState is AssetViewModel.AssetUiState.AssetDetailsLoaded &&
            assetStatuses.isNotEmpty() &&
            assetTypes.isNotEmpty()) {

            (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                editState = AssetEditState.fromAsset(original)
            }

            // Отключаем редактирование после успешной загрузки
            if (isEditing) {
                isEditing = false
            }
        }
    }

    // Диалог истории
    if (showHistoryDialog) {
        AssetHistoryDialog(
            history = viewModel.assetHistory.collectAsState().value,
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Диалог поиска сотрудников
    showEmployeeSearchDialog?.let { userType ->
        EmployeeSearchDialog(
            userType = userType,  // Передаём тип
            onDismiss = { showEmployeeSearchDialog = null },
            onEmployeeSelected = { selectedType, employee ->  // Раскомментируем и исправляем
                editState?.let { state ->
                    val updatedState = state.addUser(type = selectedType, employee = employee)
                    editState = updatedState  // Прямое обновление
                }
                showEmployeeSearchDialog = null  // Закрываем диалог
            },
            onSearch = { employeeId, searchDepartment, page ->  // Добавляем page
                viewModel.loadEmployees(
                    page = page,
                    pageSize = 20,
                    employeeId = employeeId,
                    searchDepartment = searchDepartment
                )
            },
            paginatedEmployees = employees,  // Теперь PaginatedResponse
            isLoading = employees == null,
            currentPage = employees?.page ?: 1
        )
    }

    userPendingRemoval?.let { (userType, user) ->
        val roleText = when (userType) {
            UserType.USER -> "Пользователи"
            UserType.RESPONSIBLE -> "Ответственные"
            UserType.SERVING -> "Обслуживающий персонал"
        }

        AlertDialog(
            onDismissRequest = { userPendingRemoval = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Подтверждение удаления") },
            text = {
                Text("Вы уверены, что хотите удалить сотрудника\n\"${user.fullNameRu}\"\nиз списка \"${roleText}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        editState?.let { state ->
                            val updatedState = state.removeUser(type = userType, userGuid = user.guid)
                            editState = updatedState // Прямое обновление состояния
                        }
                        userPendingRemoval = null // Закрываем диалог
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { userPendingRemoval = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showNextServiceDatePicker) {
        val datePickerState = rememberDatePickerState(
            // Опционально: можно установить начальную дату из editState, если она есть
            initialSelectedDateMillis = editState?.nextService?.let {
                java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showNextServiceDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Преобразуем миллисекунды в формат yyyy-MM-dd для API
                        val isoDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()

                        editState?.let { state ->
//                            onEditStateChange(state.copy(nextService = isoDate))
                            editState = editState?.copy(nextService = isoDate)
                        }
                    }
                    showNextServiceDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNextServiceDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AssetDetailsContent(
        uiState = uiState,
        assetStatuses = assetStatuses,
        assetTypes = assetTypes,
        isEditing = isEditing,
        editState = editState,
        onEditStateChange = { newState -> editState = newState },
        onToggleEdit = { isEditing = !isEditing },
        onSave = {
            editState?.let { state ->
                (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                    viewModel.updateAsset(assetId, state.toUpdate(original))
                }
            }
        },
        onCancelEdit = {
            isEditing = false
            (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                editState = AssetEditState.fromAsset(original)
            }
        },
        onShowHistory = { showHistoryDialog = true },
        onBackClick = { navController.popBackStack() },
        onNavigateToNotifications = {assetId -> navController.navigate("asset_notifications/asset/$assetId")},
        onNavigateToParent = { parentId -> navController.navigate("asset_details/$parentId") },
        onRetryClick = { firstLoadData(viewModel = viewModel, assetId = assetId) },

        // onAddUser: открываем диалог поиска
        onAddUser = { userType ->
            showEmployeeSearchDialog = userType
            viewModel.loadEmployees(page = 1, pageSize = 20)
        },

        // onRemoveUser: сохраняем какого пользователя надо удалить
        onRemoveUser = { userType, user ->
            userPendingRemoval = Pair(userType, user) // Запоминаем пользователя вместо мгновенного удаления
        },
        onNextServiceClick = { showNextServiceDatePicker = true }
    )
}

fun firstLoadData(viewModel: AssetViewModel, assetId: Int) {
    viewModel.loadAssetDetails(assetId)
    viewModel.loadAssetStatuses()
    viewModel.loadAssetTypes()
    viewModel.loadAssetHistory(assetId)
}

// ==================== CONTENT: UI + Preview ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsContent(
    uiState: AssetViewModel.AssetUiState,
    assetStatuses: List<AssetStatusDto>,
    assetTypes: List<AssetTypeDto>,
    isEditing: Boolean,
    editState: AssetEditState?,
    onEditStateChange: (AssetEditState) -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onShowHistory: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToNotifications: (Int) -> Unit,
    onNavigateToParent: (Int) -> Unit,
    onRetryClick: () -> Unit,
    onAddUser: ((UserType) -> Unit)? = null,
    onRemoveUser: ((UserType, AssetUserFullResponse) -> Unit)? = null,
    onNextServiceClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is AssetViewModel.AssetUiState.AssetDetailsLoaded -> {
                val asset = uiState.asset
                Column(modifier = Modifier.fillMaxSize()) {
                    // ActionBar
                    MyCustomActionBar(
                        text = asset.name,
                        onBackClick = onBackClick,
                        actionButton = {
                            Row {
                                if (isEditing) {
                                    IconButton(onClick = onSave) {
                                        Icon(Icons.Default.Save, "Сохранить", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = onCancelEdit) {
                                        Icon(Icons.Default.Close, "Отмена", tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    if (asset.assetId != null) {
                                        IconButton(onClick = { onNavigateToNotifications(asset.assetId) }) {
                                            Icon(
                                                Icons.Default.NotificationsNone,
                                                "Уведомления",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = onShowHistory) {
                                        Icon(Icons.Default.History, "История", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = onToggleEdit) {
                                        Icon(Icons.Default.Edit, "Редактировать", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    )

                    // Контент
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Карточка статуса
                        item {
                            StatusCard(
                                asset = asset,
                                isEditing = isEditing,
                                assetStatuses = assetStatuses,
                                editState = editState,
                                onStatusChange = { newStatusId ->
                                    editState?.let { state ->
                                        onEditStateChange(state.copy(assetStatusId = newStatusId))
                                    }
                                }
                            )
                        }

                        // Основная информация
                        item {
                            if (isEditing) {
                                EditableInfoSection(
                                    asset = asset,
                                    assetTypes = assetTypes,
                                    editState = editState ?: AssetEditState.fromAsset(asset),
                                    onEditStateChange = onEditStateChange
                                )
                            } else {
                                ReadOnlyInfoSection(asset = asset, onNavigateToParent = onNavigateToParent)
                            }
                        }

                        // Локация
                        item { LocationCard(location = asset.location, isEditing = isEditing) }

                        // Сервисная информация
                        item {
                            ServiceCard(
                                asset = asset,
                                isEditing = isEditing,
                                editState = editState,
                                onEditStateChange = onEditStateChange,
                                onNextServiceClick = onNextServiceClick
                            )
                        }

                        // Пользователи
                        item {
                            UsersSection(
                                title = "Пользователи",
                                users = editState?.currentUsers ?: asset.users,
                                icon = Icons.Default.Person,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                isEditing = isEditing,
                                onAddUser = if (isEditing) { { onAddUser?.invoke(UserType.USER) } } else null,
                                onRemoveUser = onRemoveUser
                            )
                        }

                        // Ответственные
                        item {
                            UsersSection(
                                title = "Ответственные",
                                users = editState?.currentResponsibleUsers ?: asset.responsibleUsers,
                                icon = Icons.Default.VerifiedUser,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                isEditing = isEditing,
                                onAddUser = if (isEditing) { { onAddUser?.invoke(UserType.RESPONSIBLE) } } else null,
                                onRemoveUser = onRemoveUser
                            )
                        }

                        // Обслуживающий персонал
                        item {
                            UsersSection(
                                title = "Обслуживающий персонал",
                                users = editState?.currentServingUsers ?: asset.servingUsers,
                                icon = Icons.Default.Build,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                isEditing = isEditing,
                                onAddUser = if (isEditing) { { onAddUser?.invoke(UserType.SERVING) } } else null,
                                onRemoveUser = onRemoveUser
                            )
                        }

                        // Мета-информация
                        item { MetaInfoCard(asset = asset) }
                    }
                }
            }

            is AssetViewModel.AssetUiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetryClick,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {}
        }
    }
}

// ==================== КАРТОЧКА СТАТУСА ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCard(
    asset: AssetResponseDto,
    isEditing: Boolean,
    assetStatuses: List<AssetStatusDto>,
    editState: AssetEditState? = null,
    onStatusChange: (Int?) -> Unit = {}
) {
    val currentStatusId = if (isEditing) {
        editState?.assetStatusId ?: asset.assetStatusId
    } else {
        asset.assetStatusId
    }

    val statusText = assetStatuses.find { it.id == currentStatusId }?.status
        ?: asset.assetStatus
        ?: "Не указан"

    val statusColor = when (statusText.lowercase()) {
        "приемка", "отремонтирован", "на складе", "в работе" -> Color(0, 150, 0, 170)
        "удален", "списан" -> Color(220, 0, 0, 170)
        "на обслуживании", "ожидает зч", "требует проверки", "в ремонте" -> Color(255, 193, 7, 170)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = statusColor, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (currentStatusId == 10) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Статус", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isEditing) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = statusText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Выберите статус") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            assetStatuses.forEach { statusDto ->
                                DropdownMenuItem(
                                    text = { Text(statusDto.status) },
                                    onClick = {
                                        onStatusChange(statusDto.id)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (statusDto.id == currentStatusId) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== ИНФОРМАЦИЯ ОБ АКТИВЕ ====================
@Composable
fun ReadOnlyInfoSection(asset: AssetResponseDto, onNavigateToParent: (Int) -> Unit) {
    InfoSectionCard(icon = Icons.Default.Info, title = "Основная информация") {
        InfoRow(label = "Название", value = asset.name)
        InfoRow(label = "Инв. номер", value = asset.inventoryId, copyable = true)
        InfoRow(label = "Серийный номер", value = asset.serialNumber, copyable = true)
        InfoRow(label = "Тип", value = asset.assetTypeName)
        InfoRow(label = "Модель", value = asset.modelName)
        InfoRow(label = "Количество", value = asset.quantity?.toString())
        InfoRow(label = "Производитель", value = asset.manufacturerName)
        InfoRow(label = "Поставщик", value = asset.vendorName)
        InfoRow(label = "ОС", value = asset.osName)

        asset.parentName?.let { parentName ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { asset.parentId?.let(onNavigateToParent) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Родительский актив", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(parentName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        asset.comment?.let { comment ->
            Spacer(modifier = Modifier.height(8.dp))
            Text("Комментарий", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(comment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableInfoSection(
    asset: AssetResponseDto,
    assetTypes: List<AssetTypeDto>,
    editState: AssetEditState,
    onEditStateChange: (AssetEditState) -> Unit
) {
    InfoSectionCard(icon = Icons.Default.Edit, title = "Редактирование") {
        OutlinedTextField(
            value = editState.name ?: "",
            onValueChange = { onEditStateChange(editState.copy(name = it)) },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = editState.inventoryId ?: "",
            onValueChange = { onEditStateChange(editState.copy(inventoryId = it)) },
            label = { Text("Инв. номер") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = editState.serialNumber ?: "",
            onValueChange = { onEditStateChange(editState.copy(serialNumber = it)) },
            label = { Text("Серийный номер") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Тип актива (dropdown)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = assetTypes.find { it.assetTypeId == editState.assetTypeId }?.name ?: "Выберите тип",
                onValueChange = {},
                readOnly = true,
                label = { Text("Тип актива") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                assetTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onEditStateChange(editState.copy(assetTypeId = type.assetTypeId))
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = editState.quantity?.toString() ?: "",
            onValueChange = { onEditStateChange(editState.copy(quantity = it.toIntOrNull())) },
            label = { Text("Количество") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = editState.comment ?: "",
            onValueChange = { onEditStateChange(editState.copy(comment = it)) },
            label = { Text("Комментарий") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
    }
}

@Composable
fun InfoSectionCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?, copyable: Boolean = false) {
    if (value.isNullOrEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (copyable) Modifier.clickable { /* Copy to clipboard */ } else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ==================== ЛОКАЦИЯ ====================
@Composable
fun LocationCard(location: AssetLocationResponse?, isEditing: Boolean) {
    if (location == null) return

    InfoSectionCard(icon = Icons.Default.LocationOn, title = "Локация") {
        if (!isEditing) {
            InfoRow(label = "Цех", value = location.workshopName)
            InfoRow(label = "Место", value = location.place)
            InfoRow(label = "Этаж", value = location.level?.toString())
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Редактирование локации доступно на карте", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { /* TODO: открыть карту */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Открыть карту")
            }
        }
    }
}

// ==================== СЕРВИСНАЯ ИНФОРМАЦИЯ ====================
@Composable
fun ServiceCard(
    asset: AssetResponseDto,
    isEditing: Boolean,
    editState: AssetEditState? = null,
    onEditStateChange: (AssetEditState) -> Unit = {},
    onNextServiceClick: (() -> Unit)? = null
) {
    InfoSectionCard(icon = Icons.Default.MiscellaneousServices, title = "Сервис") {
        if (!isEditing) {
            InfoRow(label = "Еженедельная проверка", value = if ((editState?.everyWeekCheck ?: asset.everyWeekCheck) == true) "Да" else "Нет")
            InfoRow(label = "След. обслуживание", value = asset.nextService?.formatIsoToReadable())
            InfoRow(label = "Период (дни)", value = (editState?.servicePeriod ?: asset.servicePeriod)?.toString())
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Еженедельная проверка", style = MaterialTheme.typography.labelMedium)
                val currentCheck = editState?.everyWeekCheck ?: asset.everyWeekCheck ?: false
                Switch(
                    checked = currentCheck,
                    onCheckedChange = { newValue ->
                        editState?.let { state ->
                            onEditStateChange(state.copy(everyWeekCheck = newValue))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // <-- ПОЛЕ ДАТЫ С DATE PICKER
            OutlinedTextField(
                value = editState?.nextService ?: asset.nextService ?: "",
                onValueChange = { }, // Оставляем пустым, так как поле readOnly
                label = { Text("След. обслуживание") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNextServiceClick?.invoke() }, // Клик по всему полю открывает календарь
                readOnly = true, // Запрещаем ручной ввод
                placeholder = { Text("ДД.ММ.ГГГГ") },
                trailingIcon = {
                    IconButton(onClick = { onNextServiceClick?.invoke() }) {
                        Icon(Icons.Default.CalendarToday, "Выбрать дату")
                    }
                },
                singleLine = true,
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedContainerColor = MaterialTheme.colorScheme.surface,
//                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
//                )
            )


            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = (editState?.servicePeriod ?: asset.servicePeriod ?: 0).toString(),
                onValueChange = { input ->
                    val current = editState?.servicePeriod ?: asset.servicePeriod ?: 0
                    val period = if (current == 0 && input.isNotEmpty() && input != "0") {
                        input.toIntOrNull() ?: 0
                    } else {
                        input.toIntOrNull() ?: 0
                    }
                    editState?.let { state ->
                        onEditStateChange(state.copy(servicePeriod = period))
                    }
                },
                label = { Text("Период (дни)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().widthIn(min = 60.dp),
                singleLine = true
            )
        }
    }
}

// ==================== ПОЛЬЗОВАТЕЛИ ====================
@Composable
fun UsersSection(
    title: String,
    users: List<AssetUserFullResponse>?,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    isEditing: Boolean = false,
    onAddUser: ((UserType) -> Unit)? = null,
    onRemoveUser: ((UserType, AssetUserFullResponse) -> Unit)? = null
) {
    if (users.isNullOrEmpty() && !isEditing) return

    val userType = when (title) {
        "Пользователи" -> UserType.USER
        "Ответственные" -> UserType.RESPONSIBLE
        "Обслуживающий персонал" -> UserType.SERVING
        else -> UserType.USER
    }

    InfoSectionCard(icon = icon, title = title) {
        users?.forEach { user ->
            ExpandableUserCard(
                user = user,
                color = color,
                icon = icon,
                isEditing = isEditing,
//                onRemove = if (isEditing) { { onRemoveUser?.invoke(userType, user.guid) } } else null
                onRemoveUser = if (isEditing && onRemoveUser != null) { { onRemoveUser(userType, user) } } else null

            )
        }

        if (onAddUser != null && isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onAddUser(userType) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить")
            }
        }
    }
}

// ==================== КАРТОЧКА ПОЛЬЗОВАТЕЛЯ ====================
@Composable
private fun ExpandableUserCard(
    user: AssetUserFullResponse,
    color: Color,
    icon: ImageVector,
    isEditing: Boolean,
//    onRemoveUser: (() -> Unit)? = null
    onRemoveUser: ((AssetUserFullResponse) -> Unit)? = null
) {
    var expanded by rememberSaveable(user.guid) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize(animationSpec = spring())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullNameRu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    user.position?.name?.let { position ->
                        Text(
                            text = position,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onRemoveUser != null && isEditing) {
                    IconButton(onClick = { onRemoveUser(user) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRowSmall(label = "Таб. номер", value = user.employeeId)
                    user.phone?.let { phone -> InfoRowSmall(label = "Телефон", value = phone) }
                    user.email?.let { email -> InfoRowSmall(label = "Email", value = email) }
                    user.department?.shortName?.let { dept -> InfoRowSmall(label = "Департамент", value = dept) }
                    user.division?.shortName?.let { division -> InfoRowSmall(label = "Отдел", value = division) }
                    user.group?.shortName?.let { group -> InfoRowSmall(label = "Группа", value = group) }

                    if (user.startDate != null || user.endDate != null) {
                        InfoRowSmall(
                            label = "Период владения",
                            value = "${user.startDate ?: "–"} – ${user.endDate ?: "∞"}"
                        )
                    }

                    user.assignmentType?.let { type ->
                        InfoRowSmall(
                            label = "Тип",
                            value = when (type) {
                                "user" -> "Пользователь"
                                "responsible" -> "Ответственный"
                                "serving" -> "Обслуживающий"
                                else -> type
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ ====================
@Composable
private fun InfoRowSmall(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$label:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ==================== МЕТА-ИНФОРМАЦИЯ ====================
@Composable
fun MetaInfoCard(asset: AssetResponseDto) {
    InfoSectionCard(icon = Icons.Default.Info, title = "Мета-информация") {
        InfoRow(label = "Создан", value = asset.createdAt.formatIsoToReadable())
        InfoRow(label = "Обновлён", value = asset.updatedAt?.formatIsoToReadable())
        InfoRow(label = "Создал", value = asset.createdBy)
        InfoRow(label = "Обновил", value = asset.updatedBy)
        InfoRow(label = "Текущий пользователь", value = asset.currentUserFullName)
    }
}

// ==================== ДИАЛОГ ИСТОРИИ ====================
@Composable
fun AssetHistoryDialog(history: List<AssetHistoryDto>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("История изменений") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entry ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(entry.fieldName ?: "Поле", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(entry.changedAt.formatIsoToReadable() ?: "", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Кем: ${entry.changerFullNameRu}", style = MaterialTheme.typography.bodySmall)
                            if (entry.oldValue != null || entry.newValue != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text("Было: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    Text(entry.oldValue ?: "–", style = MaterialTheme.typography.labelSmall)
                                }
                                Row {
                                    Text("Стало: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(entry.newValue ?: "–", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } }
    )
}

// ==================== PREVIEWS ====================
@Preview(showBackground = true, showSystemUi = true, name = "Детали актива", device = "spec:width=380dp,height=2250dp")
@Composable
private fun AssetDetailsPreview_ViewMode() {
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                isEditing = false,
                editState = AssetEditState.fromAsset(getSampleAsset()),
                onEditStateChange = {},
                onToggleEdit = {},
                onSave = {},
                onCancelEdit = {},
                onShowHistory = {},
                onBackClick = {},
                onNavigateToNotifications = {},
                onNavigateToParent = {},
                onRetryClick = {},
                onAddUser = {},
                onRemoveUser = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Детали актива (Редактирование)", device = "spec:width=380dp,height=2250dp")
@Composable
private fun AssetDetailsPreview_EditMode() {
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                isEditing = true,
                editState = AssetEditState.fromAsset(getSampleAsset()),
                onEditStateChange = {},
                onToggleEdit = {},
                onSave = {},
                onCancelEdit = {},
                onShowHistory = {},
                onBackClick = {},
                onNavigateToNotifications = {},
                onNavigateToParent = {},
                onRetryClick = {},
                onAddUser = {},
                onRemoveUser = { _, _ -> }
            )
        }
    }
}

@Preview
@Composable
fun AssetHistoryDialogPreview() {
    val history = AssetHistoryDto(
        id = 12,
        assetId = 52,
        actionType = "update",
        fieldName = "asset_status_id",
        oldValue = "9",
        newValue = "10",
        changedBy = "0000015370",
        changedAt = "2026-09-03T06:26:16.840853Z",
        comment = "comment",
        sessionId = "1a79db60-9f43-45f4-9d8d-e399ba7e0f51",
        changerFullNameRu = "Малышев Тимур Максимович",
        changerFullNameEn = "Malyshev Timur Maksimovich",
    )

    MaterialTheme {
        Surface {
            AssetHistoryDialog(history = listOf(history), onDismiss = {})
        }
    }
}

fun getSampleAsset(): AssetResponseDto {
    return AssetResponseDto(
        assetId = 48,
        materialId = null,
        name = "Актив 2",
        inventoryId = "INV_NUMBER_48",
        serialNumber = "SER_NUMBER_48",
        assetStatus = "В работе",
        assetStatusId = 10,
        comment = "Описание123123",
        dateIssue = "2026-08-19",
        datePurchasing = null,
        modelId = null,
        modelName = "модель 3",
        assetTypeId = 10,
        parentId = null,
        locationId = null,
        quantity = 120,
        preparedBy = null,
        checkedBy = null,
        parentName = "чего то там",
        manufacturerName = "китай",
        vendorName = "Z",
        osName = "окнО",
        everyWeekCheck = false,
        nextService = "2026-09-08",
        servicePeriod = 5,
        createdBy = "0000012657",
        updatedBy = "0000015370",
        createdAt = "2026-07-14T19:23:04.784110",
        updatedAt = "2026-09-03T09:26:16.840853",
        assetTypeName = "Оборудование MU",
        location = AssetLocationResponse(workshopId = 6, workshopName = "Логистика", place = "mesto213111111111111", level = 4, x = 237, y = 415),
        users = listOf(
            AssetUserFullResponse(
                guid = "974f470d-a7cd-11ef-a3b2-000c290ca5c4",
                employeeId = "0000010680",
                birthDate = "1994-12-30",
                employmentDate = "2024-11-21",
                dismissalDate = null,
                phone = "+79805882104",
                email = "Andrey.Malykh@hmmr.ru",
                comment = null,
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "80911547-78ee-11f0-a3c1-000c290ca5c4",
                createdAt = "2026-07-08T14:17:34.650680",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Малых Андрей Владимирович",
                fullNameEn = "Malykh Andrey Vladimirovich",
                society = null,
                department = null,
                division = null,
                group = null,
                position = PositionResponse(name = "Младший инженер по внедрению информационных систем 2 категории", nameEn = null),
                startDate = "2026-08-24",
                endDate = null,
                assignmentType = "user"
            ),
            AssetUserFullResponse(
                guid = "14ba77ab-2d91-11f1-a3cb-000c290ca5c4",
                employeeId = "0000015370",
                birthDate = "2002-09-06",
                employmentDate = "2026-04-01",
                dismissalDate = null,
                phone = "+79190809746",
                email = "Timur.Malyshev@hmmr.ru",
                comment = "Проверка",
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "6334328f-f69a-11f0-a3c7-000c290ca5c4",
                createdAt = "2026-07-08T14:17:44.545594",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Малышев Тимур Максимович",
                fullNameEn = "Malyshev Timur Maksimovich",
                society = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "SOCIETY", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                department = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "DEPARTMENT", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                division = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "DIVISION", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                group = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "GROUP", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                position = PositionResponse(name = "Разработчик Программного Обеспечения", nameEn = null),
                startDate = "2026-08-26",
                endDate = null,
                assignmentType = "user"
            )
        ),
        responsibleUsers = listOf(
            AssetUserFullResponse(
                guid = "14ba77ab-2d91-11f1-a3cb-000c290ca5c4",
                employeeId = "0000015370",
                birthDate = "2002-09-06",
                employmentDate = "2026-04-01",
                dismissalDate = null,
                phone = "+79190809746",
                email = "Timur.Malyshev@hmmr.ru",
                comment = "Проверка",
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "6334328f-f69a-11f0-a3c7-000c290ca5c4",
                createdAt = "2026-07-08T14:17:44.545594",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Малышев Тимур Максимович",
                fullNameEn = "Malyshev Timur Maksimovich",
                society = null,
                department = null,
                division = null,
                group = null,
                position = PositionResponse(name = "Разработчик Программного Обеспечения", nameEn = null),
                startDate = "2026-08-26",
                endDate = null,
                assignmentType = "responsible"
            )
        ),
        servingUsers = listOf(
            AssetUserFullResponse(
                guid = "c0ed588f-1c4a-11f1-a3ca-000c290ca5c4",
                employeeId = "0000014942",
                birthDate = "2003-04-19",
                employmentDate = "2026-03-10",
                dismissalDate = null,
                phone = "+79805882044",
                email = "Oleg.Feshchenko@hmmr.ru",
                comment = null,
                positionGuid = "f508e032-1c57-11f1-a3ca-000c290ca5c4",
                departmentGuid = "6334328f-f69a-11f0-a3c7-000c290ca5c4",
                createdAt = "2026-07-08T14:17:43.360340",
                updatedAt = "2026-09-03T02:00:11.229864",
                fullNameRu = "Фещенко Олег Игоревич",
                fullNameEn = "Feshchenko Oleg Igorevich",
                society = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "SOCIETY", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                department = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "DEPARTMENT", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                division = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "DIVISION", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                group = WorkplaceResponse(guid = "295dd391-1099-11e7-80ca-6c0b843fb628", name = "ХАВЕЙЛ", nameEn = "", shortName = "GROUP", creationDate = "2019-08-01", closureDate = null, parentGuid = "00000000-0000-0000-0000-000000000000"),
                position = PositionResponse(name = "Младший инженер по внедрению информационных систем 2 категории", nameEn = null),
                startDate = "2026-09-02",
                endDate = null,
                assignmentType = "serving"
            )
        ),
        currentUser = "0000012657",
        currentUserFullName = "Евсиков Константин Александрович",
        parent = null
    )
}