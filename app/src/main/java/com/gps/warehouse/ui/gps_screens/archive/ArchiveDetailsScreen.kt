package com.gps.warehouse.ui.gps_screens.archive

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.MaterialDto
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar

// Список доступных статусов для фильтрации (иконки)
private val materialStatusFilters = listOf(
    "all" to Icons.AutoMirrored.Filled.List,    // Все
    "new" to Icons.Default.AddCircle,           // Новые
    "done" to Icons.Default.CheckCircle,        // Подтвержденные
    "dispute" to Icons.Default.Error            // Спорные/Ошибки
)

@Composable
fun OrderDetailsScreen(
    orderNumber: String,
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Состояния для поиска и фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("all") }

    // Загружаем материалы заказа при открытии
    LaunchedEffect(orderNumber) {
        viewModel.loadMaterials(orderNumber)
    }

    OrderDetailsContent(
        orderNumber = orderNumber,
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStatus = selectedStatus,
        onStatusSelected = { selectedStatus = it },
        onBackClick = { navController.popBackStack() },
        onRetryClick = { viewModel.loadMaterials(orderNumber) }
    )
}

@Composable
fun OrderDetailsContent(
    orderNumber: String,
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(onBackClick = onBackClick, text = "Просмотр: $orderNumber")

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
                    label = { Text("Поиск по артикулу или имени") },
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
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }

            is MainViewModel.UiState.MaterialsLoaded -> {
                val allMaterials = uiState.materials

                // Логика фильтрации
                val filteredMaterials = allMaterials.filter { material ->
                    // Фильтрация по поиску (артикул или имя)
                    val query = searchQuery.lowercase()
                    val searchMatch = query.isEmpty() ||
                            material.material.lowercase().contains(query) ||
                            (material.name?.lowercase()?.contains(query) ?: false)
                    searchMatch
                }

                if (filteredMaterials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ничего не найдено")
                            if (allMaterials.isNotEmpty()) {
                                TextButton(onClick = {
                                    onSearchQueryChange("")
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
                                text = "Найдено: ${filteredMaterials.size} из ${allMaterials.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        items(filteredMaterials) { material ->
                            MaterialDetailCard(material = material)
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

// Блок статуса для материала
@Composable
fun RowHeaderAndStatusMaterial(material: MaterialDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = material.material,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        // Блок статуса с иконкой и цветом
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            val statusText = when (material.status) {
                "done" -> "Подтвержден"
                "dispute" -> "Не подтвержден"
                "new" -> "Новый"
                "sent" -> "Отправлен"
                else -> "В пути"
            }

            val statusColor = when (material.status) {
                "done" -> Color(0, 150, 0, 255)
                "dispute" -> Color.Red
                "new" -> Color.Black
                "sent" -> Color.Black
                else -> Color.Gray
            }

            val statusIcon = when (material.status) {
                "done" -> Icons.Default.CheckCircle
                "dispute" -> Icons.Default.Error
                "new" -> Icons.Default.AddCircle
                "sent" -> Icons.Default.PresentToAll
                else -> Icons.Default.QuestionMark
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun MaterialDetailCard(material: MaterialDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RowHeaderAndStatusMaterial(material)

            if (material.name != null) {
                Text(text = material.name, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "Количество: ${material.qty}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (!material.scannedCode.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Код упаковки: ${material.scannedCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============= Preview =============
@Preview(showBackground = true)
@Composable
fun PreviewOrderDetailsScreen() {
    OrderDetailsContent(
        orderNumber = "123",
        uiState = MainViewModel.UiState.MaterialsLoaded(
            listOf(
                MaterialDto(
                    id = "123",
                    material = "mat 1",
                    qty = "22",
                    name = "Door",
                    status = "new",
                    scannedCode = "i38fh3"
                ),
                MaterialDto(
                    id = "124",
                    material = "mat 2",
                    qty = "10",
                    name = "Window",
                    status = "done",
                    scannedCode = "x99zz"
                ),
                MaterialDto(
                    id = "125",
                    material = "mat 3",
                    qty = "5",
                    name = "Handle",
                    status = "dispute",
                    scannedCode = null
                ),
            )
        ),
        searchQuery = "",
        onSearchQueryChange = {},
        selectedStatus = "all",
        onStatusSelected = {},
        onBackClick = {},
        onRetryClick = {}
    )
}

// ============= Preview =============
@Preview(showBackground = true)
@Composable
fun PreviewOrderDetailsLoading() {
    OrderDetailsContent(
        orderNumber = "123",
        uiState = MainViewModel.UiState.Loading,
        searchQuery = "",
        onSearchQueryChange = {},
        selectedStatus = "all",
        onStatusSelected = {},
        onBackClick = {},
        onRetryClick = {}
    )
}