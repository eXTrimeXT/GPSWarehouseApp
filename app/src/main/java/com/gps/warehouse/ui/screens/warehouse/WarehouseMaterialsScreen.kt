package com.gps.warehouse.ui.screens.warehouse

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.dto.WarehouseMaterialDto
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SearchAndFilterBar // Импортируем новый компонент
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WarehouseMaterialsScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Даты по умолчанию
    val calendar = Calendar.getInstance()
    val endDateDefault = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    calendar.add(Calendar.MONTH, -1)
    val startDateDefault = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    var startDate by remember { mutableStateOf(startDateDefault) }
    var endDate by remember { mutableStateOf(endDateDefault) }

    // Состояния для фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedStorage by remember { mutableStateOf<String?>(null) }

    // Состояние раскрытия фильтров
    var isFiltersExpanded by remember { mutableStateOf(false) }

    // Инициализация сканера
    val honeywellHelper = remember { ScannerManager(context) }

    LaunchedEffect(startDate, endDate) {
        val startApi = "$startDate'T'00:00:00.000'Z'"
        val endApi = "$endDate'T'23:59:59.999'Z'"
        viewModel.loadWarehouseMaterials(startDate = startApi, endDate = endApi)

        // Инициализация сканера при открытии экрана
        honeywellHelper.init(
//            onInitialized = { honeywellHelper.enableScanner(true) },
//            onError = { e -> Toast.makeText(context, "Ошибка сканера: ${e.message}", Toast.LENGTH_LONG).show() }
        )
    }

    // Обработка сканирования на экране Склада Деталей
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                // Парсим данные со сканера
                val materialCode = BarcodeParser.parse(scannedData)?.material ?: scannedData

                // Автоматически устанавливаем текст в поле поиска
                searchQuery = materialCode

                // Показываем уведомление, что поиск выполнен
                Toast.makeText(context, "Поиск по артикулу: $materialCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Освобождение ресурсов сканера при уходе с экрана
    DisposableEffect(Unit) {
        onDispose {
//            honeywellHelper.enableScanner(false)
            honeywellHelper.release()
        }
    }

    WarehouseMaterialsContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStorage = selectedStorage,
        onStorageSelected = { selectedStorage = it },
        startDate = startDate,
        onStartDateChange = { startDate = it },
        endDate = endDate,
        onEndDateChange = { endDate = it },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        onBackClick = { navController.popBackStack() },
        onRetry = {
            val startApi = "$startDate'T'00:00:00.000'Z'"
            val endApi = "$endDate'T'23:59:59.999'Z'"
            viewModel.loadWarehouseMaterials(startDate = startApi, endDate = endApi)
        }
    )
}

@Composable
fun WarehouseMaterialsContent(
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStorage: String?,
    onStorageSelected: (String?) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(onBackClick = onBackClick, text = "Склад деталей")

        // === ИСПОЛЬЗУЕМ НОВЫЙ КОМПОНЕНТ ===
        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isFiltersExpanded = isFiltersExpanded,
            onToggleFilters = onToggleFilters
        ) {
            // Выбор дат
            Text("Период:", style = MaterialTheme.typography.labelMedium)
            DateRangePickerCompact(
                startDate = startDate,
                onStartDateChange = onStartDateChange,
                endDate = endDate,
                onEndDateChange = onEndDateChange
            )

            // Фильтр по складу (если нужно оставить внутри шторки)
//            if (uiState is MainViewModel.UiState.WarehouseMaterialsLoaded) {
//                val storages = uiState.materials.mapNotNull { it.storage }.distinct().sorted()
//                if (storages.isNotEmpty()) {
//                    Spacer(Modifier.height(8.dp))
//                    Text("Склад:", style = MaterialTheme.typography.labelMedium)
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        FilterChip(
//                            selected = selectedStorage == null,
//                            onClick = { onStorageSelected(null) },
//                            label = { Text("Все") },
//                            modifier = Modifier.weight(1f)
//                        )
//                        storages.take(3).forEach { storage ->
//                            FilterChip(
//                                selected = selectedStorage == storage,
//                                onClick = { onStorageSelected(storage) },
//                                label = { Text(storage) },
//                                modifier = Modifier.weight(1f)
//                            )
//                        }
//                    }
//                }
//            }
        }

        // Основной контент (список материалов)
        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }
            is MainViewModel.UiState.WarehouseMaterialsLoaded -> {
                val allMaterials = uiState.materials

                // Фильтрация списка
                val filteredMaterials = allMaterials.filter { mat ->
                    val storageMatch = selectedStorage == null || mat.storage == selectedStorage
                    val query = searchQuery.lowercase()
                    val searchMatch = query.isEmpty() ||
                            mat.material.lowercase().contains(query) ||
                            (mat.name?.lowercase()?.contains(query) ?: false)
                    storageMatch && searchMatch
                }

                if (filteredMaterials.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Нет материалов по выбранным фильтрам")
                            if (allMaterials.isNotEmpty()) {
                                TextButton(onClick = {
                                    onSearchQueryChange("")
                                    onStorageSelected(null)
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

                        items(filteredMaterials) { mat ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = mat.material,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Склад: ${mat.storage ?: "-"}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = mat.name ?: "Без названия",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (!mat.model.isNullOrEmpty()) {
                                        Text(
                                            text = "Модель: ${mat.model}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        StatItem(label = "План", value = "${mat.qtyPlan ?: 0}")
                                        StatItem(label = "Упаковано", value = "${mat.qtyPacked ?: 0}")
                                        StatItem(label = "За смену", value = "${mat.qtyPackedShift ?: 0}")
                                        StatItem(label = "Отгружено", value = "${mat.qtyDispatched ?: 0}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is MainViewModel.UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }
            else -> { CustomLoadingView() }
        }
    }
}

/**
 * Компактный вариант выбора дат для использования внутри фильтров
 */
@Composable
fun DateRangePickerCompact(
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        onStartDateChange(sdf.format(java.util.Date(it)))
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        onEndDateChange(sdf.format(java.util.Date(it)))
                    }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = startDate,
            onValueChange = {},
            label = { Text("С") },
            modifier = Modifier.weight(1f),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showStartPicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Начало")
                }
            }
        )
        OutlinedTextField(
            value = endDate,
            onValueChange = {},
            label = { Text("По") },
            modifier = Modifier.weight(1f),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showEndPicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Конец")
                }
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, name = "Warehouse - Loaded")
@Composable
fun WarehousePreviewLoaded() {
    val fakeMaterials = listOf(
        WarehouseMaterialDto(
            material = "5532012XS56XA",
            name = "Заглушка отверстия",
            storage = "4007",
            model = "F7/F7x",
            qtyPacked = 26,
            qtyPackedShift = 0,
            qtyDispatched = 25,
            qtyPlan = 2
        ),
        WarehouseMaterialDto(
            material = "MAT-002",
            name = "Винт М5x20",
            storage = "B-05-1",
            model = null,
            qtyPacked = 500,
            qtyPackedShift = 100,
            qtyDispatched = 400,
            qtyPlan = 1000
        )
    )
    MaterialTheme {
        Surface {
            WarehouseMaterialsContent(
                uiState = MainViewModel.UiState.WarehouseMaterialsLoaded(fakeMaterials),
                searchQuery = "",
                onSearchQueryChange = {},
                selectedStorage = null,
                onStorageSelected = {},
                startDate = "2026-04-01",
                onStartDateChange = {},
                endDate = "2026-05-01",
                onEndDateChange = {},
                isFiltersExpanded = false,
                onToggleFilters = {},
                onBackClick = {},
                onRetry = {}
            )
        }
    }
}