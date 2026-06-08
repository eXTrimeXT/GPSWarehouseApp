package com.gps.warehouse.ui.gps_screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.OrderDto
import com.gps.warehouse.data.remote.gps_dto.Rules
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar
import kotlin.collections.filter

// Список доступных статусов для фильтрации (простой список пар)
private val statusFilters = listOf(
    "all" to Icons.AutoMirrored.Filled.List,    // Все
    "new" to Icons.Default.AddCircle,           // Новые
    "sent" to Icons.Default.PresentToAll,       // Отправленные
    "inway" to Icons.Default.AirportShuttle,    // В пути
)

@Composable
fun OrdersScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Состояние для поиска
    var searchQuery by remember { mutableStateOf("") }

    // Состояние для выбранного статуса фильтра (по умолчанию "all")
    var selectedStatus by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    OrdersContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStatus = selectedStatus,
        onStatusSelected = { selectedStatus = it },
        onOrderClick = { orderNumber ->
            navController.navigate("receive/$orderNumber")
        },
        onBackClick = { navController.popBackStack() },
        onRetryClick = { viewModel.loadOrders() }
    )
}

@Composable
fun OrdersContent(
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    onOrderClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(onBackClick = onBackClick, text = "Список заказов")

        // Панель фильтров и поиска
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Поле поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Поиск по заказу или SAP") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Горизонтальный ряд иконок-фильтров
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    statusFilters.forEach { (status, icon) ->
                        val isSelected = selectedStatus == status

                        // Определяем цвет иконки
                        val tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }

                        IconButton(
                            onClick = { onStatusSelected(status) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = status,
                                tint = tint,
                                modifier = Modifier.size(if (isSelected) 28.dp else 24.dp)
                            )
                        }
                    }
                }
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }

            is MainViewModel.UiState.OrdersLoaded -> {
                // Убираем статус Done и Dispute, потому что они должны быть в архиве
                val allOrders = uiState.orders.filter { order ->
                    val onlyWorkStatus = !(order.status == "done" || order.status == "dispute")
//                            && order.qty != null // также убираем заказы с количеством null
                    onlyWorkStatus
                }

                // Логика фильтрации
                val filteredOrders = allOrders.filter { order ->
                    // 1. Фильтрация по статусу
                    val statusMatch = if (selectedStatus == "all") {
                        true
                    } else {
                        order.status == selectedStatus
                    }

                    // 2. Фильтрация по поиску (номер заказа или SAP)
                    val query = searchQuery.lowercase()
                    val searchMatch = query.isEmpty() ||
                            order.orderNumber.lowercase().contains(query) ||
                            (order.sapOrder?.lowercase()?.contains(query) ?: false)
                    statusMatch && searchMatch
                }

                if (filteredOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ничего не найдено")
                            if (allOrders.isNotEmpty()) {
                                TextButton(onClick = {
                                    onSearchQueryChange("")
                                    onStatusSelected("all")
                                }) {
                                    Text("Сбросить фильтры")
                                }
                            }
                        }
                    }
                }
                else {
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
                            OrderCard(
                                order = order,
                                onClick = { onOrderClick(order.orderNumber) }
                            )
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

// --- PREVIEWS ---
@Preview(showBackground = true, name = "Orders - Loaded")
@Composable
fun OrdersPreviewLoaded() {
    val fakeOrders = listOf(
        OrderDto(
            id = "217",
            orderNumber = "GPS_ORDER_217",
            sapOrder = "SAP-12345",
            dispatch = null,
            invoice = null,
            doc = null,
            fiNum = null,
            dateCreate = "2026-04-20 15:38:13",
            dateSent = "2026-04-21 10:00:00",
            dateInWay = "2026-04-22 12:30:00",
            dateDone = "2026-04-23 09:15:00",
            status = "new",
            qty = "10",
            rules = Rules(ps = "1", pda = "1")
        ),
        OrderDto(
            id = "216",
            orderNumber = "GPS_ORDER_216",
            sapOrder = null,
            dispatch = null,
            invoice = null,
            doc = null,
            fiNum = null,
            dateCreate = "2026-04-06 15:45:06",
            dateSent = null,
            dateInWay = null,
            dateDone = "2026-04-07 18:00:00",
            status = "new",
            qty = "5",
            rules = Rules(ps = "1", pda = "1")
        ),
        OrderDto(
            id = "216",
            orderNumber = "GPS_ORDER_216",
            sapOrder = null,
            dispatch = null,
            invoice = null,
            doc = null,
            fiNum = null,
            dateCreate = "2026-04-06 15:45:06",
            dateSent = null,
            dateInWay = null,
            dateDone = "2026-04-07 18:00:00",
            status = "done",
            qty = "5",
            rules = Rules(ps = "1", pda = "1")
        ),
    )
    OrdersContent(
        uiState = MainViewModel.UiState.OrdersLoaded(fakeOrders),
        searchQuery = "",
        onSearchQueryChange = {},
        selectedStatus = "all",
        onStatusSelected = {},
        onOrderClick = {},
        onBackClick = {},
        onRetryClick = {}
    )
}