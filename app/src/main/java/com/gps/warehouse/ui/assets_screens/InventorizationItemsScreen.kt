package com.gps.warehouse.ui.assets_screens.inventorization

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.ui.AssetViewModel
import com.gps.warehouse.ui.components.MyCustomActionBar

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

    Scaffold(
        topBar = {
            MyCustomActionBar(
                text = "Сессия #$sessionId",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AssetViewModel.AssetUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AssetViewModel.AssetUiState.InventorizationItemsLoaded -> {
                val items = state.items
                val checkedCount = items.count { it.isChecked }
                val totalCount = items.size

                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // Прогресс-бар и кнопка завершения
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

                            // ПОКАЗЫВАЕМ КНОПКУ ТОЛЬКО ЕСЛИ НЕ ЗАВЕРШЕНО
                            if (!isCompleted) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCompleteDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = checkedCount == totalCount && totalCount > 0
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
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    // Список элементов
                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Нет активов в этой сессии", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    isCompleted = isCompleted, // <-- Передаем флаг
                                    onCheckClick = { viewModel.checkInventorizationItem(sessionId, item.assetId) },
                                    onDetailsClick = { navController.navigate("asset_details/${item.assetId}") }
                                )
                            }
                        }
                    }
                }
            }
            is AssetViewModel.AssetUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadInventorizationItems(sessionId) }) { Text("Повторить") }
                    }
                }
            }
            else -> {}
        }
    }

    // Диалог подтверждения завершения
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Завершить инвентаризацию?") },
            text = { Text("Все активы проверены. Сессия будет отмечена как завершённая и изменена не будет.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.completeInventorizationSession(sessionId)
                    showCompleteDialog = false
                    navController.popBackStack()
                }) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) { Text("Отмена") }
            }
        )
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
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ПОКАЗЫВАЕМ ЧЕКБОКС ТОЛЬКО ЕСЛИ НЕ ЗАВЕРШЕНО
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