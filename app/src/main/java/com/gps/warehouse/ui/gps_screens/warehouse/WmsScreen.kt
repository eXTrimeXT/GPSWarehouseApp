package com.gps.warehouse.ui.gps_screens.warehouse

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.ui.components.SearchAndFilterBar // Импортируем универсальный компонент
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.utils.ScannerManager
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannedData

@Composable
fun WmsScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Состояния для фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedStorageFilterId by remember { mutableStateOf<String?>(null) }
    // Новый фильтр - показывать только с ненулевым количеством
    var showOnlyNonZeroQty by remember { mutableStateOf(false) }
    // Состояние раскрытия шторки фильтров
    var isFiltersExpanded by remember { mutableStateOf(false) }

    // Состояния для диалога перемещения
    var showMoveDialog by remember { mutableStateOf(false) }
    var itemToMove by remember { mutableStateOf<WmsItemDto?>(null) }
    var moveQty by remember { mutableStateOf("1") }
    var targetStorage by remember { mutableStateOf("") }

    // Локальное состояние для отображения успеха ВНУТРИ диалога
    var showDialogSuccess by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // Инициализация сканера
    val honeywellHelper = remember { ScannerManager(context) }

    // Инициализация сканера при открытии экрана
    LaunchedEffect(Unit) {
        viewModel.loadWmsData()
        viewModel.loadAvailableWarehouses()
        honeywellHelper.init()
    }

    // СЕРВЕРНЫЙ ПОИСК вместо локального
//    LaunchedEffect(Unit) {
//        honeywellHelper.barcodeFlow.collect { scannedData ->
//            if (scannedData.isNotEmpty()) {
//                val parsedData: ScannedData? = BarcodeParser.parse(scannedData)
//                val materialCode = parsedData?.material ?: scannedData.trim()
//
//                viewModel.searchWmsByMaterial(materialCode)
//
//                searchQuery = materialCode
//
//                // 2. Ищем материал в текущем загруженном списке
//                if (uiState is MainViewModel.UiState.WmsLoaded) {
//                    val allItems = (uiState as MainViewModel.UiState.WmsLoaded).items
//                    val foundItem = allItems.find { it.material.equals(materialCode, ignoreCase = true) }
//
//                    if (foundItem != null) {
//                        // Материал найден — сразу открываем диалог перемещения
//                        itemToMove = foundItem
//                        moveQty = if (parsedData != null && parsedData.qty > 0) {
//                            parsedData.qty.toString()
//                        } else {
//                            "1"
//                        }
//                        targetStorage = ""
//                        dialogError = null
//                        showDialogSuccess = false
//                        showMoveDialog = true
//
//                        // Опционально: вибрация или звук для подтверждения
//                        // vibrator.vibrate(50)
//                    } else {
//                        // Материал не найден в текущем списке
//                        Toast.makeText(
//                            context,
//                            "Материал $materialCode не найден в списке",
//                            Toast.LENGTH_SHORT
//                        ).show()
//
//                        // Опционально: выполнить серверный поиск как fallback
//                        // viewModel.searchWmsByMaterial(materialCode)
//                    }
//                }
//
//                // Опционально: показать Toast о начале поиска
//                Toast.makeText(context, "Поиск: $materialCode", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    // Добавляем переменную для отслеживания последнего сканированного кода
    var lastScannedCode by remember { mutableStateOf<String?>(null) }

    // Обработчик сканера (отправляет запрос на сервер)
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                val parsedData: ScannedData? = BarcodeParser.parse(scannedData)
                val materialCode = parsedData?.material ?: scannedData.trim()

                // Запоминаем код, чтобы потом проверить результат поиска
                lastScannedCode = materialCode

                // Запускаем серверный поиск (сбрасывает список на 1 страницу)
                viewModel.searchWmsByMaterial(materialCode)
            }
        }
    }

    // Реактивный обработчик: автооткрытие диалога при успешном поиске
    LaunchedEffect(uiState) {
        // Проверяем, есть ли активный скан и вернулся ли ответ от сервера
        if (lastScannedCode != null && uiState is MainViewModel.UiState.WmsLoaded) {
            // Ищем точное совпадение по артикулу
            val foundItem = (uiState as MainViewModel.UiState.WmsLoaded).items.find {
                it.material.equals(lastScannedCode, ignoreCase = true)
            }

            if (foundItem != null) {
                // Материал найден — автоматически открываем диалог
                itemToMove = foundItem
                moveQty = if (foundItem.qty > 0) foundItem.qty.toInt().toString() else ""
                targetStorage = ""
                dialogError = null
                showDialogSuccess = false
                showMoveDialog = true
            } else {
                // Материал не найден
                Toast.makeText(context, "Материал $lastScannedCode не найден на складе", Toast.LENGTH_LONG).show()
            }

            // Обязательно сбрасываем флаг, чтобы не срабатывало повторно при смене состояния
            lastScannedCode = null
        }

        // Логика диалога (успех/ошибка/загрузка)
        when (uiState) {
            is MainViewModel.UiState.WmsMoveSuccess -> {
                if (showMoveDialog) {
                    showDialogSuccess = true
                    dialogError = null
                }
            }
            is MainViewModel.UiState.Error -> {
                if (showMoveDialog) {
                    dialogError = (uiState as MainViewModel.UiState.Error).message
                    showDialogSuccess = false
                }
            }
            is MainViewModel.UiState.Loading -> {
                if (showMoveDialog && !showDialogSuccess) {
                    dialogError = null
                }
            }
            else -> {}
        }
    }

    // === LaunchedEffect ДЛЯ СЕРВЕРНОЙ ФИЛЬТРАЦИИ ===
    // При изменении поиска — перезагружаем данные с сервера
    LaunchedEffect(searchQuery) {
        viewModel.updateWmsFilters(
            storageId = selectedStorageFilterId,
            searchQuery = searchQuery,
            hideZeroQty = showOnlyNonZeroQty
        )
    }

    // При изменении фильтра склада
    LaunchedEffect(selectedStorageFilterId) {
        viewModel.updateWmsFilters(
            storageId = selectedStorageFilterId,
            searchQuery = searchQuery,
            hideZeroQty = showOnlyNonZeroQty
        )
    }

    // При изменении фильтра "только с остатком"
    LaunchedEffect(showOnlyNonZeroQty) {
        viewModel.updateWmsFilters(
            storageId = selectedStorageFilterId,
            searchQuery = searchQuery,
            hideZeroQty = showOnlyNonZeroQty
        )
    }

    // Освобождение ресурсов сканера при уходе с экрана
    DisposableEffect(Unit) {
        onDispose {
//            honeywellHelper.enableScanner(false)
            honeywellHelper.release()
        }
    }

    // Обработка состояний UI для управления диалогом
    LaunchedEffect(uiState) {
        when (uiState) {
            // 1. Успех перемещения
            is MainViewModel.UiState.WmsMoveSuccess -> {
                if (showMoveDialog) {
                    // Показываем экран успеха внутри диалога
                    showDialogSuccess = true
                    dialogError = null
                    // Больше нет delay(1500). Ждем нажатия кнопки ОК.
                }
            }

            // 2. Ошибка при перемещении
            is MainViewModel.UiState.Error -> {
                if (showMoveDialog) {
                    dialogError = (uiState as MainViewModel.UiState.Error).message
                    showDialogSuccess = false
                }
            }

            // 3. Загрузка (сброс ошибки перед новым запросом)
            is MainViewModel.UiState.Loading -> {
                if (showMoveDialog && !showDialogSuccess) {
                    dialogError = null
                }
            }

            else -> {}
        }
    }

    WmsContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedStorageFilterId = selectedStorageFilterId,
        onStorageFilterSelected = { selectedStorageFilterId = it },
        isFiltersExpanded = isFiltersExpanded,
        showDialogSuccess = showDialogSuccess,
        onToggleFilters = { isFiltersExpanded = !isFiltersExpanded },
        onBackClick = { navController.popBackStack() },
        onNavigateToRequests = { navController.navigate("wms_requests") },
        onRetryClick = { viewModel.loadWmsData() },
        onItemClick = { item ->
            itemToMove = item
            moveQty = "" // item.qty.toInt().toString()
            targetStorage = ""
            dialogError = null
            showDialogSuccess = false
            showMoveDialog = true
        },
        showMoveDialog = showMoveDialog,
        itemToMove = itemToMove,
        moveQty = moveQty,
        onMoveQtyChange = { moveQty = it },
        targetStorage = targetStorage,
        onTargetStorageChange = { targetStorage = it },
        dialogError = dialogError,
        onClearDialogError = { dialogError = null },
        onDismissMoveDialog = {
            // Не даем закрыть диалог, если показываем успех или идет загрузка
            if (!showDialogSuccess && uiState !is MainViewModel.UiState.Loading) {
                showMoveDialog = false
                itemToMove = null
                dialogError = null
            }
        },
        onConfirmMove = { item, qtyStr, toStorage ->
            val qty = qtyStr.toDoubleOrNull() ?: 0.0
            if (qty > 0 && toStorage.isNotEmpty()) {
                dialogError = null
                showDialogSuccess = false
                viewModel.moveWmsMaterial(item.material, item.storage, toStorage, qty.toInt())
            }
        },
        // Колбэк для кнопки ОК в окне успеха
        onSuccessAcknowledge = {
            showMoveDialog = false
            showDialogSuccess = false
            itemToMove = null
            moveQty = "1"
            targetStorage = ""

            Toast.makeText(context, "Перемещение успешно", Toast.LENGTH_SHORT).show()

            // Перезагружаем список материалов
            viewModel.loadWmsData()
        },
        // Передаем параметры нового фильтра
        showOnlyNonZeroQty = showOnlyNonZeroQty,
        onShowOnlyNonZeroQtyChange = { showOnlyNonZeroQty = it },
        onResetFilters = {
            searchQuery = ""
            selectedStorageFilterId = null
            showOnlyNonZeroQty = false
        },
        viewModel = viewModel
    )
}

@Composable
fun WmsContent(
    uiState: MainViewModel.UiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedStorageFilterId: String?,
    onStorageFilterSelected: (String?) -> Unit,
    isFiltersExpanded: Boolean,
    showDialogSuccess: Boolean,
    onToggleFilters: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onRetryClick: () -> Unit,
    onItemClick: (WmsItemDto) -> Unit,
    showMoveDialog: Boolean,
    itemToMove: WmsItemDto?,
    moveQty: String,
    onMoveQtyChange: (String) -> Unit,
    targetStorage: String,
    onTargetStorageChange: (String) -> Unit,
    dialogError: String?,
    onClearDialogError: () -> Unit,
    onDismissMoveDialog: () -> Unit,
    onConfirmMove: (WmsItemDto, String, String) -> Unit,
    onSuccessAcknowledge: () -> Unit,
    // Новые параметры для фильтра по количеству
    showOnlyNonZeroQty: Boolean,
    onShowOnlyNonZeroQtyChange: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    viewModel: MainViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Склады",
            actionButton = {
                IconButton(onClick = onNavigateToRequests) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Запросы", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        // Получаем список доступных складов из ViewModel
        val availableWarehouses by viewModel.availableWarehouses.collectAsState()

        // === ИСПОЛЬЗУЕМ SEARCHANDFILTERBAR ===
        SearchAndFilterBar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            isFiltersExpanded = isFiltersExpanded,
            onToggleFilters = onToggleFilters
        ) {
            // Фильтр по складу — используем список из профиля
            Text("Склад: ", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Чип "Все"
                FilterChip(
                    selected = selectedStorageFilterId == null,
                    onClick = { onStorageFilterSelected(null) },
                    label = { Text("Все") }
                )
                // Чипы из доступных складов профиля
                availableWarehouses.forEach { storageId ->
                    FilterChip(
                        selected = selectedStorageFilterId == storageId.id,
                        onClick = { onStorageFilterSelected(storageId.id) },
                        label = { Text(storageId.name) } // Можно отображать имя, если в профиле есть мапа ID -> Name
                    )
                }
            }

                // === НОВЫЙ ФИЛЬТР: Только с остатком ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Только с остатком",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Checkbox(
                        checked = showOnlyNonZeroQty,
                        onCheckedChange = onShowOnlyNonZeroQtyChange
                    )
                }

            // КНОПКА СБРОСА ФИЛЬТРОВ
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = {
                        // Сбрасываем локальные состояния (сработают LaunchedEffect в WmsScreen)
                        onResetFilters()

                        // Явно вызываем обновление с пустыми параметрами, чтобы избежать гонки запросов
                        viewModel.updateWmsFilters(
                            storageId = null,
                            searchQuery = "",
                            hideZeroQty = false
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Сбросить фильтры")
                }
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> {
                if (!showMoveDialog) CustomLoadingView()
            }

//            is MainViewModel.UiState.WmsLoaded -> {
//                val allItemsPage = uiState.items
//
//                // Состояние списка для отслеживания скролла
//                val lazyListState = rememberLazyListState()
//
//                // Автозагрузка при прокрутке к концу
//                LaunchedEffect(lazyListState) {
//                    snapshotFlow { lazyListState.layoutInfo }
//                        .collect { layoutInfo ->
//                            val totalItems = layoutInfo.totalItemsCount
//                            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index
//
//                            // Если дошли до конца и есть ещё страницы — загружаем
//                            if (lastVisibleItem != null &&
//                                lastVisibleItem >= totalItems - 1 &&
//                                !viewModel.isLoadingMore &&
//                                viewModel.hasMorePages) {
//                                viewModel.loadMoreWmsData()
//                            }
//                        }
//                }
//
//                LazyColumn(
//                    modifier = Modifier.weight(1f),
//                    contentPadding = PaddingValues(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(8.dp),
//                    state = lazyListState // Привязываем состояние
//                ) {
//                    item {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(
//                            text = "Загружено: ${allItemsPage.size} материалов",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.align(Alignment.End)
//                        )
//                    }
//
//                    items(allItemsPage.reversed()) { item ->
//                        WmsItemCard(item = item, onClick = { onItemClick(item) })
//                    }
//
//                    // Индикатор загрузки внизу
//                    if (viewModel.isLoadingMore) {
//                        item {
//                            Box(
//                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                            }
//                        }
//                    }
//                }
//            }
            // В WmsScreen.kt, внутри блока is MainViewModel.UiState.WmsLoaded:

            is MainViewModel.UiState.WmsLoaded -> {
                val allItems = uiState.items  // ✅ Все загруженные материалы (локально)

                // Состояние списка для отслеживания скролла
                val lazyListState = rememberLazyListState()

                // ✅ Автозагрузка при прокрутке к концу
                LaunchedEffect(lazyListState) {
                    snapshotFlow { lazyListState.layoutInfo }
                        .collect { layoutInfo ->
                            val totalItems = layoutInfo.totalItemsCount
                            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index

                            // Если дошли до конца и есть ещё страницы — загружаем
                            if (lastVisibleItem != null &&
                                lastVisibleItem >= totalItems - 1 &&  // ✅ -1, т.к. индекс с 0
                                !viewModel.isLoadingMore &&
                                viewModel.hasMorePages) {
                                viewModel.loadMoreWmsData()
                            }
                        }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = lazyListState
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // ✅ Текст: Загружено X / Y материалов
                        Text(
                            text = "Загружено: ${allItems.size} / ${viewModel.totalMaterials} материалов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    // items(allItems.reversed()) — отображаем все загруженные материалы
                    items(allItems.reversed()) { item ->
                        WmsItemCard(item = item, onClick = { onItemClick(item) })
                    }

                    // Индикатор загрузки внизу
                    if (viewModel.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Сообщение "Все загружено"
                    if (!viewModel.hasMorePages && viewModel.totalMaterials > 0) {
                        item {
                            Text(
                                text = "• Все материалы загружены •",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            is MainViewModel.UiState.Error -> {
                if (!showMoveDialog) {
                    ErrorStateView(message = uiState.message, onRetry = onRetryClick, modifier = Modifier.weight(1f))
                }
            }
            else -> {}
        }
    }

    if (showMoveDialog && itemToMove != null) {
        MoveMaterialDialog(
            itemToMove = itemToMove,
            moveQty = moveQty,
            targetStorage = targetStorage,
            isLoading = uiState is MainViewModel.UiState.Loading && !showDialogSuccess,
            isSuccess = showDialogSuccess, // Передаем флаг успеха
            errorMessage = dialogError,
            onDismissRequest = onDismissMoveDialog,
            onQtyChange = onMoveQtyChange,
            onTargetStorageChange = onTargetStorageChange,
            onClearError = onClearDialogError,
            onConfirmMove = {
                onConfirmMove(itemToMove, moveQty, targetStorage)
            },
            onSuccessAcknowledge = onSuccessAcknowledge // Передаем колбэк ОК
        )
    }
}

@Composable
fun WmsItemCard(item: WmsItemDto, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = item.material, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "Кол-во: ${item.qty}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Склад: ${item.storage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Позиция: ${item.position}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Диалог перемещения с поддержкой отображения успеха и кнопкой ОК.
 */
@Composable
fun MoveMaterialDialog(
    itemToMove: WmsItemDto,
    moveQty: String,
    targetStorage: String,
    isLoading: Boolean,
    isSuccess: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onQtyChange: (String) -> Unit,
    onTargetStorageChange: (String) -> Unit,
    onClearError: () -> Unit,
    onConfirmMove: () -> Unit,
    onSuccessAcknowledge: () -> Unit // Колбэк для кнопки ОК
) {
    AlertDialog(
        // Блокируем закрытие диалога, если идет загрузка или показан успех
        onDismissRequest = if (isLoading || isSuccess) { {} } else { onDismissRequest },

        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),

        icon = {
            if (isSuccess) {
                Icons.Default.CheckCircle.let {
                    Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                }
            } else {
                Icons.Default.SwapHoriz.let {
                    Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        },

        title = {
            Text(if (isSuccess) "Успешно!" else "Перемещение материала", style = MaterialTheme.typography.headlineSmall)
        },

        text = {
            if (isSuccess) {
                // Экран успеха
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
                    Text("Материал перемещен", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Со склада:", style = MaterialTheme.typography.labelSmall)
                                Text(itemToMove.storage, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("На склад:", style = MaterialTheme.typography.labelSmall)
                                Text(targetStorage, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Количество:", style = MaterialTheme.typography.labelSmall)
                                Text(moveQty, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Обычный экран ввода
                Column(modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // 1. Артикул + Позиция (в одну строку)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = itemToMove.material,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                // Позиция как компактный бейдж
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = itemToMove.position,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // 2. Наименование (занимает всю ширину, но компактно)
                            Text(
                                text = itemToMove.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3. Склад + Остаток (в одну строку, акцент на количестве)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Склад: ${itemToMove.storage}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${itemToMove.qty.toInt()} шт.",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetStorage,
                        onValueChange = onTargetStorageChange,
                        label = { Text("Целевой склад") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = targetStorage.isEmpty() && !isLoading
                    )

                    OutlinedTextField(
                        value = moveQty,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                val inputQty = newValue.toIntOrNull() ?: 0
                                val maxQty = itemToMove.qty.toInt()
                                if (inputQty <= maxQty || newValue.isEmpty()) onQtyChange(newValue)
                            }
                        },
                        label = { Text("Количество") },
                        supportingText = { Text("Макс: ${itemToMove.qty.toInt()}") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = (moveQty.toIntOrNull() ?: 0) > itemToMove.qty.toInt() && moveQty.isNotEmpty()
                    )

                    if (errorMessage != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(androidx.compose.material.icons.Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = onClearError) {
                                    Icon(Icons.Default.Clear, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выполняется перемещение...")
                        }
                    }
                }
            }
        },

        confirmButton = {
            if (isSuccess) {
                // Кнопка ОК для экрана успеха
                Button(
                    onClick = onSuccessAcknowledge,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("ОК")
                }
            } else {
                // Кнопка "Переместить" для обычного режима
                Button(
                    onClick = onConfirmMove,
                    enabled = !isLoading && targetStorage.isNotEmpty() && moveQty.isNotEmpty() && (moveQty.toIntOrNull() ?: 0) > 0 && (moveQty.toIntOrNull() ?: 0) <= itemToMove.qty.toInt(),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Переместить")
                }
            }
        },

        dismissButton = {
            if (!isSuccess) {
                TextButton(onClick = onDismissRequest, enabled = !isLoading, modifier = Modifier.height(48.dp)) {
                    Text("Отмена")
                }
            }
            // Если isSuccess == true, кнопка отмены скрыта, чтобы пользователь нажал ОК
        }
    )
}

// --- PREVIEWS ---
@Preview(showBackground = true, name = "WMS Screen - Loaded")
@Composable
fun WmsPreviewLoaded() {
    val fakeItems = listOf(
        WmsItemDto(
            id = 66,
            material = "LA0602600443",
            max = 2,
            min = 0,
            position = "BUFF",
            price = 150.0,
            qty = 4.0,
            sapA = 1,
            storage = "3051",
            storageId = 1,
            name = "СИГНАЛИЗАЦИОННАЯ ЛАМПА"
        ),
        WmsItemDto(
            id = 67,
            material = "LA0713000190",
            max = 5,
            min = 1,
            position = "A-01",
            price = 1200.0,
            qty = 10.0,
            sapA = 1,
            storage = "4007",
            storageId = 2,
            name = "Станция зарядки"
        )
    )
    MaterialTheme {
        Surface {
            WmsContent(
                uiState = MainViewModel.UiState.WmsLoaded(fakeItems),
                searchQuery = "",
                onSearchQueryChange = {},
                selectedStorageFilterId = null,
                onStorageFilterSelected = {},
                isFiltersExpanded = false,
                showDialogSuccess = true,
                onToggleFilters = {},
                onBackClick = {},
                onNavigateToRequests = {},
                onRetryClick = {},
                onItemClick = {},
                showMoveDialog = false,
                itemToMove = null,
                moveQty = "1",
                onMoveQtyChange = {},
                targetStorage = "",
                onTargetStorageChange = {},
                dialogError = null,
                onClearDialogError = {},
                onDismissMoveDialog = {},
                onConfirmMove = { _, _, _ -> },
                onSuccessAcknowledge = {},
                showOnlyNonZeroQty = true,
                onShowOnlyNonZeroQtyChange = {},
                onResetFilters = {},
                viewModel = hiltViewModel()
            )
        }
    }
}

@Preview(showBackground = true, name = "WMS Screen - Loaded")
@Composable
fun DialogPreview() {
    MoveMaterialDialog(
        itemToMove = WmsItemDto(
            id = 1,
            material = "material",
            max = 100,
            min = 1,
            position = "position",
            price = 211.0,
            qty = 222.0,
            sapA = 111,
            storage = "storage",
            storageId = 3,
            name = "name"
        ),
        moveQty = "String",
        targetStorage = "String",
        isLoading = false,
        isSuccess = false,
        errorMessage = "",
        onDismissRequest = {},
        onQtyChange = {},
        onTargetStorageChange = {},
        onClearError = {},
        onConfirmMove = {},
        onSuccessAcknowledge = {}
    )
}
