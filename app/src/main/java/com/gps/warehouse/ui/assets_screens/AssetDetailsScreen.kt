package com.gps.warehouse.ui.assets_screens

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
import com.gps.warehouse.ui.components.MyCustomActionBar
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

    var isEditing by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Единое состояние для редактирования
    var editState by remember { mutableStateOf<AssetEditState?>(null) }

    // Загружаем данные при открытии
    LaunchedEffect(assetId) {
        viewModel.loadAssetDetails(assetId)
        viewModel.loadAssetStatuses()
        viewModel.loadAssetTypes()
        viewModel.loadAssetHistory(assetId)
    }

    // Инициализируем editState при загрузке актива
    LaunchedEffect(uiState) {
        (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
            editState = AssetEditState.fromAsset(original)
        }
        // Отключаем редактирование только после получения новых данных
        if (isEditing) {
            isEditing = false
        }
    }

    AssetDetailsContent(
        uiState = uiState,
        assetStatuses = assetStatuses,
        assetTypes = assetTypes,
        isEditing = isEditing,
        editState = editState,
        onEditStateChange = { editState = it },
        onToggleEdit = { isEditing = !isEditing },
        onSave = {
            editState?.let { state ->
                // Безопасное приведение + let
                (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                    viewModel.updateAsset(assetId, state.toUpdate(original))
                }
                viewModel.loadAssetDetails(assetId)
            }
        },
        onCancelEdit = {
            isEditing = false
            // Восстанавливаем editState из оригинала
            (uiState as? AssetViewModel.AssetUiState.AssetDetailsLoaded)?.asset?.let { original ->
                editState = AssetEditState.fromAsset(original)
            }
        },
        onShowHistory = { showHistoryDialog = true },
        onBackClick = { navController.popBackStack() },
        onNavigateToParent = { parentId -> navController.navigate("asset_details/$parentId") }
    )

    // Диалог истории
    if (showHistoryDialog) {
        AssetHistoryDialog(
            history = viewModel.assetHistory.collectAsState().value,
            onDismiss = { showHistoryDialog = false }
        )
    }
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
    onSave: () -> Unit,  // Больше не принимает AssetUpdate
    onCancelEdit: () -> Unit,
    onShowHistory: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToParent: (Int) -> Unit
) {
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
                            // Кнопка истории
                            IconButton(onClick = onShowHistory) {
                                Icon(Icons.Default.History, "История", tint = MaterialTheme.colorScheme.primary)
                            }
                            // Кнопка редактирования / сохранения
                            if (isEditing) {
                                IconButton(onClick = onSave) {
                                    Icon(Icons.Default.Save, "Сохранить", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = onCancelEdit) {
                                    Icon(Icons.Default.Close, "Отмена", tint = MaterialTheme.colorScheme.error)
                                }
                            } else {
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
                                onEditStateChange(editState?.copy(assetStatusId = newStatusId) ?: AssetEditState())
                            }
                        )
                    }

                    // Основная информация
                    item {
                        if (isEditing) {
                            // Передаём editState и callback
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
                    item {
                        LocationCard(location = asset.location, isEditing = isEditing)
                    }

                    // Сервисная информация
                    item {
                        ServiceCard(asset = asset, isEditing = isEditing)
                    }

                    // Пользователи
                    item {
                        UsersSection(
                            title = "Пользователи",
                            users = asset.users,
                            icon = Icons.Default.Person,
                            isEditing = isEditing,
                            onAddUser = if (isEditing) { { /* TODO: открыть диалог выбора */ } } else null
                        )
                    }
                    item {
                        UsersSection(
                            title = "Ответственные",
                            users = asset.responsibleUsers,
                            icon = Icons.Default.VerifiedUser,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            isEditing = isEditing,
                            onAddUser = if (isEditing) { { /* TODO: открыть диалог выбора */ } } else null
                        )
                    }
                    item {
                        UsersSection(
                            title = "Обслуживающий персонал",
                            users = asset.servingUsers,
                            icon = Icons.Default.Build,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            isEditing = isEditing,
                            onAddUser = if (isEditing) { { /* TODO: открыть диалог выбора */ } } else null
                        )
                    }

                    // Мета-информация
                    item {
                        MetaInfoCard(asset = asset)
                    }
                }
            }
        }
        is AssetViewModel.AssetUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Retry */ }) { Text("Повторить") }
                }
            }
        }
        else -> {}
    }
}

// ==================== КАРТОЧКА СТАТУСА ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCard(
    asset: AssetResponseDto,
    isEditing: Boolean,
    assetStatuses: List<AssetStatusDto>,
    editState: AssetEditState? = null,  // Новый параметр
    onStatusChange: (Int?) -> Unit = {}  // Callback для обновления статуса
) {
    val statusText = if (isEditing) {
        // В режиме редактирования берём статус из editState или из asset
        assetStatuses.find { it.id == (editState?.assetStatusId ?: asset.assetStatusId) }?.status
            ?: asset.assetStatus ?: "Не указан"
    } else {
        asset.assetStatus ?: "Не указан"
    }

    val statusColor = when (asset.assetStatus?.lowercase()) {
        "приемка", "отремонтирован", "на складе", "в работе" -> Color(0, 150, 0, 170)
        "удален", "списан" -> Color(220, 0, 0, 170)
        "на обслуживании", "ожидает зч", "требует проверки", "в ремонте" -> Color(255, 193, 7, 170)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (asset.assetStatusId == 10) Icons.Default.CheckCircle else Icons.Default.Info,
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
                    // DROPDOWN для выбора статуса
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = statusText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Выберите статус") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            assetStatuses.forEach { statusDto ->
                                DropdownMenuItem(
                                    text = { Text(statusDto.status) },
                                    onClick = {
                                        onStatusChange(statusDto.id)  // Обновляем статус через callback
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (statusDto.id == (editState?.assetStatusId ?: asset.assetStatusId)) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Режим просмотра — просто текст
                    Text(statusText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "ID: ${asset.assetId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
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
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                )
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
        // ReadOnly-версия
        if (!isEditing) {
            InfoRow(label = "Цех", value = location.workshopName)
            InfoRow(label = "Место", value = location.place)
            InfoRow(label = "Этаж", value = location.level?.toString())
        }
        // Editable-версия (заглушка — редактирование на карте)
        else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Редактирование локации доступно на карте", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { /* TODO: открыть карту */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Открыть карту")
            }
        }
    }
}

// ==================== СЕРВИСНАЯ ИНФОРМАЦИЯ ====================
@Composable
fun ServiceCard(asset: AssetResponseDto, isEditing: Boolean) {
    InfoSectionCard(icon = Icons.Default.MiscellaneousServices, title = "Сервис") {
        if (!isEditing) {
            // ReadOnly-версия
            InfoRow(label = "Еженедельная проверка", value = if (asset.everyWeekCheck == true) "Да" else "Нет")
            InfoRow(label = "След. обслуживание", value = asset.nextService?.formatIsoToReadable(pattern = "dd.MM.yyyy"))
            InfoRow(label = "Период (дни)", value = asset.servicePeriod?.toString())
        } else {
            // Editable-версия
            OutlinedTextField(
                value = if (asset.everyWeekCheck == true) "Да" else "Нет",
                onValueChange = { /* TODO: update editState */ },
                label = { Text("След. обслуживание") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = asset.nextService ?: "",
                onValueChange = { /* TODO: update editState */ },
                label = { Text("След. обслуживание") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = asset.servicePeriod?.toString() ?: "",
                onValueChange = { /* TODO: update editState */ },
                label = { Text("Период (дни)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
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
    isEditing: Boolean = false,  // Новый параметр
    onAddUser: (() -> Unit)? = null  // Callback для добавления (в режиме редактирования)
) {
    if (users.isNullOrEmpty() && !isEditing) return

    InfoSectionCard(icon = icon, title = title) {
        if (!isEditing) {
            // ReadOnly-версия: список пользователей
            users?.forEach { user ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullNameRu ?: user.employeeId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        user.position?.name?.let { position ->
                            Text(position, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        user.phone?.let { phone -> Text(phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        user.email?.let { email -> Text(email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                if (user != users.last()) {
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                }
            }
        } else {
            // Editable-версия: список + кнопка добавления
            users?.forEach { user ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullNameRu ?: user.employeeId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    // Кнопка удаления пользователя (TODO)
                    IconButton(onClick = { /* TODO: remove user */ }) {
                        Icon(Icons.Default.Close, "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
                if (user != users.last()) {
                    HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                }
            }

            // Кнопка добавления пользователя
            if (onAddUser != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAddUser,
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
fun AssetHistoryDialog(
    history: List<AssetHistoryDto>,
    onDismiss: () -> Unit
) {
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
                                Text(entry.changedAt.formatIsoToReadable("dd.MM HH:mm") ?: "", style = MaterialTheme.typography.labelSmall)
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
        confirmButton = {
//            Button(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

// ==================== PREVIEWS ====================
@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Детали актива",
    device = "spec:width=380dp,height=1750dp"
)
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
                onNavigateToParent = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Детали актива (Редактирование)",
    device = "spec:width=380dp,height=1850dp",
)
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
                onNavigateToParent = {}
            )
        }
    }
}

@Preview
@Composable
fun AssetHistoryDialogPreview(){
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
            AssetHistoryDialog(
                history = listOf(history),
                onDismiss = {}
            )
        }
    }
}

private fun getSampleAsset(): AssetResponseDto {
    return AssetResponseDto(
        assetId = 48,
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

        // Сервисная информация
        everyWeekCheck = false,
        nextService = "2026-09-08",
        servicePeriod = 5,

        // Мета
        createdBy = "0000012657",
        updatedBy = "0000015370",
        createdAt = "2026-07-14T19:23:04.784110",
        updatedAt = "2026-09-03T09:26:16.840853",
        assetTypeName = "Оборудование MU",

        // Вложенные объекты
        location = AssetLocationResponse(
            workshopId = 6,
            workshopName = "Логистика",
            place = "mesto213111111111111",
            level = 4,
            x = 237,
            y = 415
        ),

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
                position = PositionResponse(name = "Пользователь", nameEn = null),
                startDate = "2026-08-24",
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
                position = PositionResponse(name = "Администратор", nameEn = null),
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
                society = null,
                department = null,
                division = null,
                group = null,
                position = PositionResponse(name = "Техник", nameEn = null),
                startDate = "2026-09-02",
                endDate = null,
                assignmentType = "serving"
            )
        ),

        // Текущий пользователь
        currentUser = "0000012657",
        currentUserFullName = "Евсиков Константин Александрович",

        // Родительский актив
        parent = null
    )
}