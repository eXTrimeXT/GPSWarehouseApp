package com.gps.warehouse.ui.gps_screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.InventoryOrderDto
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SearchAndFilterBar // Импортируем компонент

@Composable
fun InventoryListScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Состояния для фильтрации и поиска
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) } // null = все, "1" = активные, "0" = завершенные
    var warehouseFilter by remember { mutableStateOf<String?>(null) } // null = все склады

    // Состояние раскрытия шторки фильтров
    var isFiltersExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInventoryOrders()
    }

    InventoryListContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        statusFilter = statusFilter,
        onStatusFilterChange = { statusFilter = it },
        warehouseFilter = warehouseFilter,
        onWarehouseFilterChange = { warehouseFilter = it },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        onOrderClick = { orderNumber, warehouse, isActive ->
            viewModel.setInventoryContext(orderNumber, warehouse, isActive == "1")
            navController.navigate("inventory_check/$orderNumber")
        },
        onBackClick = { navController.popBackStack() },
        onRetryClick = { viewModel.loadInventoryOrders() }
    )
}

@Composable
fun InventoryListContent(
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: String?,
    onStatusFilterChange: (String?) -> Unit,
    warehouseFilter: String?,
    onWarehouseFilterChange: (String?) -> Unit,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onOrderClick: (String, String, String) -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(onBackClick = onBackClick, text = "Инвентаризация")

        // === ИСПОЛЬЗУЕМ SEARCHANDFILTERBAR ===
        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isFiltersExpanded = isFiltersExpanded,
            onToggleFilters = onToggleFilters
        ) {
            // Контент фильтров внутри шторки

            // 1. Фильтр по статусу
            Text("Статус:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("Все") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = statusFilter == "1",
                    onClick = { onStatusFilterChange("1") },
                    label = { Text("Активные") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = statusFilter == "0",
                    onClick = { onStatusFilterChange("0") },
                    label = { Text("Завершенные") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Фильтр по складу
            if (uiState is MainViewModel.UiState.InventoryOrdersLoaded) {
                val warehouses = uiState.orders.map { it.warehouse }.distinct().sorted()
                if (warehouses.isNotEmpty()) {
                    Text("Склад:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = warehouseFilter == null,
                            onClick = { onWarehouseFilterChange(null) },
                            label = { Text("Все") },
                            modifier = Modifier.weight(1f)
                        )
                        // Показываем первые 3-4 склада, остальные можно добавить в скролл если нужно
                        warehouses.take(4).forEach { wh ->
                            FilterChip(
                                selected = warehouseFilter == wh,
                                onClick = { onWarehouseFilterChange(wh) },
                                label = { Text(wh) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }

            is MainViewModel.UiState.InventoryOrdersLoaded -> {
                val allOrders = uiState.orders

                // Логика фильтрации
                val filteredOrders = allOrders.filter { order ->
                    // 1. Фильтр по поиску (номер заказа)
                    val searchMatch = searchQuery.isEmpty() ||
                            order.orderNumber.lowercase().contains(searchQuery.lowercase())

                    // 2. Фильтр по статусу
                    val statusMatch = statusFilter == null || order.isActive == statusFilter

                    // 3. Фильтр по складу
                    val warehouseMatch = warehouseFilter == null || order.warehouse == warehouseFilter

                    searchMatch && statusMatch && warehouseMatch
                }

                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Нет заказов по выбранным фильтрам")
                            if (allOrders.isNotEmpty()) {
                                TextButton(onClick = {
                                    onSearchQueryChange("")
                                    onStatusFilterChange(null)
                                    onWarehouseFilterChange(null)
                                }) {
                                    Text("Сбросить фильтры")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Найдено: ${filteredOrders.size} из ${allOrders.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        items(filteredOrders) { order ->
                            InventoryOrderCard(
                                order = order,
                                onClick = {
                                    onOrderClick(
                                        order.orderNumber,
                                        order.warehouse,
                                        order.isActive
                                    )
                                })
                        }
                    }
                }
            }

            is MainViewModel.UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetryClick,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> { CustomLoadingView() }
        }
    }
}

@Composable
fun InventoryOrderCard(order: InventoryOrderDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color =
                        if (order.isActive == "1") {
                            Color(0, 150, 0, 170)
                        } else {
                            Color(220, 0, 0, 190)
                        },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (order.isActive == "1") "Активен" else "Завершен",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Склад: ${order.warehouse}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "План: ${order.count}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Факт: ${order.countFact}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Создан: ${order.dateCreate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!order.dateFinish.isNullOrEmpty()) {
                Text(
                    text = "Завершен: ${order.dateFinish}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=380dp, height=680dp")
@Composable
fun InventoryPreviewLoaded() {
    val fakeOrders = listOf(
        InventoryOrderDto(
            id = "12",
            orderNumber = "INV-2026-001",
            warehouse = "Склад 3051",
            isActive = "1",
            count = "150",
            countFact = "45",
            dateCreate = "28.04.2026 09:00",
            dateFinish = "28.04.2026 09:00",
        ),
        InventoryOrderDto(
            id = "13",
            orderNumber = "INV-2026-002",
            warehouse = "Склад 4007",
            isActive = "0",
            count = "150",
            countFact = "150",
            dateCreate = "28.04.2026 09:00",
            dateFinish = "29.04.2026 09:00",
        )
    )
    MaterialTheme {
        Surface {
            InventoryListContent(
                uiState = MainViewModel.UiState.InventoryOrdersLoaded(fakeOrders),
                searchQuery = "",
                onSearchQueryChange = {},
                statusFilter = null,
                onStatusFilterChange = {},
                warehouseFilter = null,
                onWarehouseFilterChange = {},
                isFiltersExpanded = false,
                onToggleFilters = {},
                onOrderClick = { _, _, _ -> },
                onBackClick = {},
                onRetryClick = {}
            )
        }
    }
}