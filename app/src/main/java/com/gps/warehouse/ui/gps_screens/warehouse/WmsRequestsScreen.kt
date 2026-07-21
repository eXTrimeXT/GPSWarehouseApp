package com.gps.warehouse.ui.gps_screens.warehouse

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.WmsRequestDto
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SearchAndFilterBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager

// ====================== 1. ЭКРАН (Владеет состоянием и ViewModel) ======================
@Composable
fun WmsRequestsScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Состояния фильтров
    var searchQuery by remember { mutableStateOf("") }
    var selectedStorage by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedIsIncoming by remember { mutableStateOf<String?>(null) }
    var isFiltersExpanded by remember { mutableStateOf(false) }

    // Состояние выбранного запроса для диалога
    var selectedRequest by remember { mutableStateOf<WmsRequestDto?>(null) }

    // === Состояния для диалога результата (успех/ошибка) ===
    var showResultDialog by remember { mutableStateOf(false) }
    var isResultSuccess by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    // Загрузка данных при входе
    LaunchedEffect(Unit) {
        viewModel.loadWmsRequests()
    }

    // === Обработка состояний успеха/ошибки от ViewModel ===
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            // Успешное действие с запросом
            is MainViewModel.UiState.WmsRequestAccepted -> {
                isResultSuccess = true
                resultMessage = state.message ?: "Запрос успешно принят"
                showResultDialog = true
                selectedRequest = null
                viewModel.resetWmsRequestActionState()
            }
            is MainViewModel.UiState.WmsRequestCancelled -> {
                isResultSuccess = true
                resultMessage = state.message ?: "Запрос отклонён"
                showResultDialog = true
                selectedRequest = null
                viewModel.resetWmsRequestActionState()
            }
            // Ошибка при действии с запросом
            is MainViewModel.UiState.Error -> {
                // Показываем ошибку только если мы ждали результат действия с запросом
                if (selectedRequest != null) {
                    isResultSuccess = false
                    // Очищаем сообщение от HTML-тегов, если они есть
                    resultMessage = state.message
                        .replace(Regex("<[^>]*>"), "") // Удаляем все HTML-теги
                        .trim()
                        ?: "Неизвестная ошибка"
                    showResultDialog = true
                    selectedRequest = null
                    viewModel.resetWmsRequestActionState()
                }
            }
            else -> {}
        }
    }

    // Сканер
    val honeywellHelper = remember { ScannerManager(context) }
    LaunchedEffect(Unit) {
        honeywellHelper.init()
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val materialCode = BarcodeParser.parse(scannedData)?.material ?: scannedData
                searchQuery = materialCode
                Toast.makeText(context, "Поиск: $materialCode", Toast.LENGTH_SHORT).show()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            honeywellHelper.release()
        }
    }

    // === ДИАЛОГ ДЕЙСТВИЙ С ЗАПРОСОМ ===
    if (selectedRequest != null) {
        WmsRequestActionDialog(
            request = selectedRequest!!,
            onDismiss = { selectedRequest = null },
            onConfirm = { actionType ->
                selectedRequest?.let { req ->
                    when (actionType) {
                        "cancel" -> viewModel.cancelWmsRequest(req.id)
                        "accept" -> viewModel.acceptWmsRequest(req.id)
                    }
                    // Диалог закроется после получения ответа от сервера
                }
            }
        )
    }

    // === ДИАЛОГ РЕЗУЛЬТАТА ОПЕРАЦИИ (Успех / Ошибка) ===
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {
                // Запрещаем закрытие по клику вне, чтобы пользователь явно нажал ОК
            },
            icon = {
                Icon(
                    if (isResultSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isResultSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    if (isResultSuccess) "Успешно!" else "Ошибка",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(resultMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultDialog = false
                        // После закрытия диалога перезагружаем список запросов
                        viewModel.loadWmsRequests()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Вызов чистого UI-компонента
    WmsRequestsContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStorage = selectedStorage,
        onStorageSelected = { selectedStorage = it },
        selectedStatus = selectedStatus,
        onStatusSelected = { selectedStatus = it },
        selectedIsIncoming = selectedIsIncoming,
        onIsIncomingSelected = { selectedIsIncoming = it },
        isFiltersExpanded = isFiltersExpanded,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        // Передаем выбранный запрос и колбэки
        selectedRequest = selectedRequest,
        onRequestClick = { selectedRequest = it },
        onDismissDialog = { selectedRequest = null },
        onConfirmDialog = { actionType ->
            selectedRequest?.let { req ->
                when (actionType) {
                    "cancel" -> viewModel.cancelWmsRequest(req.id)
                    "accept" -> viewModel.acceptWmsRequest(req.id)
                }
            }
        },
        onRetryClick = { viewModel.loadWmsRequests() },
        onBackClick = { navController.popBackStack() }
    )
}

// ====================== 2. ЧИСТЫЙ UI КОМПОНЕНТ ======================
@Composable
fun WmsRequestsContent(
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStorage: String?,
    onStorageSelected: (String?) -> Unit,
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
    selectedIsIncoming: String?,
    onIsIncomingSelected: (String?) -> Unit,
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    selectedRequest: WmsRequestDto?,
    onRequestClick: (WmsRequestDto) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: (String) -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(onBackClick = onBackClick, text = "Складские запросы")

        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isFiltersExpanded = isFiltersExpanded,
            onToggleFilters = onToggleFilters
        ) {
            // 1. Статус
            Text("Статус: ", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelected(null) },
                    label = { Text("Все") },
                    modifier = Modifier
                )
                FilterChip(
                    selected = selectedStatus == "1",
                    onClick = { onStatusSelected("1") },
                    label = { Text("В ожидании") },
                    modifier = Modifier
                )
                FilterChip(
                    selected = selectedStatus == "0",
                    onClick = { onStatusSelected("0") },
                    label = { Text("Выполненные") },
                    modifier = Modifier
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 2. Склад
            if (uiState is MainViewModel.UiState.WmsRequestsLoaded) {
                val storages =
                    uiState.requests.map { it.fromStorage.trim() }.filter { it.isNotEmpty() }
                        .distinct().sorted()
                if (storages.isNotEmpty()) {
                    Text("Склад отправления: ", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedStorage == null,
                            onClick = { onStorageSelected(null) },
                            label = { Text("Все склады") })
                        storages.forEach { storage ->
                            FilterChip(
                                selected = selectedStorage == storage,
                                onClick = { onStorageSelected(storage) },
                                label = { Text(storage) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 3. Тип запроса
            Text("Тип запроса: ", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedIsIncoming == null,
                    onClick = { onIsIncomingSelected(null) },
                    label = { Text("Все") },
                    modifier = Modifier
                )
                FilterChip(
                    selected = selectedIsIncoming == "0",
                    onClick = { onIsIncomingSelected("0") },
                    label = { Text("Исходящий") },
                    modifier = Modifier
                )
                FilterChip(
                    selected = selectedIsIncoming == "1",
                    onClick = { onIsIncomingSelected("1") },
                    label = { Text("Входящий") },
                    modifier = Modifier
                )
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> CustomLoadingView()
            is MainViewModel.UiState.Error -> ErrorStateView(message = uiState.message, onRetry = onRetryClick, modifier = Modifier.weight(1f))
            is MainViewModel.UiState.WmsRequestsLoaded -> {
                val allRequests = uiState.requests
                val filteredRequests = allRequests.filter { request ->
                    val statusMatch = selectedStatus == null || request.isActive == selectedStatus
                    val cleanStorage = request.fromStorage.trim()
                    val storageMatch = selectedStorage == null || cleanStorage == selectedStorage.trim()
                    val typeMatch = selectedIsIncoming == null || request.isIncoming == selectedIsIncoming
                    val query = searchQuery.lowercase()
                    val searchMatch = query.isEmpty() || request.material.lowercase().contains(query) || request.name.lowercase().contains(query)
                    statusMatch && storageMatch && typeMatch && searchMatch
                }

                if (filteredRequests.isEmpty()) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Нет запросов по фильтрам")
                            if (allRequests.isNotEmpty()) {
                                TextButton(onClick = { onSearchQueryChange(""); onStorageSelected(null); onStatusSelected(null); onIsIncomingSelected(null) }) {
                                    Text("Сбросить фильтры")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Найдено: ${filteredRequests.size} из ${allRequests.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
                        }
                        items(filteredRequests.reversed()) { request ->
                            // Разрешаем клик только если статус в ожидании
                            WmsRequestCard(request = request, onClick = { if (request.isActive == "1") onRequestClick(request) })
                        }
                    }
                }
            }
            else -> CustomLoadingView()
        }

        // Диалог действий (перенесён выше, но оставляем для совместимости)
        if (selectedRequest != null) {
            WmsRequestActionDialog(
                request = selectedRequest,
                onDismiss = onDismissDialog,
                onConfirm = onConfirmDialog
            )
        }
    }
}

// ====================== 3. КАРТОЧКА ЗАПРОСА ======================
@Composable
fun WmsRequestCard(request: WmsRequestDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.clickable(onClick = onClick)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.material,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = if (request.isActive == "1") Color(255, 120, 0, 200) else Color(0, 150, 0, 255),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (request.isActive == "1") "В ожидании" else "Выполнен",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = request.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Откуда: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(request.fromStorage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowRightAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("Куда: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(request.toStorage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Кол-во: ${request.qty}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp).wrapContentWidth()
                )
                Row {
                    Text(
                        text = if (request.isIncoming == "0") "Исходящий" else "Входящий",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (request.isIncoming == "0")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).wrapContentWidth()
                    )
                    Icon(
                        imageVector = if (request.isIncoming == "0") Icons.Default.NorthEast else Icons.Default.SouthWest,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ====================== 4. ДИАЛОГ ДЕЙСТВИЙ ======================
@Composable
fun WmsRequestActionDialog(
    request: WmsRequestDto,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        modifier = Modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (request.isIncoming == "1") "Входящий запрос" else "Исходящий запрос",
                fontWeight = FontWeight.SemiBold
            )
        },
        titleContentColor = if (request.isIncoming == "1") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Материал: ${request.material}", style = MaterialTheme.typography.bodyMedium)
                Text("Наименование: ${request.name}", style = MaterialTheme.typography.bodyMedium)
                Text("Количество: ${request.qty}", style = MaterialTheme.typography.bodyMedium)
                Text("Откуда: ${request.fromStorage} → Куда: ${request.toStorage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(
                    onClick = { onConfirm("cancel") },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Отклонить") }
                if (request.isIncoming == "1") {
                    Button(onClick = { onConfirm("accept") }) { Text("Принять") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

// ====================== 5. ПРЕВЬЮ ======================
@Preview(showBackground = true, showSystemUi = true, name = "WmsRequests - Фильтры активны", device = "spec:width=380dp,height=1400dp")
@Composable
fun WmsRequestsPreviewWithFilters() {
    val fakeRequests = listOf(
        WmsRequestDto(
            id = "33", fromId = "gw07015370", userAccept = null,
            material = "TEST-MAT", fromStorage = "BUFF", toStorage = "ARCHIVE",
            qty = "1", name = "Тестовый материал", isActive = "1",
            type = "stock", isIncoming = "1"
        ),
        WmsRequestDto(
            id = "34", fromId = "gw07015370", userAccept = null,
            material = "TEST-MAT", fromStorage = "BUFF", toStorage = "ARCHIVE",
            qty = "1", name = "Тестовый материал", isActive = "1",
            type = "stock", isIncoming = "0"
        )
    )
    MaterialTheme {
        Surface {
            WmsRequestsContent(
                uiState = MainViewModel.UiState.WmsRequestsLoaded(fakeRequests),
                searchQuery = "TEST",
                onSearchQueryChange = {},
                selectedStorage = null,
                onStorageSelected = {},
                selectedStatus = null,
                onStatusSelected = {},
                selectedIsIncoming = null,
                onIsIncomingSelected = {},
                isFiltersExpanded = true,
                onToggleFilters = {},
                selectedRequest = fakeRequests.first(),
                onRequestClick = {},
                onDismissDialog = {},
                onConfirmDialog = {},
                onRetryClick = {},
                onBackClick = {}
            )
        }
    }
}