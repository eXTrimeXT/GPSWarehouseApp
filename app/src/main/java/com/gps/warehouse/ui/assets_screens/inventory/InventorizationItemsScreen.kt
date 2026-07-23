package com.gps.warehouse.ui.assets_screens.inventory

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar

// ==================== SCREEN: Логика + Навигация ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationItemsScreen(
    sessionId: Int,
    isCompleted: Boolean,
    navController: NavHostController,
    viewModel: AssetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadInventorizationItems(sessionId)
    }

    var showCompleteDialog by remember { mutableStateOf(false) }

    InventorizationItemsContent(
        uiState = uiState,
        sessionId = sessionId,
        isCompleted = isCompleted,
        showCompleteDialog = showCompleteDialog,
        onCheckClick = { assetId -> viewModel.checkInventorizationItem(sessionId, assetId) },
        onShowCompleteDialogChange = { showCompleteDialog = it },
        onCompleteSession = {
            viewModel.completeInventorizationSession(sessionId)
            navController.popBackStack()
        },
        onDetailsClick = { assetId -> navController.navigate("asset_details/$assetId") },
        onRetry = { viewModel.loadInventorizationItems(sessionId) },
        onBackClick = { navController.popBackStack() }
    )
}

// ==================== CONTENT: UI + Preview ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorizationItemsContent(
    uiState: AssetViewModel.AssetUiState,
    sessionId: Int,
    isCompleted: Boolean,
    showCompleteDialog: Boolean,
    onCheckClick: (assetId: Int) -> Unit,
    onShowCompleteDialogChange: (Boolean) -> Unit,
    onCompleteSession: () -> Unit,
    onDetailsClick: (assetId: Int) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MyCustomActionBar(
                text = "Сессия #$sessionId",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.InventorizationItemsLoaded -> {
                val items = uiState.items
                val checkedCount = items.count { it.isChecked }
                val totalCount = items.size

                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Прогресс + кнопка завершения
                    ProgressCard(
                        checkedCount = checkedCount,
                        totalCount = totalCount,
                        isCompleted = isCompleted,
                        onCompleteClick = { onShowCompleteDialogChange(true) }
                    )

                    // Список элементов
                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Нет активов в этой сессии",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                InventoryItemCard(
                                    item = item,
                                    isCompleted = isCompleted,
                                    onCheckClick = { onCheckClick(item.assetId) },
                                    onDetailsClick = { onDetailsClick(item.assetId) }
                                )
                            }
                        }
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {}
        }
    }

    // Диалог подтверждения завершения
    if (showCompleteDialog && !isCompleted) {
        AlertDialog(
            onDismissRequest = { onShowCompleteDialogChange(false) },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Завершить инвентаризацию?") },
            text = { Text("Все активы проверены. Сессия будет отмечена как завершённая и изменена не будет.") },
            confirmButton = {
                Button(onClick = onCompleteSession) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { onShowCompleteDialogChange(false) }) { Text("Отмена") }
            }
        )
    }
}

// ==================== ВСПОМОГАТЕЛЬНЫЕ COMPOSABLES ====================
@Composable
private fun ProgressCard(
    checkedCount: Int,
    totalCount: Int,
    isCompleted: Boolean,
    onCompleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Прогресс", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "$checkedCount / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (checkedCount == totalCount && totalCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier.fillMaxWidth(),
//                    enabled = checkedCount == totalCount && totalCount > 0
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Завершить инвентаризацию")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Инвентаризация завершена",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventorizationItemDto,
    isCompleted: Boolean,
    onCheckClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (item.isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
//        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCompleted) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onCheckClick() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Актив #${item.assetId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.isChecked) "Проверен" else "Не проверен",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDetailsClick) {
                Icon(Icons.Default.Info, "Детали актива", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ==================== PREVIEWS ====================
@PreviewLightDark
@Composable
private fun InventorizationItemsContentPreview_Loaded() {
    MaterialTheme {
        Surface {
            InventorizationItemsContent(
                uiState = AssetViewModel.AssetUiState.InventorizationItemsLoaded(
                    sessionId = 1,
                    items = listOf(
                        InventorizationItemDto(
                            inventorizationId = 1,
                            sessionId = 1,
                            assetId = 1,
                            assetName = "",
                            assetInventoryId = "",
                            assetSerialNumber = "",
                            assetStatus = "",
                            isChecked = false
                        ),
                    ),
                ),
                sessionId = 42,
                isCompleted = false,
                showCompleteDialog = false,
                onCheckClick = {},
                onShowCompleteDialogChange = {},
                onCompleteSession = {},
                onDetailsClick = {},
                onRetry = {},
                onBackClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun InventoryItemCardPreview() {
    MaterialTheme {
        Surface {
            Column {
                InventoryItemCard(
                    item = InventorizationItemDto(
                        inventorizationId = 1,
                        sessionId = 1,
                        assetId = 1,
                        assetName = "",
                        assetInventoryId = "",
                        assetSerialNumber = "",
                        assetStatus = "",
                        isChecked = false
                    ),
                    isCompleted = false,
                    onCheckClick = {},
                    onDetailsClick = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                InventoryItemCard(
                    item = InventorizationItemDto(
                        inventorizationId = 1,
                        sessionId = 1,
                        assetId = 1,
                        assetName = "",
                        assetInventoryId = "",
                        assetSerialNumber = "",
                        assetStatus = "",
                        isChecked = false
                    ),
                    isCompleted = false,
                    onCheckClick = {},
                    onDetailsClick = {}
                )
            }
        }
    }
}