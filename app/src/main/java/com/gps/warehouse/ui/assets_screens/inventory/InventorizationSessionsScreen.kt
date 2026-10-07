package com.gps.warehouse.ui.assets_screens.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsScreen(
    navController: NavHostController,
    assetViewModel: AssetViewModel
) {
    // Отдельные StateFlow для данных и UI-статуса
    val inventorizationSessions by assetViewModel.inventorizationSessions.collectAsState()
    val assetTypes by assetViewModel.assetTypes.collectAsState()
    val inventorizationUiState by assetViewModel.inventorizationUiState.collectAsState() // Только статус

    LaunchedEffect(Unit) {
        assetViewModel.loadAssetTypes()
        assetViewModel.loadInventorizationSessions()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAssetTypeId by remember { mutableIntStateOf(0) }

    InventorizationSessionsContent(
        sessions = inventorizationSessions,
        assetTypes = assetTypes,
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
                assetTypeId=assetTypeId,
                departmentCode=departmentCode,
                startDate=startDate,
                endDate=endDate
                )
            showCreateDialog = false
        },
        onRetry = { assetViewModel.loadInventorizationSessions() },
        onBackClick = { navController.popBackStack() }
    )
}

// ==================== CONTENT: UI + Preview ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsContent(
    sessions: List<InventorizationSessionDto>?,
    assetTypes: List<AssetTypeDto>,
    uiState: AssetViewModel.InventorizationUiState,
    showCreateDialog: Boolean,
    selectedAssetTypeId: Int,
    onSessionClick: (sessionId: Int, isCompleted: Boolean) -> Unit,
    onShowCreateDialogChange: (Boolean) -> Unit,
    onSelectedAssetTypeIdChange: (Int) -> Unit,
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.InventorizationUiState.SessionsLoaded -> {
                val items = sessions.orEmpty()
                if (items.isEmpty()) {
                    EmptySessionsState(modifier = Modifier.padding(paddingValues))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
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
            onDismiss = { onShowCreateDialogChange(false) },
            onSelectedTypeChange = { onSelectedAssetTypeIdChange(it ?: 0) },
            onCreateSession = { assetTypeId, deptCode, start, end ->
//                assetViewModel.startInventorizationSession(
//                onCreateSession(
//                    assetTypeId = assetTypeId,
//                    departmentCode = deptCode,
//                    startDate = start,
//                    endDate = end
//                )
                onShowCreateDialogChange(false)
            }
        )
    }
}

// ==================== ДИАЛОГ: Отдельный Composable ====================
// В InventorizationSessionsScreen.kt, замените CreateInventorySessionDialog на этот:

@Composable
fun CreateInventorySessionDialog(
    assetTypes: List<AssetTypeDto>,
    selectedAssetTypeId: Int,
    onDismiss: () -> Unit,
    onSelectedTypeChange: (Int?) -> Unit, // Изменено на Int?
    onCreateSession: (assetTypeId: Int?, departmentCode: String?, startDate: String?, endDate: String?) -> Unit
) {
    var departmentCode by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AddBox, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Новая инвентаризация") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Укажите тип актива ИЛИ коды департаментов:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Выбор типа актива (Опционально)
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectedTypeChange(null) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedAssetTypeId == 0, onClick = { onSelectedTypeChange(null) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Не указан (только департаменты)")
                        }
                    }
                    items(assetTypes) { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectedTypeChange(type.assetTypeId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedAssetTypeId == type.assetTypeId, onClick = { onSelectedTypeChange(type.assetTypeId) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Коды департаментов
                OutlinedTextField(
                    value = departmentCode,
                    onValueChange = { departmentCode = it },
                    label = { Text("Коды департаментов (через ;)") },
                    placeholder = { Text("Например: RU01050099;RU01050020") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Даты
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Дата начала (ГГГГ-ММ-ДД)") },
                    placeholder = { Text("2026-10-07") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Дата окончания (ГГГГ-ММ-ДД)") },
                    placeholder = { Text("2026-10-31") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateSession(
                        if (selectedAssetTypeId > 0) selectedAssetTypeId else null,
                        departmentCode.takeIf { it.isNotBlank() },
                        startDate.takeIf { it.isNotBlank() },
                        endDate.takeIf { it.isNotBlank() }
                    )
                },
                enabled = (selectedAssetTypeId > 0 || departmentCode.isNotBlank()) && assetTypes.isNotEmpty()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (session.status) {
                    "in_progress" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    "completed" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (session.status) {
                            "in_progress" -> Icons.Default.Sync
                            "completed" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Inventory2
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = when (session.status) {
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
                    text = "Дата начала: ${session.startDate?.take(10) ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Дата завершения: ${session.startDate?.take(10) ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = when (session.status) {
                    "in_progress" -> MaterialTheme.colorScheme.primaryContainer
                    "completed" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = when (session.status) {
                        "in_progress" -> "В процессе"
                        "completed" -> "Завершена"
                        else -> session.status
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (session.status) {
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

// ==================== PREVIEWS ====================
@PreviewLightDark
@Composable
private fun InventorizationSessionsContentPreview_Loaded() {
    val mockSessions = listOf(
        InventorizationSessionDto(
            sessionId = 1,
            assetTypeId = 1,
            departmentCodes = "",
            assetTypeName = "Компьютеры",
            assetTypeEnName = "computers",
            status = "in_progress",
            createdAt = "2026-07-23T08:59:53.158615Z",
            startDate = "2026-07-23T08:59:53.158615Z",
            endDate = "2026-07-30T08:59:53.158615Z",
            createdBy = "0000015370",
            createdByFullName = "ФИО",
        ),
        InventorizationSessionDto(
            sessionId = 2,
            assetTypeId = 7,
            departmentCodes = "RU01050099",
            assetTypeName = "Сетевое оборудование",
            assetTypeEnName = "network_equipment",
            status = "completed",
            createdAt = "2026-07-23T08:59:53.158615Z",
            startDate = "2026-07-30T08:59:53.158615Z",
            endDate = "2026-07-30T08:59:53.158615Z",
            createdBy = "0000015370",
            createdByFullName = "ФИО",
        )
    )
    MaterialTheme {
        Surface {
            InventorizationSessionsContent(
                sessions = listOf(
                    InventorizationSessionDto(
                        sessionId = 1,
                        assetTypeId = 1,
                        departmentCodes = "Компьютеры",
                        assetTypeName = "computers",
                        assetTypeEnName = "in_progress",
                        status = "2026-07-23T08:59:53.158615Z",
                        createdAt = "2026-07-09T08:59:53.158615Z",
                        startDate = "2026-08-23T08:59:53.158615Z"
                    ),
                    InventorizationSessionDto(
                        sessionId = 2,
                        assetTypeId = 7,
                        departmentCodes = null,
                        assetTypeName = "Сетевое оборудование",
                        assetTypeEnName = "network_equipment",
                        status = "completed",
                        createdAt = "2026-07-20T14:30:00.000000Z"
                    )
                ),
                assetTypes = listOf(
                    AssetTypeDto(
                        assetTypeId = 1,
                        name = "Компьютеры",
                        enName = "computers",
                        createdBy = null,
                        createdAt = "2026-07-06T07:18:41.873769",
                        updatedAt = null
                    ),
                    AssetTypeDto(
                        assetTypeId = 7,
                        name = "Сетевое оборудование",
                        enName = "network_equipment",
                        createdBy = null,
                        createdAt = "2026-07-06T07:21:39.334371",
                        updatedAt = null
                    )
                ),
                uiState = AssetViewModel.InventorizationUiState.SessionsLoaded(mockSessions),
                showCreateDialog = false,
                selectedAssetTypeId = 0,
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

@Preview
@Composable
private fun CreateInventorySessionDialogPreview() {
    MaterialTheme {
        CreateInventorySessionDialog(
            assetTypes = listOf(
                AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
                AssetTypeDto(7, "Сетевое оборудование", "network_equipment", null, "2026-07-06T07:21:39.334371", null)
            ),
            selectedAssetTypeId = 1,
            onDismiss = {},
            onSelectedTypeChange = {},
            onCreateSession = { _, _, _, _ -> },
        )
    }
}