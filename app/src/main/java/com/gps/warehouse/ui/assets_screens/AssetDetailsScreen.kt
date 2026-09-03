//package com.gps.warehouse.ui.assets_screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavHostController
//import com.gps.warehouse.data.remote.assets_dto.AssetParentResponseDto
//import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
//import com.gps.warehouse.data.remote.assets_dto.AssetUserResponseDto
//import com.gps.warehouse.data.remote.assets_dto.LocationResponseDto
//import com.gps.warehouse.ui.AssetViewModel
//import com.gps.warehouse.ui.components.ErrorStateView
//import com.gps.warehouse.ui.components.MyCustomActionBar
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AssetDetailsScreen(
//    assetId: Int,
//    navController: NavHostController,
//    viewModel: AssetViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//
//    LaunchedEffect(assetId) {
//        viewModel.loadAssetDetails(assetId)
//    }
//
//    Scaffold(
//        topBar = {
//            MyCustomActionBar(
//                text = "Детали актива",
//                onBackClick = { navController.popBackStack() }
//            )
//        }
//    ) { paddingValues ->
//        when (val state = uiState) {
//            is AssetViewModel.AssetUiState.Loading -> {
//                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator()
//                }
//            }
//            is AssetViewModel.AssetUiState.AssetDetailsLoaded -> {
//                AssetDetailsContent(
//                    asset = state.asset,
//                    modifier = Modifier.padding(paddingValues)
//                )
//            }
//            is AssetViewModel.AssetUiState.Error -> {
//                ErrorStateView(
//                    message = state.message,
//                    onRetry = { viewModel.loadAssetDetails(assetId) }
//                )
//            }
//            else -> {}
//        }
//    }
//}
//
//@Composable
//fun AssetDetailsContent(asset: AssetResponseDto, modifier: Modifier = Modifier) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .verticalScroll(rememberScrollState())
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        // Основная информация
//        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                Text("Основная информация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
//                Spacer(modifier = Modifier.height(12.dp))
//                DetailRow("Название", asset.name)
//                DetailRow("Инвентарный номер", asset.inventoryId)
//                DetailRow("Серийный номер", asset.serialNumber)
//                DetailRow("Статус", asset.assetStatus)
//                DetailRow("Тип актива", asset.assetTypeName)
//                DetailRow("Модель", asset.modelName)
//                asset.comment?.let { DetailRow("Комментарий", it) }
//            }
//        }
//
//        // Даты и создание
//        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                Text("Информация о создании", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
//                Spacer(modifier = Modifier.height(12.dp))
//                DetailRow("Дата ввода в эксплуатацию", asset.dateIssue)
//                DetailRow("Дата покупки", asset.datePurchasing)
//                DetailRow("Создано", asset.createdAt)
//                DetailRow("Обновлено", asset.updatedAt)
//                DetailRow("Создал", asset.createdBy)
//                DetailRow("Обновил", asset.updatedBy)
//            }
//        }
//
//        // Локация (если есть)
//        asset.location?.let { loc ->
//            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text("Локация", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
//                    Spacer(modifier = Modifier.height(12.dp))
//                    DetailRow("Наименование", loc.workshopName)
//                    DetailRow("Место", loc.place)
//                    DetailRow("Этаж", loc.level.toString())
//                }
//            }
//        }
//
//        // Пользователи (если есть)
//        if (!asset.users.isNullOrEmpty()) {
//            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text("Закреплённые пользователи", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
//                    Spacer(modifier = Modifier.height(12.dp))
//                    asset.users.forEach { user ->
//                        DetailRow("Сотрудник", user.fullNameRu)
//                        DetailRow("Табельный номер", user.employeeId)
//                        DetailRow("Дата начала", user.startDate)
//                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//                    }
//                }
//            }
//        }
//
//        // Родительский актив (если есть)
//        asset.parent?.let { parent ->
//            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text("Родительский актив", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
//                    Spacer(modifier = Modifier.height(12.dp))
//                    DetailRow("Название", parent.name)
//                    DetailRow("Инвентарный номер", parent.inventoryId)
//                    DetailRow("Серийный номер", parent.serialNumber)
//                    DetailRow("Тип", parent.assetTypeName)
//                    DetailRow("Статус", parent.assetStatus)
//                }
//            }
//        }
//    }
//}
//
//
//// PREVIEW ФУНКЦИИ И MOCK ДАННЫЕ
//@Preview(showBackground = true, showSystemUi = true, name = "Детали актива (Полные)", device = "spec:width=380dp,height=1270dp")
//@Composable
//fun AssetDetailsContentPreview_Full() {
//    MaterialTheme {
//        Surface(modifier = Modifier.fillMaxSize()) {
//            AssetDetailsContent(
//                asset = getSampleFullAssetResponseDto(),
//                modifier = Modifier.fillMaxSize()
//            )
//        }
//    }
//}
//
//// MOCK ДАННЫЕ
//private fun getSampleFullAssetResponseDto(): AssetResponseDto {
//    return AssetResponseDto(
//        assetId = 1,
//        name = "Ноутбук Lenovo ThinkPad X1",
//        inventoryId = "INV-2024-00158",
//        serialNumber = "SN-9876543210",
//        assetStatus = "В эксплуатации",
//        comment = "Выдан системному администратору",
//        dateIssue = "2024-01-15",
//        datePurchasing = "2024-01-10",
//        modelId = 55,
//        modelName = "ThinkPad X1 Carbon Gen 11",
//        assetTypeId = 1,
//        parentId = 2,
//        locationId = 1,
//        preparedBy = "0000015370",
//        checkedBy = "0000015370",
//        parentName = "Рабочая станция №12",
//        manufacturerName = "Lenovo",
//        vendorName = "ООО ТехноПоставка",
//        osName = "Windows 11 Pro",
//        createdBy = "0000015370",
//        updatedBy = "0000015370",
//        createdAt = "2024-01-01T10:00:00Z",
//        updatedAt = "2024-01-15T12:30:00Z",
//        assetTypeName = "Компьютер",
//        location = LocationResponseDto(
////            locationId = 1,
////            name = "Центральный склад",
////            address = "г. Тула, ул. Ленина, д. 10"
//            workshopId = 1,
//            workshopName = "Логистика",
//            place = "mesto",
//            level = 4,
//            x = 0, y = 0
//        ),
//        users = listOf(
//            AssetUserResponseDto(
//                guid = "14ba77ab-2d91-11f1-a3cb-000c290ca5c4",
//                employeeId = "0000015370",
//                fullNameRu = "Малышев Тимур Максимович",
//                fullNameEn = "Malyshev Timur Maksimovich",
//                startDate = "2024-01-15",
//                endDate = null
//            )
//        ),
//        parent = AssetParentResponseDto(
//            assetId = 2,
//            name = "Рабочая станция №12",
//            inventoryId = "INV-WS-12",
//            serialNumber = "SN-WS-12",
//            assetStatus = "В эксплуатации",
//            comment = null,
//            dateIssue = null,
//            datePurchasing = null,
//            modelId = null,
//            modelName = null,
//            assetTypeId = null,
//            parentId = null,
//            locationId = null,
//            preparedBy = null,
//            checkedBy = null,
//            parentName = null,
//            manufacturerName = null,
//            vendorName = null,
//            osName = null,
//            createdBy = null,
//            updatedBy = null,
//            createdAt = "2024-01-01T10:00:00Z",
//            updatedAt = null,
//            assetTypeName = "Рабочая станция"
//        )
//    )
//}

package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.*
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar
import java.time.format.DateTimeFormatter
import kotlin.collections.isNullOrEmpty

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

    // Загружаем данные при открытии
    LaunchedEffect(assetId) {
        viewModel.loadAssetDetails(assetId)
        viewModel.loadAssetStatuses()
        viewModel.loadAssetTypes()
        viewModel.loadAssetHistory(assetId)
    }

    AssetDetailsContent(
        uiState = uiState,
        assetStatuses = assetStatuses,
        assetTypes = assetTypes,
        isEditing = isEditing,
        onToggleEdit = { isEditing = !isEditing },
        onSave = { updatedAsset -> viewModel.updateAsset(assetId, updatedAsset) },
        onCancelEdit = { isEditing = false },
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
    onToggleEdit: () -> Unit,
    onSave: (AssetUpdate) -> Unit,
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
                                IconButton(onClick = { onSave(buildAssetUpdate(asset)) }) {
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
                        StatusCard(asset = asset, isEditing = isEditing, assetStatuses = assetStatuses)
                    }

                    // Основная информация
                    item {
                        if (isEditing) {
                            EditableInfoSection(asset = asset, assetTypes = assetTypes)
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
                            icon = Icons.Default.Person
                        )
                    }
                    item {
                        UsersSection(
                            title = "Ответственные",
                            users = asset.responsibleUsers,
                            icon = Icons.Default.VerifiedUser,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    item {
                        UsersSection(
                            title = "Обслуживающий персонал",
                            users = asset.servingUsers,
                            icon = Icons.Default.Build,
                            color = MaterialTheme.colorScheme.tertiaryContainer
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
@Composable
fun StatusCard(asset: AssetResponseDto, isEditing: Boolean, assetStatuses: List<AssetStatusDto>) {
    val statusColor = when (asset.assetStatus.lowercase()) {
        "в работе", "active", "приемка" -> Color(0, 150, 0, 170)
        "списан", "inactive", "удален" -> Color(220, 0, 0, 170)
        "в ремонте", "ожидает зч" -> Color(255, 193, 7, 170)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    // TODO: Dropdown для выбора статуса
                    Text(asset.assetStatus ?: "Не указан", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(asset.assetStatus ?: "Не указан", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    InfoSectionCard(title = "Основная информация") {
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
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
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
fun EditableInfoSection(asset: AssetResponseDto, assetTypes: List<AssetTypeDto>) {
    var name by remember { mutableStateOf(asset.name) }
    var inventoryId by remember { mutableStateOf(asset.inventoryId) }
    var serialNumber by remember { mutableStateOf(asset.serialNumber ?: "") }
    var quantity by remember { mutableStateOf(asset.quantity?.toString() ?: "") }
    var comment by remember { mutableStateOf(asset.comment ?: "") }

    InfoSectionCard(title = "Редактирование") {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = inventoryId,
            onValueChange = { inventoryId = it },
            label = { Text("Инв. номер") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = serialNumber,
            onValueChange = { serialNumber = it },
            label = { Text("Серийный номер") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Тип актива (dropdown)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = assetTypes.find { it.assetTypeId == asset.assetTypeId }?.name ?: "Выберите тип",
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
                        onClick = { /* TODO: update asset_type_id */ expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Количество") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Комментарий") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
    }
}

@Composable
fun InfoSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
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
fun LocationCard(location: LocationResponseDto?, isEditing: Boolean) {
    if (location == null) return

    InfoSectionCard(title = "📍 Локация") {
        InfoRow(label = "Цех", value = location.workshopName)
        InfoRow(label = "Место", value = location.place)
        InfoRow(label = "Уровень", value = location.level?.toString())
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Координаты", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("X: ${location.x}, Y: ${location.y}", style = MaterialTheme.typography.bodyMedium)
        }
        if (isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Редактирование локации доступно на карте", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== СЕРВИСНАЯ ИНФОРМАЦИЯ ====================
@Composable
fun ServiceCard(asset: AssetResponseDto, isEditing: Boolean) {
    InfoSectionCard(title = "🔧 Сервис") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Еженедельная проверка", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (asset.every_week_check == true) "✅ Да" else "❌ Нет", style = MaterialTheme.typography.bodyMedium)
            }
            if (isEditing) {
                // TODO: Switch для every_week_check
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        InfoRow(label = "След. обслуживание", value = asset.next_service?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        InfoRow(label = "Период (дни)", value = asset.service_period?.toString())
    }
}

// ==================== ПОЛЬЗОВАТЕЛИ ====================
@Composable
fun UsersSection(title: String, users: List<AssetUserFullResponse>?, icon: androidx.ui.graphics.vector.ImageVector, color: Color = MaterialTheme.colorScheme.surfaceVariant) {
    if (users.isNullOrEmpty()) return

    InfoSectionCard(title = title) {
        users.forEach { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.full_name_ru ?: user.employee_id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    user.position?.name?.let { position ->
                        Text(position, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    user.phone?.let { phone ->
                        Text(phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    user.email?.let { email ->
                        Text(email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (user != users.last()) {
                HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            }
        }
    }
}

// ==================== МЕТА-ИНФОРМАЦИЯ ====================
@Composable
fun MetaInfoCard(asset: AssetResponseDto) {
    InfoSectionCard(title = "ℹ️ Мета-информация") {
        InfoRow(label = "Создан", value = asset.created_at?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
        InfoRow(label = "Обновлён", value = asset.updated_at?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
        InfoRow(label = "Создал", value = asset.created_by)
        InfoRow(label = "Обновил", value = asset.updated_by)
        InfoRow(label = "Текущий пользователь", value = asset.current_user_full_name)
    }
}

// ==================== ДИАЛОГ ИСТОРИИ ====================
@Composable
fun AssetHistoryDialog(history: List<AssetHistoryDto>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("История изменений") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entry ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(entry.field_name ?: "Поле", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(entry.changed_at?.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")) ?: "", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Кем: ${entry.changer_full_name_ru}", style = MaterialTheme.typography.bodySmall)
                            if (entry.old_value != null || entry.new_value != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text("Было: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    Text(entry.old_value ?: "–", style = MaterialTheme.typography.labelSmall)
                                }
                                Row {
                                    Text("Стало: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(entry.new_value ?: "–", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ====================
private fun buildAssetUpdate(asset: AssetResponseDto): AssetUpdate {
    return AssetUpdate(
        name = asset.name,
        inventory_id = asset.inventory_id,
        serial_number = asset.serial_number,
        asset_status_id = asset.asset_status_id,
        quantity = asset.quantity,
        comment = asset.comment,
        // ... остальные поля
        current_user = asset.current_user
    )
}

// ==================== PREVIEWS ====================
@PreviewLightDark
@Composable
private fun AssetDetailsPreview_ViewMode() {
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                isEditing = false,
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
private fun AssetDetailsPreview_EditMode() {
    MaterialTheme {
        Surface {
            AssetDetailsContent(
                uiState = AssetViewModel.AssetUiState.AssetDetailsLoaded(getSampleAsset()),
                assetStatuses = emptyList(),
                assetTypes = emptyList(),
                isEditing = true,
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

private fun getSampleAsset(): AssetResponseDto {
    return AssetResponseDto(
        asset_id = 48,
        name = "Актив 2",
        inventory_id = "INV_NUMBER_48",
        serial_number = "SER_NUMBER_48",
        asset_status = "В работе",
        asset_status_id = 10,
        quantity = 120,
        comment = "Описание123123",
        asset_type_name = "Оборудование MU",
        manufacturer_name = "китай",
        vendor_name = "Z",
        os_name = "окнО",
        created_at = java.time.Instant.now(),
        current_user_full_name = "Евсиков Константин Александрович",
        location = AssetLocationResponse(workshop_id = 6, workshop_name = "Логистика", place = "mesto213", level = 4, x = 237, y = 415)
    )
}