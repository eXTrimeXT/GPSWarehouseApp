package com.gps.warehouse.ui.assets_screens.assets

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.*
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.assets_screens.mobile.InfoRow
import com.gps.warehouse.ui.components.EmployeeSearchDialog
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.formatIsoToReadable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsScreen(
    assetId: Int? = null,
    materialId: String? = null,
    navController: NavHostController,
    assetViewModel: AssetViewModel
) {
    val uiState by assetViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val assetStatuses by assetViewModel.assetStatuses.collectAsState()
    val assetTypes by assetViewModel.assetTypes.collectAsState()
    val assetHistory by assetViewModel.assetHistory.collectAsState()

    var showEmployeeSearchDialog by remember { mutableStateOf<UserType?>(null) }
    val employees by assetViewModel.employees.collectAsState()
    val employeeMe by assetViewModel.employeeMe.collectAsState()

    val assetTransfers by assetViewModel.assetTransfers.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showNextServiceDatePicker by remember { mutableStateOf(false) }
    var userPendingRemoval by remember { mutableStateOf<Pair<UserType, AssetUserFullResponse>?>(null) }

    var editState by remember { mutableStateOf<AssetEditState?>(null) }

    // === Передача актива ===
    var showTransferDialog by remember { mutableStateOf(false) }
    var showEmployeeSearchForTransfer by remember { mutableStateOf(false) }
    var selectedEmployeeForTransfer by remember { mutableStateOf<EmployeeShortResponse?>(null) }
    var isTransferring by remember { mutableStateOf(false) }

    // Новый state для диалога подтверждения
    var transferToCancel by remember { mutableStateOf<Int?>(null) }
    var isCancellingTransfer by remember { mutableStateOf(false) }

    // === Вкладки ===
    val pagerState = rememberPagerState(pageCount = { 3 })
    var selectedTab by remember { mutableIntStateOf(0) }

    // Синхронизация табов и пейджера
    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab)
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    // Загружаем данные при открытии
    LaunchedEffect(assetId, materialId) {
        firstLoadData(viewModel = assetViewModel, assetId = assetId, materialId = materialId)
    }

    // Инициализируем editState когда всё загружено
    LaunchedEffect(uiState, assetStatuses, assetTypes) {
        if (uiState is AssetViewModel.AssetUiState.AssetDetailsLoaded &&
            assetStatuses.isNotEmpty() && assetTypes.isNotEmpty()) {

            (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                editState = AssetEditState.fromAsset(original)
            }
            if (isEditing) isEditing = false
        }
    }

    // Загрузка списка передач
    LaunchedEffect(uiState) {
        val asset = (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset
        if (asset?.assetId != null) {
            assetViewModel.loadAssetTransfers(asset.assetId)
        }
    }

    // === Диалоги ===
    showEmployeeSearchDialog?.let { userType ->
        EmployeeSearchDialog(
            userType = userType,
            onDismiss = { showEmployeeSearchDialog = null },
            onEmployeeSelected = { selectedType, employee ->
                editState?.let { state ->
                    editState = state.addUser(type = selectedType, employee = employee)
                }
                showEmployeeSearchDialog = null
            },
            onSearch = { employeeId, searchDepartment, page ->
                assetViewModel.loadEmployees(page = page, pageSize = 20,
                    employeeId = employeeId, searchDepartment = searchDepartment)
            },
            paginatedEmployees = employees,
            isLoading = employees == null,
            currentPage = employees?.page ?: 1
        )
    }

    userPendingRemoval?.let { (userType, user) ->
        val roleText = when (userType) {
            UserType.USER -> "Владелец"
            UserType.SERVING -> "Обслуживающий персонал"
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { userPendingRemoval = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Подтверждение удаления") },
            text = { Text("Вы уверены, что хотите удалить сотрудника\n\"${user.fullNameRu}\"\nиз списка \"${roleText}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        editState?.let { state ->
                            editState = state.removeUser(type = userType, userGuid = user.guid)
                        }
                        userPendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { userPendingRemoval = null }) { Text("Отмена") } }
        )
    }

    if (showNextServiceDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editState?.nextService?.let {
                LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showNextServiceDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val isoDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        editState?.let { editState = it.copy(nextService = isoDate) }
                    }
                    showNextServiceDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showNextServiceDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEmployeeSearchForTransfer) {
        EmployeeSearchDialog(
            userType = UserType.USER,
            onDismiss = { showEmployeeSearchForTransfer = false },
            onEmployeeSelected = { _, employee ->
                selectedEmployeeForTransfer = employee
                showEmployeeSearchForTransfer = false
                showTransferDialog = true
            },
            onSearch = { employeeId, searchDepartment, page ->
                assetViewModel.loadEmployees(page = page, pageSize = 20,
                    employeeId = employeeId, searchDepartment = searchDepartment)
            },
            paginatedEmployees = employees,
            isLoading = employees == null,
            currentPage = employees?.page ?: 1
        )
    }

    if (showTransferDialog && selectedEmployeeForTransfer != null) {
        val currentAsset = (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset
        if (currentAsset != null) {
            AssetTransferDialog(
                asset = currentAsset,
                targetEmployee = selectedEmployeeForTransfer!!,
                isLoading = isTransferring,
                onDismiss = {
                    if (!isTransferring) {
                        showTransferDialog = false
                        selectedEmployeeForTransfer = null
                    }
                },
                onConfirm = { assignmentType, comment ->
                    isTransferring = true
                    assetViewModel.requestAssetTransfer(
                        assetId = currentAsset.assetId,
                        materialId = currentAsset.materialId,
                        targetEmployeeId = selectedEmployeeForTransfer!!.employeeId,
                        assignmentType = assignmentType,
                        comment = comment,
                        onSuccess = { response ->
                            isTransferring = false
                            showTransferDialog = false
                            selectedEmployeeForTransfer = null
                            Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                        },
                        onError = { error ->
                            isTransferring = false
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }

    // Диалог подтверждения отмены
    transferToCancel?.let { transferId ->
        AlertDialog(
            onDismissRequest = {
                if (!isCancellingTransfer) transferToCancel = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Отменить передачу?") },
            text = { Text("Запрос на передачу актива будет отменён. Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        isCancellingTransfer = true
                        assetViewModel.cancelTransfer(
                            transferId = transferId,
                            onSuccess = { response ->
                                isCancellingTransfer = false
                                transferToCancel = null
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                // Перезагружаем список передач и статус
                                (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)
                                    ?.asset?.assetId?.let { assetId ->
                                        assetViewModel.loadAssetTransfers(assetId)
                                        assetViewModel.checkTransferExists(assetId)
                                    }
                            },
                            onError = { error ->
                                isCancellingTransfer = false
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isCancellingTransfer,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isCancellingTransfer) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Отменить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { transferToCancel = null },
                    enabled = !isCancellingTransfer
                ) { Text("Назад") }
            }
        )
    }

    // === Контент ===
    AssetDetailsContent(
        uiState = uiState,
        assetStatuses = assetStatuses,
        assetTypes = assetTypes,
        assetHistory = assetHistory,
        isEditing = isEditing,
        editState = editState,
        selectedTab = selectedTab,
        pagerState = pagerState,
        onTabSelected = { selectedTab = it },
        onEditStateChange = { newState -> editState = newState },
        onToggleEdit = { isEditing = !isEditing },
        onSave = {
            editState?.let { state ->
                (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                    val idToUpdate = original.assetId ?: assetId
                    if (idToUpdate != null) {
                        assetViewModel.updateAsset(idToUpdate, state.toUpdate(original))
                    } else {
                        Log.e("AssetDetailsScreen", "Невозможно обновить: assetId отсутствует")
                    }
                }
            }
        },
        onCancelEdit = {
            isEditing = false
            (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                editState = AssetEditState.fromAsset(original)
            }
        },
        onBackClick = { navController.popBackStack() },
        onNavigateToNotifications = { assetId -> navController.navigate("asset_notifications/asset/$assetId") },
        onNavigateToParent = { parentId -> navController.navigate("asset_details/$parentId") },
        onRetryClick = { firstLoadData(viewModel = assetViewModel, assetId = assetId, materialId = materialId) },
        onAddUser = { userType ->
            showEmployeeSearchDialog = userType
            assetViewModel.loadEmployees(page = 1, pageSize = 20)
        },
        onRemoveUser = { userType, user ->
            userPendingRemoval = Pair(userType, user)
        },
        onNextServiceClick = { showNextServiceDatePicker = true },
        onTransferClick = {
            showEmployeeSearchForTransfer = true
            assetViewModel.loadEmployees(page = 1, pageSize = 20)
        },
        assetTransfers = assetTransfers,
        currentEmployeeId = employeeMe?.employeeId,   // ← новое
        onCancelTransfer = { transferId ->            // ← новое
            // Открываем диалог подтверждения через state
            transferToCancel = transferId
        }
    )
}

fun firstLoadData(viewModel: AssetViewModel, assetId: Int?, materialId: String?) {
    viewModel.loadAssetDetails(assetId = assetId, materialId = materialId)
    viewModel.loadAssetStatuses()
    viewModel.loadAssetTypes()
    viewModel.getEmployeeMe()
}

// ==================== CONTENT: UI + TABS ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsContent(
    uiState: AssetViewModel.AssetUiState,
    assetStatuses: List<AssetStatusDto>,
    assetTypes: List<AssetTypeDto>,
    assetHistory: List<AssetHistoryDto>,
    isEditing: Boolean,
    editState: AssetEditState?,
    selectedTab: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onTabSelected: (Int) -> Unit,
    onEditStateChange: (AssetEditState) -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToNotifications: (Int) -> Unit,
    onNavigateToParent: (Int) -> Unit,
    onRetryClick: () -> Unit,
    onAddUser: ((UserType) -> Unit)? = null,
    onRemoveUser: ((UserType, AssetUserFullResponse) -> Unit)? = null,
    onNextServiceClick: (() -> Unit)? = null,
    onTransferClick: () -> Unit,
    assetTransfers: List<AssetTransferDto>,
    currentEmployeeId: String?,
    onCancelTransfer: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                MyCustomActionBar(
                    text = "Загрузка актива...",
                    onBackClick = onBackClick,
                )
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetryClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is AssetViewModel.AssetUiState.AssetDetailsLoaded -> {
                val asset = uiState.asset
                Column(modifier = Modifier.fillMaxSize()) {
                    // === ActionBar (только Назад + Редактировать / Save+Cancel) ===
                    MyCustomActionBar(
                        text = asset.name,
                        onBackClick = onBackClick,
                        actionButton = {
                            Row {
                                if (isEditing) {
                                    IconButton(onClick = onSave) {
                                        Icon(Icons.Default.Save, "Сохранить",
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = onCancelEdit) {
                                        Icon(Icons.Default.Close, "Отмена",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    IconButton(onClick = onToggleEdit) {
                                        Icon(Icons.Default.Edit, "Редактировать",
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    )
                    // === Вкладки ===
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { onTabSelected(0) },
                            text = { Text("Основное") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { onTabSelected(1) },
                            text = { Text("Люди") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { onTabSelected(2) },
                            text = { Text("История") }
                        )
                    }

                    // === Контент вкладок (свайп) ===
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> MainInfoTab(
                                asset = asset,
                                assetTypes = assetTypes,
                                assetStatuses = assetStatuses,
                                isEditing = isEditing,
                                editState = editState,
                                onEditStateChange = onEditStateChange,
                                onNavigateToParent = onNavigateToParent,
                                onNextServiceClick = onNextServiceClick
                            )
                            1 -> UsersTab(
                                asset = asset,
                                isEditing = isEditing,
                                editState = editState,
                                onAddUser = onAddUser,
                                onRemoveUser = onRemoveUser,
                                onTransferClick = onTransferClick,
                                onCancelTransfer = onCancelTransfer,
                                transfers = assetTransfers,
                                currentEmployeeId = currentEmployeeId
                            )
                            2 -> HistoryTab(
                                asset = asset,
                                history = assetHistory,
                                onNavigateToNotifications = onNavigateToNotifications
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

// ==================== FIXED HEADER CARD ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetHeaderCard(
    asset: AssetResponseDto,
    isEditing: Boolean,
    assetStatuses: List<AssetStatusDto>,
    editState: AssetEditState?,
    onStatusChange: (Int?) -> Unit
) {
    val currentStatusId = if (isEditing) editState?.assetStatusId ?: asset.assetStatusId
    else asset.assetStatusId

    val statusText = assetStatuses.find { it.id == currentStatusId }?.status
        ?: asset.assetStatus ?: "Не указан"

    val statusColor = when (statusText.lowercase()) {
        "приемка", "отремонтирован", "на складе", "в работе", "в эксплуатации" -> Color(0, 150, 0, 170)
        "удален", "списан" -> Color(220, 0, 0, 170)
        "на обслуживании", "ожидает зч", "требует проверки", "в ремонте" -> Color(255, 193, 7, 170)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка статуса
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (currentStatusId == 10) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isEditing) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded,
                            onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = statusText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Статус") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            ExposedDropdownMenu(expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                assetStatuses.forEach { statusDto ->
                                    DropdownMenuItem(
                                        text = { Text(statusDto.status) },
                                        onClick = {
                                            onStatusChange(statusDto.id)
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            if (statusDto.id == currentStatusId) {
                                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Статус", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(statusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Краткие идентификаторы
//            if (!isEditing) {
//                Spacer(Modifier.height(12.dp))
//                HorizontalDivider(color = statusColor.copy(alpha = 0.3f))
//                Spacer(Modifier.height(8.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    CompactIdRow(
//                        icon = Icons.Outlined.QrCode,
//                        label = "Инв. №",
//                        value = asset.inventoryId ?: "—"
//                    )
//                }
//                Spacer(Modifier.height(8.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    if (!asset.serialNumber.isNullOrBlank()) {
//                        CompactIdRow(
//                            icon = Icons.Default.Code,
//                            label = "Серийный",
//                            value = asset.serialNumber
//                        )
//                    }
//                }
//
//            }
        }
    }
}

@Composable
private fun CompactIdRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
    ) {
        Icon(icon, null, Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text("$label: ", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false))
    }
}

// ==================== TAB 1: MAIN INFO ====================
@Composable
fun MainInfoTab(
    asset: AssetResponseDto,
    assetTypes: List<AssetTypeDto>,
    assetStatuses: List<AssetStatusDto>,
    isEditing: Boolean,
    editState: AssetEditState?,
    onEditStateChange: (AssetEditState) -> Unit,
    onNavigateToParent: (Int) -> Unit,
    onNextServiceClick: (() -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // === Фиксированный хедер со статусом ===
            AssetHeaderCard(
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
        item { LocationCard(location = asset.location, isEditing = isEditing) }
        item {
            ServiceCard(
                asset = asset,
                isEditing = isEditing,
                editState = editState,
                onEditStateChange = onEditStateChange,
                onNextServiceClick = onNextServiceClick
            )
        }
    }
}

// ==================== TAB 2: USERS ====================
//@Composable
//fun UsersTab(
//    asset: AssetResponseDto,
//    isEditing: Boolean,
//    editState: AssetEditState?,
//    onAddUser: ((UserType) -> Unit)?,
//    onRemoveUser: ((UserType, AssetUserFullResponse) -> Unit)?,
//    onTransferClick: () -> Unit,
//    transfers: List<AssetTransferDto>
//) {
//    LazyColumn(
//        modifier = Modifier.fillMaxSize(),
//        contentPadding = PaddingValues(16.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        item {
//            UsersSection(
//                title = "Владелец",
//                users = editState?.currentUsers ?: asset.users,
//                icon = Icons.Default.Person,
//                color = MaterialTheme.colorScheme.surfaceVariant,
//                isEditing = isEditing,
//                onAddUser = if (isEditing) { { onAddUser?.invoke(UserType.USER) } } else null,
//                onRemoveUser = onRemoveUser
//            )
//        }
//        item {
//            UsersSection(
//                title = "Обслуживающий персонал",
//                users = editState?.currentServingUsers ?: asset.servingUsers,
//                icon = Icons.Default.Build,
//                color = MaterialTheme.colorScheme.tertiaryContainer,
//                isEditing = isEditing,
//                onAddUser = if (isEditing) { { onAddUser?.invoke(UserType.SERVING) } } else null,
//                onRemoveUser = onRemoveUser
//            )
//        }
//
//        // === История передач (только в режиме просмотра) ===
//        if (!isEditing && transfers.isNotEmpty()) {
//            item {
//                AssetTransfersSection(transfers = transfers)
//            }
//        }
//
//        // Кнопка передачи (только в режиме просмотра)
//        if (!isEditing) {
//            item {
//                Spacer(Modifier.height(8.dp))
//                Button(
//                    onClick = onTransferClick,
//                    modifier = Modifier.fillMaxWidth().height(56.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
//                ) {
//                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(20.dp))
//                    Spacer(Modifier.width(8.dp))
//                    Text("Передать актив",
//                        style = MaterialTheme.typography.bodyLarge,
//                        fontWeight = FontWeight.SemiBold)
//                }
//                Spacer(Modifier.height(16.dp))
//            }
//        }
//    }
//}

@Composable
fun UsersTab(
    asset: AssetResponseDto,
    isEditing: Boolean,
    editState: AssetEditState?,
    onAddUser: ((UserType) -> Unit)?,
    onRemoveUser: ((UserType, AssetUserFullResponse) -> Unit)?,
    onTransferClick: () -> Unit,
    transfers: List<AssetTransferDto>,
    currentEmployeeId: String?,                       // ← новое
    onCancelTransfer: (Int) -> Unit                   // ← новое
) {
    // Определяем: есть ли текущий пользователь среди владельцев
    val currentUsers = editState?.currentUsers ?: asset.users
    val isCurrentUserOwner = currentEmployeeId != null &&
            currentUsers?.any { it.employeeId == currentEmployeeId } == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // === Кнопка «Передать актив» — только если мы владелец ===
        if (!isEditing && isCurrentUserOwner) {
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Передать актив",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // === Обслуживающий персонал ===
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

        // === Владелец ===
        item {
            UsersSection(
                title = "Владелец",
                users = currentUsers,
                icon = Icons.Default.Person,
                color = MaterialTheme.colorScheme.surfaceVariant,
                isEditing = isEditing,
                onAddUser = if (isEditing) { { onAddUser?.invoke(UserType.USER) } } else null,
                onRemoveUser = onRemoveUser
            )
        }

        // === История передач ===
        if (!isEditing && transfers.isNotEmpty()) {
            item {
                AssetTransfersSection(
                    transfers = transfers,
                    currentEmployeeId = currentEmployeeId,
                    onCancelTransfer = onCancelTransfer
                )
            }
        }
    }
}

// ==================== TAB 3: HISTORY ====================
@Composable
fun HistoryTab(
    asset: AssetResponseDto,
    history: List<AssetHistoryDto>,
    onNavigateToNotifications: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Уведомления (переход на отдельный экран)
        item {
            asset.assetId?.let { assetId ->
                OutlinedCard(
                    onClick = { onNavigateToNotifications(assetId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.NotificationsActive, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Уведомления по активу",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text("Все связанные уведомления и действия",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Заголовок истории
        item {
            Text(
                "История изменений",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.History, null,
                            Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("История изменений пока пуста",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(history) { entry ->
                HistoryItemCard(entry = entry)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(entry: AssetHistoryDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = entry.fieldName ?: "Поле",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    entry.changedAt.formatIsoToReadable() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Кем: ${entry.changerFullNameRu}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (entry.oldValue != null || entry.newValue != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Было
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Было", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                            Text(entry.oldValue ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                    // Стало
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Стало", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(entry.newValue ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoSectionCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
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
        modifier = Modifier.fillMaxWidth()
            .then(if (copyable) Modifier.clickable { } else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

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
                modifier = Modifier.fillMaxWidth()
                    .clickable { asset.parentId?.let(onNavigateToParent) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Родительский актив", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(parentName, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        asset.comment?.let { comment ->
            Spacer(Modifier.height(8.dp))
            Text("Комментарий", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(comment, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp))
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
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = editState.inventoryId ?: "",
            onValueChange = { onEditStateChange(editState.copy(inventoryId = it)) },
            label = { Text("Инв. номер") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = editState.serialNumber ?: "",
            onValueChange = { onEditStateChange(editState.copy(serialNumber = it)) },
            label = { Text("Серийный номер") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded,
            onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = assetTypes.find { it.assetTypeId == editState.assetTypeId }?.name ?: "Выберите тип",
                onValueChange = {},
                readOnly = true,
                label = { Text("Тип актива") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true
            )
            ExposedDropdownMenu(expanded = expanded,
                onDismissRequest = { expanded = false }) {
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
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = editState.quantity?.toString() ?: "",
            onValueChange = { onEditStateChange(editState.copy(quantity = it.toIntOrNull())) },
            label = { Text("Количество") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
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
fun LocationCard(location: AssetLocationResponse?, isEditing: Boolean) {
    if (location == null) return
    InfoSectionCard(icon = Icons.Default.LocationOn, title = "Локация") {
        if (!isEditing) {
            InfoRow(label = "Цех", value = location.workshopName)
            InfoRow(label = "Место", value = location.place)
            InfoRow(label = "Этаж", value = location.level?.toString())
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Редактирование локации доступно на карте",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Открыть карту")
            }
        }
    }
}

@Composable
fun ServiceCard(
    asset: AssetResponseDto,
    isEditing: Boolean,
    editState: AssetEditState?,
    onEditStateChange: (AssetEditState) -> Unit,
    onNextServiceClick: (() -> Unit)?
) {
    InfoSectionCard(icon = Icons.Default.MiscellaneousServices, title = "Сервис") {
        if (!isEditing) {
            InfoRow(label = "Еженедельная проверка",
                value = if ((editState?.everyWeekCheck ?: asset.everyWeekCheck) == true) "Да" else "Нет")
            InfoRow(label = "След. обслуживание",
                value = asset.nextService?.formatIsoToReadable())
            InfoRow(label = "Период (дни)",
                value = (editState?.servicePeriod ?: asset.servicePeriod)?.toString())
        } else {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Еженедельная проверка",
                    style = MaterialTheme.typography.labelMedium)
                val currentCheck = editState?.everyWeekCheck ?: asset.everyWeekCheck ?: false
                Switch(
                    checked = currentCheck,
                    onCheckedChange = { newValue ->
                        editState?.let { state ->
                            onEditStateChange(state.copy(everyWeekCheck = newValue))
                        }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = editState?.nextService ?: asset.nextService ?: "",
                onValueChange = { },
                label = { Text("След. обслуживание") },
                modifier = Modifier.fillMaxWidth().clickable { onNextServiceClick?.invoke() },
                readOnly = true,
                placeholder = { Text("ДД.ММ.ГГГГ") },
                trailingIcon = {
                    IconButton(onClick = { onNextServiceClick?.invoke() }) {
                        Icon(Icons.Default.CalendarToday, "Выбрать дату")
                    }
                },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = (editState?.servicePeriod ?: asset.servicePeriod ?: 0).toString(),
                onValueChange = { input ->
                    val current = editState?.servicePeriod ?: asset.servicePeriod ?: 0
                    val period = if (current == 0 && input.isNotEmpty() && input != "0") {
                        input.toIntOrNull() ?: 0
                    } else input.toIntOrNull() ?: 0
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
        "Владелец" -> UserType.USER
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
                onRemoveUser = if (isEditing && onRemoveUser != null) {
                    { onRemoveUser(userType, user) }
                } else null
            )
        }
        if (onAddUser != null && isEditing) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onAddUser(userType) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }
    }
}

@Composable
private fun ExpandableUserCard(
    user: AssetUserFullResponse,
    color: Color,
    icon: ImageVector,
    isEditing: Boolean,
    onRemoveUser: ((AssetUserFullResponse) -> Unit)? = null
) {
    var expanded by rememberSaveable(user.guid) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
                .animateContentSize(animationSpec = spring())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = color,
                    modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.fullNameRu,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    user.position?.name?.let { position ->
                        Text(position,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (onRemoveUser != null && isEditing) {
                    IconButton(onClick = { onRemoveUser(user) },
                        modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
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
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoRowSmall(label = "Таб. номер", value = user.employeeId)
                    user.department?.shortName?.let { dept ->
                        InfoRowSmall(label = "Департамент", value = dept)
                    }
                    user.division?.shortName?.let { division ->
                        InfoRowSmall(label = "Отдел", value = division)
                    }
                    user.group?.shortName?.let { group ->
                        InfoRowSmall(label = "Группа", value = group)
                    }
                    if (user.startDate != null || user.endDate != null) {
                        InfoRowSmall(
                            label = "Период владения",
                            value = "${user.startDate ?: "–"} – ${user.endDate ?: "∞"}"
                        )
                    }
                    user.assignmentType?.let { type ->
                        InfoRowSmall(label = "Тип",
                            value = when (type) {
                                "user" -> "Пользователь"
                                "serving" -> "Обслуживающий"
                                else -> type
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRowSmall(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "$label:", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AssetTransfersSection(
    transfers: List<AssetTransferDto>,
    currentEmployeeId: String?,
    onCancelTransfer: (Int) -> Unit
) {
    InfoSectionCard(
        icon = Icons.Default.SwapHoriz,
        title = "История передач"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transfers.forEach { transfer ->
                AssetTransferCard(
                    transfer = transfer,
                    currentEmployeeId = currentEmployeeId,
                    onCancelTransfer = onCancelTransfer
                )
            }
        }
    }
}

//@Composable
//private fun AssetTransferCard(transfer: AssetTransferDto) {
//    val statusInfo = transferStatusInfo(transfer.status)
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(10.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = statusInfo.containerColor.copy(alpha = 0.35f)
//        )
//    ) {
//        Column(modifier = Modifier.padding(12.dp)) {
//            // === Верхняя строка: дата + статус ===
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = transfer.createdAt.formatIsoToReadable() ?: "",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Surface(
//                    shape = RoundedCornerShape(6.dp),
//                    color = statusInfo.containerColor
//                ) {
//                    Text(
//                        text = statusInfo.label,
//                        style = MaterialTheme.typography.labelSmall,
//                        fontWeight = FontWeight.SemiBold,
//                        color = statusInfo.contentColor,
//                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            // === Кто кому ===
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                // Инициатор
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        "От кого",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Text(
//                        transfer.initiator.fullName ?: transfer.initiator.employeeId,
//                        style = MaterialTheme.typography.bodySmall,
//                        fontWeight = FontWeight.Medium,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//
//                Icon(
//                    Icons.Default.ArrowForward,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.size(18.dp)
//                )
//                Spacer(Modifier.width(8.dp))
//
//                // Получатель
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        "Кому",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Text(
//                        transfer.targetEmployee.fullName ?: transfer.targetEmployee.employeeId,
//                        style = MaterialTheme.typography.bodySmall,
//                        fontWeight = FontWeight.Medium,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(8.dp))
//
//            // === Роль ===
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    Icons.Default.Badge,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.size(14.dp)
//                )
//                Spacer(Modifier.width(4.dp))
//                Text(
//                    text = transfer.assignmentTypeRu ?: transfer.assignmentType,
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            // === Комментарий инициатора ===
//            transfer.initiatorComment?.takeIf { it.isNotBlank() }?.let { comment ->
//                Spacer(Modifier.height(6.dp))
//                Text(
//                    text = comment,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//
//            // === Дата ответа (если есть) ===
//            transfer.respondedAt?.let { respondedAt ->
//                Spacer(Modifier.height(6.dp))
//                Text(
//                    text = "Ответ: ${respondedAt.formatIsoToReadable() ?: respondedAt}",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//                )
//            }
//        }
//    }
//}

@Composable
private fun AssetTransferCard(
    transfer: AssetTransferDto,
    currentEmployeeId: String?,
    onCancelTransfer: (Int) -> Unit
) {
    var expanded by rememberSaveable(transfer.transferId) { mutableStateOf(false) }
    val statusInfo = transferStatusInfo(transfer.status)

    // Показываем кнопку отмены, если:
    // 1. Transfer в статусе PENDING
    // 2. Мы инициатор
    val canCancel = transfer.status.equals("PENDING", ignoreCase = true) &&
            currentEmployeeId != null &&
            transfer.initiator.employeeId == currentEmployeeId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(animationSpec = spring()),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusInfo.containerColor.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // === Верхняя строка: дата + статус ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transfer.createdAt.formatIsoToReadable() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusInfo.containerColor
                ) {
                    Text(
                        text = statusInfo.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusInfo.contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // === Кто кому ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "От кого",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        transfer.initiator.fullName ?: transfer.initiator.employeeId,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Кому",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        transfer.targetEmployee.fullName ?: transfer.targetEmployee.employeeId,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Иконка раскрытия
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // === Раскрытая часть ===
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(10.dp))

                // Роль
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Роль: ${transfer.assignmentTypeRu ?: transfer.assignmentType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ID передачи
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "ID передачи: ${transfer.transferId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Комментарий инициатора
                transfer.initiatorComment?.takeIf { it.isNotBlank() }?.let { comment ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Комментарий инициатора",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Комментарий получателя
                transfer.responderComment?.takeIf { it.isNotBlank() }?.let { comment ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Комментарий получателя",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Дата ответа
                transfer.respondedAt?.let { respondedAt ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ответ: ${respondedAt.formatIsoToReadable() ?: respondedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // === Кнопка отмены ===
                if (canCancel) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onCancelTransfer(transfer.transferId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Отменить передачу")
                    }
                }
            }
        }
    }
}

// Хелпер для статуса
data class TransferStatusInfo(
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
private fun transferStatusInfo(status: String): TransferStatusInfo {
    return when (status.uppercase()) {
        "PENDING" -> TransferStatusInfo(
            label = "Ожидает",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        "ACCEPTED" -> TransferStatusInfo(
            label = "Принята",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        "DECLINED" -> TransferStatusInfo(
            label = "Отклонена",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        "CANCELLED" -> TransferStatusInfo(
            label = "Отменена",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> TransferStatusInfo(
            label = status,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== ДИАЛОГ ПЕРЕДАЧИ (без изменений) ====================
@Composable
fun AssetTransferDialog(
    asset: AssetResponseDto,
    targetEmployee: EmployeeShortResponse,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (assignmentType: String, comment: String?) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var assignmentType by remember { mutableStateOf("user") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(Icons.Default.Send, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Передать актив",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(asset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("Инв. №: ${asset.inventoryId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        asset.serialNumber?.let { serial ->
                            Text("Серийный №: $serial",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Получатель",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            targetEmployee.fullNameRu ?: targetEmployee.employeeId,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        targetEmployee.position?.name?.let { position ->
                            Text(position,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(assignmentType, comment.takeIf { it.isNotBlank() })
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Передать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Отмена") }
        }
    )
}

// ==================== PREVIEWS ====================
@Preview(showBackground = true, showSystemUi = true, name = "Вкладка Основное", device = "spec:width=380dp,height=1000dp")
@Composable
private fun AssetDetailsPreview_MainTab() {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                assetHistory = emptyList(),
                isEditing = false,
                editState = AssetEditState.fromAsset(getSampleAsset()),
                selectedTab = 0,
                pagerState = pagerState,
                onTabSelected = {},
                onEditStateChange = {},
                onToggleEdit = {},
                onSave = {},
                onCancelEdit = {},
                onBackClick = {},
                onNavigateToNotifications = {},
                onNavigateToParent = {},
                onRetryClick = {},
                onAddUser = {},
                onRemoveUser = { _, _ -> },
                onTransferClick = {},
                assetTransfers = listOf(
                    AssetTransferDto(
                        transferId = 1,
                        assetId = 48,
                        initiator = EmployeeInfoDto("0000010680", "Малых Андрей Владимирович"),
                        targetEmployee = EmployeeInfoDto("0000015370", "Малышев Тимур Максимович"),
                        assignmentType = "user",
                        assignmentTypeRu = "Пользователь",
                        status = "ACCEPTED",
                        initiatorComment = "Передаю коллеге",
                        responderComment = null,
                        createdAt = "2026-09-22T12:11:50.200708Z",
                        respondedAt = "2026-09-25T09:35:55.606425Z"
                    )
                ),
                currentEmployeeId = "0000015370",   // или null
                onCancelTransfer = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Вкладка Люди", device = "spec:width=380dp,height=700dp")
@Composable
private fun AssetDetailsPreview_UserTab() {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                assetHistory = emptyList(),
                isEditing = false,
                editState = AssetEditState.fromAsset(getSampleAsset()),
                selectedTab = 1,  // ← ИСПРАВЛЕНО: было 2
                pagerState = pagerState,
                onTabSelected = {},
                onEditStateChange = {},
                onToggleEdit = {},
                onSave = {},
                onCancelEdit = {},
                onBackClick = {},
                onNavigateToNotifications = {},
                onNavigateToParent = {},
                onRetryClick = {},
                onAddUser = {},
                onRemoveUser = { _, _ -> },
                onTransferClick = {},
                assetTransfers = listOf(
                    AssetTransferDto(
                        transferId = 1,
                        assetId = 48,
                        initiator = EmployeeInfoDto("0000010680", "Малых Андрей Владимирович"),
                        targetEmployee = EmployeeInfoDto("0000015370", "Малышев Тимур Максимович"),
                        assignmentType = "user",
                        assignmentTypeRu = "Пользователь",
                        status = "ACCEPTED",
                        initiatorComment = "Передаю коллеге",
                        responderComment = null,
                        createdAt = "2026-09-22T12:11:50.200708Z",
                        respondedAt = "2026-09-25T09:35:55.606425Z"
                    )
                ),
                currentEmployeeId = "0000015370",   // или null
                onCancelTransfer = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Вкладка История", device = "spec:width=380dp,height=600dp")
@Composable
private fun AssetDetailsPreview_HistoryTab() {
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 3 })
    val sampleHistory = listOf(
        AssetHistoryDto(
            id = 12, assetId = 48, actionType = "update",
            fieldName = "asset_status_id", oldValue = "9", newValue = "10",
            changedBy = "0000015370",
            changedAt = "2026-09-03T06:26:16.840853Z",
            comment = "comment",
            sessionId = "session_1",
            changerFullNameRu = "Малышев Тимур Максимович",
            changerFullNameEn = "TMM"
        )
    )
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                assetHistory = sampleHistory,
                isEditing = false,
                editState = AssetEditState.fromAsset(getSampleAsset()),
                selectedTab = 2,
                pagerState = pagerState,
                onTabSelected = {},
                onEditStateChange = {},
                onToggleEdit = {},
                onSave = {},
                onCancelEdit = {},
                onBackClick = {},
                onNavigateToNotifications = {},
                onNavigateToParent = {},
                onRetryClick = {},
                onAddUser = {},
                onRemoveUser = { _, _ -> },
                onTransferClick = {},
                assetTransfers = listOf(
                    AssetTransferDto(
                        transferId = 1,
                        assetId = 48,
                        initiator = EmployeeInfoDto("0000010680", "Малых Андрей Владимирович"),
                        targetEmployee = EmployeeInfoDto("0000015370", "Малышев Тимур Максимович"),
                        assignmentType = "user",
                        assignmentTypeRu = "Пользователь",
                        status = "ACCEPTED",
                        initiatorComment = "Передаю коллеге",
                        responderComment = null,
                        createdAt = "2026-09-22T12:11:50.200708Z",
                        respondedAt = "2026-09-25T09:35:55.606425Z"
                    )
                ),
                currentEmployeeId = "0000015370",   // или null
                onCancelTransfer = {}
            )
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
        assetTypeName = "Оборудование M&U",
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