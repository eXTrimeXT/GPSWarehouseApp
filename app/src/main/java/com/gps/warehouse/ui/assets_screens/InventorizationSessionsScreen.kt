//package com.gps.warehouse.ui.assets_screens
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.tooling.preview.PreviewLightDark
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
//import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
//import com.gps.warehouse.ui.AssetViewModel
//import com.gps.warehouse.ui.components.ErrorStateView
//import com.gps.warehouse.ui.components.MyCustomActionBar
//
//// ==================== SCREEN: Логика + Навигация ====================
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun InventorizationSessionsScreen(
//    navController: NavHostController,
//    viewModel: AssetViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//    val assetTypes by viewModel.assetTypes.collectAsState()
//    val inventorizationSessions by viewModel.inventorizationSessions.collectAsState()
//
//    LaunchedEffect(Unit) {
////        viewModel.loadAssetTypes()  // Загружаем типы активов для диалога
//        viewModel.loadInventorizationSessions()
////        viewModel.resetIdle()       // Сброс состояния для загрузки данных инвентаризации
//    }
//
//    var showCreateDialog by remember { mutableStateOf(false) }
//    var selectedAssetTypeId by remember { mutableIntStateOf(0) }
//
//    InventorizationSessionsContent(
//        uiState = uiState,
//        assetTypes = assetTypes,
//        inventorizationSessions = inventorizationSessions,
//        showCreateDialog = showCreateDialog,
//        selectedAssetTypeId = selectedAssetTypeId,
//        onSessionClick = { sessionId, isCompleted ->
//            navController.navigate("inventorization_items/$sessionId/$isCompleted")
//        },
//        onShowCreateDialogChange = { showCreateDialog = it },
//        onSelectedAssetTypeIdChange = { selectedAssetTypeId = it },
//        onCreateSession = { assetTypeId ->
//            viewModel.startInventorizationSession(assetTypeId)
//            showCreateDialog = false
//        },
//        onRetry = { viewModel.loadInventorizationSessions() },
//        onBackClick = { navController.popBackStack() }
//    )
//}
//
//// ==================== CONTENT: UI + Preview ====================
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun InventorizationSessionsContent(
//    uiState: AssetViewModel.AssetUiState,
//    assetTypes: List<AssetTypeDto>,
//    inventorizationSessions: List<InventorizationSessionDto>,
//    showCreateDialog: Boolean,
//    selectedAssetTypeId: Int,
//    onSessionClick: (sessionId: Int, isCompleted: Boolean) -> Unit,
//    onShowCreateDialogChange: (Boolean) -> Unit,
//    onSelectedAssetTypeIdChange: (Int) -> Unit,
//    onCreateSession: (assetTypeId: Int) -> Unit,
//    onRetry: () -> Unit,
//    onBackClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Scaffold(
//        modifier = modifier,
//        topBar = {
//            MyCustomActionBar(
//                text = "Инвентаризация",
//                onBackClick = onBackClick
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { onShowCreateDialogChange(true) },
//                containerColor = MaterialTheme.colorScheme.primary
//            ) {
//                Icon(Icons.Default.Add, contentDescription = "Новая сессия")
//            }
//        }
//    ) { paddingValues ->
//        when (uiState) {
//            is AssetViewModel.AssetUiState.Loading -> {
//                Box(
//                    modifier = Modifier.fillMaxSize().padding(paddingValues),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator()
//                }
//            }
//            is AssetViewModel.AssetUiState.InventorizationSessionsLoaded -> {
//                if (uiState.sessions.isEmpty()) {
//                    EmptySessionsState(modifier = Modifier.padding(paddingValues))
//                } else {
//                    LazyColumn(
//                        modifier = Modifier.fillMaxSize().padding(paddingValues),
//                        contentPadding = PaddingValues(16.dp),
//                        verticalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        items(inventorizationSessions) { session ->
//                            SessionCard(
//                                session = session,
//                                onClick = {
//                                    val isCompleted = session.status == "completed"
//                                    onSessionClick(session.sessionId, isCompleted)
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//            is AssetViewModel.AssetUiState.Error -> {
//                ErrorStateView(
//                    message = uiState.message,
//                    onRetry = onRetry,
//                    modifier = Modifier.padding(paddingValues)
//                )
//            }
//            else -> {}
//        }
//    }
//
//    // ДИАЛОГ СОЗДАНИЯ СЕССИИ
//    if (showCreateDialog) {
//        CreateInventorySessionDialog(
//            assetTypes = assetTypes,
//            selectedAssetTypeId = selectedAssetTypeId,
//            onDismiss = { onShowCreateDialogChange(false) },
//            onSelectedTypeChange = onSelectedAssetTypeIdChange,
//            onCreate = onCreateSession
//        )
//    }
//}
//
//// ==================== ДИАЛОГ: Выносим в отдельный Composable ====================
//@Composable
//fun CreateInventorySessionDialog(
//    assetTypes: List<AssetTypeDto>,
//    selectedAssetTypeId: Int,
//    onDismiss: () -> Unit,
//    onSelectedTypeChange: (Int) -> Unit,
//    onCreate: (Int) -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        icon = { Icon(Icons.Default.AddBox, null, tint = MaterialTheme.colorScheme.primary) },
//        title = { Text("Новая инвентаризации") },
//        text = {
//            Column {
//                Text("Выберите тип актива:", style = MaterialTheme.typography.bodyMedium)
//                Spacer(modifier = Modifier.height(8.dp))
//                if (assetTypes.isEmpty()) {
//                    Text("Загрузка типов...", color = MaterialTheme.colorScheme.onSurfaceVariant)
//                } else {
//                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
//                        items(assetTypes) { type ->
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .clickable { onSelectedTypeChange(type.assetTypeId) }
//                                    .padding(vertical = 8.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                RadioButton(
//                                    selected = selectedAssetTypeId == type.assetTypeId,
//                                    onClick = { onSelectedTypeChange(type.assetTypeId) }
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(type.name)
//                            }
//                        }
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            Button(
//                onClick = { onCreate(selectedAssetTypeId) },
//                enabled = selectedAssetTypeId > 0 && assetTypes.isNotEmpty()
//            ) {
//                Text("Создать")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Отмена")
//            }
//        }
//    )
//}
//
//// ==================== ВСПОМОГАТЕЛЬНЫЕ COMPOSABLES ====================
//@Composable
//private fun EmptySessionsState(modifier: Modifier = Modifier) {
//    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Icon(
//                Icons.Default.Inventory2,
//                null,
//                modifier = Modifier.size(64.dp),
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "Нет активных сессий",
//                style = MaterialTheme.typography.titleLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
//
//@Composable
//fun SessionCard(session: InventorizationSessionDto, onClick: () -> Unit) {
//    Card(
//        modifier = Modifier.fillMaxWidth().clickable { onClick() },
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth().padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Surface(
//                shape = MaterialTheme.shapes.small,
//                color = when (session.status) {
//                    "in_progress" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
//                    "completed" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
//                    else -> MaterialTheme.colorScheme.surfaceVariant
//                },
//                modifier = Modifier.size(40.dp)
//            ) {
//                Box(contentAlignment = Alignment.Center) {
//                    Icon(
//                        imageVector = when (session.status) {
//                            "in_progress" -> Icons.Default.Sync
//                            "completed" -> Icons.Default.CheckCircle
//                            else -> Icons.Default.Inventory2
//                        },
//                        contentDescription = null,
//                        modifier = Modifier.size(22.dp),
//                        tint = when (session.status) {
//                            "in_progress" -> MaterialTheme.colorScheme.onPrimaryContainer
//                            "completed" -> MaterialTheme.colorScheme.onTertiaryContainer
//                            else -> MaterialTheme.colorScheme.onSurfaceVariant
//                        }
//                    )
//                }
//            }
//            Spacer(modifier = Modifier.width(12.dp))
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = session.assetTypeName ?: "Тип #${session.assetTypeId}",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold
//                )
//                Spacer(modifier = Modifier.height(2.dp))
//                Text(
//                    text = "Сессия #${session.sessionId}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//                Text(
//                    text = session.createdAt.take(10),
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//                )
//            }
//            Surface(
//                shape = MaterialTheme.shapes.extraSmall,
//                color = when (session.status) {
//                    "in_progress" -> MaterialTheme.colorScheme.primaryContainer
//                    "completed" -> MaterialTheme.colorScheme.tertiaryContainer
//                    else -> MaterialTheme.colorScheme.surfaceVariant
//                }
//            ) {
//                Text(
//                    text = when (session.status) {
//                        "in_progress" -> "В процессе"
//                        "completed" -> "Завершена"
//                        else -> session.status
//                    },
//                    style = MaterialTheme.typography.labelSmall,
//                    fontWeight = FontWeight.Bold,
//                    color = when (session.status) {
//                        "in_progress" -> MaterialTheme.colorScheme.onPrimaryContainer
//                        "completed" -> MaterialTheme.colorScheme.onTertiaryContainer
//                        else -> MaterialTheme.colorScheme.onSurfaceVariant
//                    },
//                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                )
//            }
//            Spacer(modifier = Modifier.width(4.dp))
//            Icon(
//                Icons.Default.ChevronRight,
//                null,
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//            )
//        }
//    }
//}
//
//// ==================== PREVIEWS ====================
//@PreviewLightDark
//@Composable
//private fun InventorizationSessionsContentPreview_Loaded() {
//    MaterialTheme {
//        Surface {
//            InventorizationSessionsContent(
//                uiState = AssetViewModel.AssetUiState.InventorizationSessionsLoaded(
//                    sessions = listOf(
//                        InventorizationSessionDto(
//                            sessionId = 1,
//                            assetTypeId = 1,
//                            assetTypeName = "Компьютеры",
//                            assetTypeEnName = "computers",
//                            status = "in_progress",
//                            createdAt = "2026-07-23T08:59:53.158615Z"
//                        ),
//                        InventorizationSessionDto(
//                            sessionId = 2,
//                            assetTypeId = 7,
//                            assetTypeName = "Сетевое оборудование",
//                            assetTypeEnName = "network_equipment",
//                            status = "completed",
//                            createdAt = "2026-07-20T14:30:00.000000Z"
//                        )
//                    )
//                ),
//                assetTypes = listOf(
//                    AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
//                    AssetTypeDto(7, "Сетевое оборудование", "network_equipment", null, "2026-07-06T07:21:39.334371", null)
//                ),
//                inventorizationSessions = emptyList(),
//                showCreateDialog = false,
//                selectedAssetTypeId = 0,
//                onSessionClick = { _, _ -> },
//                onShowCreateDialogChange = {},
//                onSelectedAssetTypeIdChange = {},
//                onCreateSession = {},
//                onRetry = {},
//                onBackClick = {}
//            )
//        }
//    }
//}
//
//@Preview
//@Composable
//private fun InventorizationSessionsContentPreview_Empty() {
//    MaterialTheme {
//        Surface {
//            InventorizationSessionsContent(
//                uiState = AssetViewModel.AssetUiState.InventorizationSessionsLoaded(emptyList()),
//                assetTypes = emptyList(),
//                inventorizationSessions = emptyList(),
//                showCreateDialog = false,
//                selectedAssetTypeId = 0,
//                onSessionClick = { _, _ -> },
//                onShowCreateDialogChange = {},
//                onSelectedAssetTypeIdChange = {},
//                onCreateSession = {},
//                onRetry = {},
//                onBackClick = {}
//            )
//        }
//    }
//}
//
//@Preview
//@Composable
//private fun CreateInventorySessionDialogPreview() {
//    MaterialTheme {
//        CreateInventorySessionDialog(
//            assetTypes = listOf(
//                AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
//                AssetTypeDto(7, "Сетевое оборудование", "network_equipment", null, "2026-07-06T07:21:39.334371", null)
//            ),
//            selectedAssetTypeId = 1,
//            onDismiss = {},
//            onSelectedTypeChange = {},
//            onCreate = {}
//        )
//    }
//}


package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: AssetViewModel = hiltViewModel()
) {
    // Отдельные StateFlow для данных и UI-статуса
    val inventorizationSessions by viewModel.inventorizationSessions.collectAsState()
    val assetTypes by viewModel.assetTypes.collectAsState()
    val uiState by viewModel.inventorizationUiState.collectAsState() // Только статус

    LaunchedEffect(Unit) {
        viewModel.loadAssetTypes()
        viewModel.loadInventorizationSessions()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAssetTypeId by remember { mutableIntStateOf(0) }

    InventorizationSessionsContent(
        sessions = inventorizationSessions,
        assetTypes = assetTypes,
        uiState = uiState,
        showCreateDialog = showCreateDialog,
        selectedAssetTypeId = selectedAssetTypeId,
        onSessionClick = { sessionId, isCompleted ->
            navController.navigate("inventorization_items/$sessionId/$isCompleted")
        },
        onShowCreateDialogChange = { showCreateDialog = it },
        onSelectedAssetTypeIdChange = { selectedAssetTypeId = it },
        onCreateSession = { assetTypeId ->
            viewModel.startInventorizationSession(assetTypeId)
            showCreateDialog = false
        },
        onRetry = { viewModel.loadInventorizationSessions() },
        onBackClick = { navController.popBackStack() }
    )
}

// ==================== CONTENT: UI + Preview ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsContent(
    sessions: List<InventorizationSessionDto>,
    assetTypes: List<AssetTypeDto>,
    uiState: AssetViewModel.InventorizationUiState,
    showCreateDialog: Boolean,
    selectedAssetTypeId: Int,
    onSessionClick: (sessionId: Int, isCompleted: Boolean) -> Unit,
    onShowCreateDialogChange: (Boolean) -> Unit,
    onSelectedAssetTypeIdChange: (Int) -> Unit,
    onCreateSession: (assetTypeId: Int) -> Unit,
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.InventorizationUiState.SessionsLoaded -> {
                if (sessions.isEmpty()) {
                    EmptySessionsState(modifier = Modifier.padding(paddingValues))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { session ->
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

    // ИСПРАВЛЕННЫЙ ДИАЛОГ
    if (showCreateDialog) {
        CreateInventorySessionDialog(
            assetTypes = assetTypes,
            selectedAssetTypeId = selectedAssetTypeId,
            onDismiss = { onShowCreateDialogChange(false) },
            onSelectedTypeChange = onSelectedAssetTypeIdChange,
            onCreate = onCreateSession
        )
    }
}

// ==================== ДИАЛОГ: Отдельный Composable ====================
@Composable
fun CreateInventorySessionDialog(
    assetTypes: List<AssetTypeDto>,
    selectedAssetTypeId: Int,
    onDismiss: () -> Unit,
    onSelectedTypeChange: (Int) -> Unit,
    onCreate: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AddBox, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Новая сессия инвентаризации") },
        text = {
            Column {
                Text("Выберите тип актива:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (assetTypes.isEmpty()) {
                    Text("Загрузка типов...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(assetTypes) { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectedTypeChange(type.assetTypeId) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAssetTypeId == type.assetTypeId,
                                    onClick = { onSelectedTypeChange(type.assetTypeId) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(type.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedAssetTypeId) },
                enabled = selectedAssetTypeId > 0 && assetTypes.isNotEmpty()
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
                    text = session.assetTypeName ?: "Тип #${session.assetTypeId}",
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
                    text = session.createdAt.take(10),
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
    MaterialTheme {
        Surface {
            InventorizationSessionsContent(
                sessions = listOf(
                    InventorizationSessionDto(1, 1, "Компьютеры", "computers", "in_progress", "2026-07-23T08:59:53.158615Z"),
                    InventorizationSessionDto(2, 7, "Сетевое оборудование", "network_equipment", "completed", "2026-07-20T14:30:00.000000Z")
                ),
                assetTypes = listOf(
                    AssetTypeDto(1, "Компьютеры", "computers", null, "2026-07-06T07:18:41.873769", null),
                    AssetTypeDto(7, "Сетевое оборудование", "network_equipment", null, "2026-07-06T07:21:39.334371", null)
                ),
                uiState = AssetViewModel.InventorizationUiState.SessionsLoaded(emptyList()),
                showCreateDialog = false,
                selectedAssetTypeId = 0,
                onSessionClick = { _, _ -> },
                onShowCreateDialogChange = {},
                onSelectedAssetTypeIdChange = {},
                onCreateSession = {},
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
            onCreate = {}
        )
    }
}