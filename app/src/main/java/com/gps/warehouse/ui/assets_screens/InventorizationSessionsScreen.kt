package com.gps.warehouse.ui.assets_screens.inventorization

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationSessionsScreen(
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assetTypes by viewModel.assetTypes.collectAsState()

    // УБРАЛИ viewModel.loadAssetTypes(), чтобы не ломать состояние экрана
    LaunchedEffect(Unit) {
        viewModel.loadInventorizationSessions()
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAssetTypeId by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Инвентаризация",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новая сессия")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.InventorizationSessionsLoaded -> {
                if (state.sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Нет активных сессий", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sessions) { session ->
                            SessionCard(
                                session = session,
                                onClick = {
                                    // Передаем флаг isCompleted в следующий экран
                                    val isCompleted = session.status == "completed"
                                    navController.navigate("inventorization_items/${session.sessionId}/$isCompleted")
                                }
                            )
                        }
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadInventorizationSessions() }) { Text("Повторить") }
                    }
                }
            }
            else -> {}
        }
    }

    // Диалог создания новой сессии (оставляем как был, он работает)
//    if (showCreateDialog) {
//        AlertDialog(
//            onDismissRequest = { showCreateDialog = false },
//            title = { Text("Новая сессия инвентаризации") },
//            text = {
//                // Здесь можно оставить заглушку или реальный список, если он нужен
//                Text("Функционал создания сессии через диалог требует доработки списка типов")
//
//            },
//            confirmButton = {
//                Button(onClick = { showCreateDialog = false }) { Text("Закрыть") }
//            }
//        )
//    }
    // Диалог создания новой сессии
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Новая сессия инвентаризации") },
            text = {
                Column {
                    Text("Выберите тип актива:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    assetTypes.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAssetTypeId = type.assetTypeId }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAssetTypeId == type.assetTypeId,
                                onClick = { selectedAssetTypeId = type.assetTypeId }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(type.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startInventorizationSession(selectedAssetTypeId)
                        showCreateDialog = false
                    },
                    enabled = selectedAssetTypeId > 0
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun SessionCard(
    session: InventorizationSessionDto,
    onClick: () -> Unit
) {
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
                // Используем assetTypeName прямо из DTO
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
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

// PREVIEW
//@Preview(showBackground = true, name = "Экран сессий (Загружено)")
//@Composable
//fun InventorizationSessionsContentPreview_Loaded() {
//    MaterialTheme {
//        Surface(modifier = Modifier.fillMaxSize()) {
//            InventorizationSessionsContent(
//                uiState = AssetViewModel.AssetUiState.InventorizationSessionsLoaded(
//                    sessions = listOf(
//                        InventorizationSessionDto(
//                            sessionId = 1,
//                            assetTypeId = 1,
//                            assetTypeName = "Asset Name",
//                            assetTypeEnName = "Asset En Name",
//                            status = "in_progress",
//                            createdAt = "2026-07-23T08:59:53.158615Z"
//                        ),
//                        InventorizationSessionDto(
//                            sessionId = 2,
//                            assetTypeId = 7,
//                            assetTypeName = "Asset Name",
//                            assetTypeEnName = "Asset En Name",
//                            status = "completed",
//                            createdAt = "2026-07-20T14:30:00.000000Z"
//                        )
//                    )
//                ),
//                assetTypes = listOf(
//                    com.gps.warehouse.data.remote.assets_dto.AssetTypeDto(
//                        assetTypeId = 1,
//                        name = "Компьютер",
//                        enName = "computer",
//                        createdBy = null,
//                        createdAt = "2026-07-06T07:18:41.873769",
//                        updatedAt = null
//                    ),
//                    com.gps.warehouse.data.remote.assets_dto.AssetTypeDto(
//                        assetTypeId = 7,
//                        name = "Сетевое оборудование",
//                        enName = "network_equipment",
//                        createdBy = null,
//                        createdAt = "2026-07-06T07:21:39.334371",
//                        updatedAt = null
//                    )
//                ),
//                onSessionClick = {},
//                onCreateSession = {},
//                onRetry = {},
//                onBackClick = {},
//                showCreateDialog = false,
//                onShowCreateDialogChange = {},
//                selectedAssetTypeId = 0,
//                onSelectedAssetTypeIdChange = {}
//            )
//        }
//    }
//}