package com.gps.warehouse.ui.gps_screens.inventory

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.InventoryMaterialDto
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager

@Composable
fun InventoryCheckScreen(
    orderNumber: String,
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val honeywellHelper = remember { ScannerManager(context) }

    // Состояние выбранного материала для сверки
    var selectedMaterial by remember { mutableStateOf<InventoryMaterialDto?>(null) }
    // Состояние количества для ввода
    var inputQty by remember { mutableStateOf("") }

    // Состояние для диалога подтверждения завершения
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

    // Получаем статус активности заказа из ViewModel
    val isOrderActive = viewModel.isInventoryActive

    LaunchedEffect(orderNumber) {
        viewModel.loadInventoryMaterials(orderNumber)
    }

    // Слушаем сканер
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                // Если заказ завершен, игнорируем сканирование
                if (!isOrderActive) {
                    Toast.makeText(
                        context,
                        "Инвентаризация завершена. Изменения невозможны.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@collect
                }

                handleInventoryScan(
                    scannedData = scannedData,
                    uiState = uiState,
                    onMaterialFound = { material ->
                        selectedMaterial = material
                        inputQty = "" // Сбрасываем количество при новом сканировании
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        honeywellHelper.init()
        onDispose {
            honeywellHelper.release()
        }
    }

    // Диалог подтверждения завершения инвентаризации
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            icon = {
                Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Завершить инвентаризацию?") },
            text = {
                Text("Вы уверены, что хотите завершить инвентаризацию заказа $orderNumber? После этого редактирование будет невозможно.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishInventoryOrder(orderNumber)
                        showFinishConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    InventoryCheckContent(
        orderNumber = orderNumber,
        uiState = uiState,
        isOrderActive = isOrderActive,
        selectedMaterial = selectedMaterial,
        inputQty = inputQty,
        onQtyChange = { inputQty = it },
        onMaterialSelect = {
            selectedMaterial = it
        },
        onConfirmClick = {
            val mat = selectedMaterial
            if (mat != null) {
                val qty = inputQty.toIntOrNull() ?: 0
                if (qty >= 0) {
                    viewModel.checkInventoryMaterial(
                        material = mat.material,
                        order = orderNumber,
                        qty = qty
                    )
                    // Опционально: сбросить выбор после успешной отправки
                     selectedMaterial = null
                } else {
                    Toast.makeText(context, "Количество не может быть меньше 0", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Выберите материал для сверки", Toast.LENGTH_SHORT).show()
            }
        },
        onBackClick = { navController.popBackStack() },
        onFinishClick = {
            // Показываем диалог вместо немедленного вызова
            showFinishConfirmDialog = true
        },
        onRetryClick = { viewModel.loadInventoryMaterials(orderNumber) }
    )
}

private fun handleInventoryScan(
    scannedData: String,
    uiState: MainViewModel.UiState,
    onMaterialFound: (InventoryMaterialDto) -> Unit,
    onError: (String) -> Unit
) {
    // Попытка парсинга JSON из Base64 (если используется сложный QR)
    val materialArticle = BarcodeParser.parse(scannedData)?.material  ?: scannedData

    if (uiState is MainViewModel.UiState.InventoryMaterialsLoaded) {
        val materials = uiState.materials
        val foundMaterial = materials.find { it.material == materialArticle }

        if (foundMaterial != null) {
            onMaterialFound(foundMaterial)
        } else {
            onError("Материал $materialArticle не найден в списке")
        }
    } else {
        onError("Список материалов еще не загружен")
    }
}

@Composable
fun InventoryCheckContent(
    orderNumber: String,
    uiState: MainViewModel.UiState,
    isOrderActive: Boolean,
    selectedMaterial: InventoryMaterialDto?,
    inputQty: String,
    onQtyChange: (String) -> Unit,
    onMaterialSelect: (InventoryMaterialDto) -> Unit,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    // Состояние для поиска
    var searchQuery by remember { mutableStateOf("") }

    // Создаем состояние списка для управления прокруткой
    val listState = rememberLazyListState()

    // Когда меняется выбранный материал, прокручиваем к нему
    LaunchedEffect(selectedMaterial) {
        if (selectedMaterial != null && uiState is MainViewModel.UiState.InventoryMaterialsLoaded) {
            val index = uiState.materials.indexOfFirst { it.material == selectedMaterial.material }
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ActionBar с кнопкой завершения справа (только если заказ активен)
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Сверка: $orderNumber",
            actionButton = if (isOrderActive) {
                {
                    IconButton(onClick = onFinishClick) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Завершить инвентаризацию",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else null
        )

        // Панель поиска
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                singleLine = true
            )
        }

        // Индикатор статуса заказа (если завершен)
        if (!isOrderActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "Заказ завершен.\nРедактирование запрещено.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }

            is MainViewModel.UiState.InventoryMaterialsLoaded -> {
                val allMaterials = uiState.materials

                // Фильтрация списка
                val filteredMaterials = if (searchQuery.isBlank()) {
                    allMaterials
                } else {
                    val query = searchQuery.lowercase()
                    allMaterials.filter {
                        it.material.lowercase().contains(query) ||
                                (it.name?.lowercase()?.contains(query) ?: false)
                    }
                }

                if (filteredMaterials.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (allMaterials.isEmpty()) "Нет материалов" else "Ничего не найдено")
                            if (allMaterials.isNotEmpty() && searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Сбросить поиск")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
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
                            InventoryMaterialCard(
                                material = material,
                                isSelected = selectedMaterial?.material == material.material,
                                onClick = {
                                    if (isOrderActive) onMaterialSelect(material)
                                }
                            )
                        }
                    }
                }
            }

            is MainViewModel.UiState.InventoryFinished -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0, 150, 0, 170),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Инвентаризация завершена!",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0, 150, 0, 170)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetryClick) { Text("Обновить") }
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

        // Показываем ActionPanel ТОЛЬКО если заказ активен
        if (isOrderActive) {
            ActionPanel(
                selectedMaterial = selectedMaterial,
                inputQty = inputQty,
                onQtyChange = onQtyChange,
                onConfirmClick = onConfirmClick,
                isLoading = uiState is MainViewModel.UiState.Loading
            )
        }
    }
}

@Composable
fun InventoryMaterialCard(
    material: InventoryMaterialDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Логика цветов
    val plan = material.count.toIntOrNull() ?: 0
    val fact = material.countFact.toIntOrNull() ?: 0
    val isComplete = plan >= 0 && plan == fact

    // Цвет фона карточки
    val containerColor = when {
        material.isJustChecked -> Color.Green.copy(alpha = 0.2f) // Зеленый при успехе
        material.hasError -> Color.Red.copy(alpha = 0.2f)       // Красный при ошибке
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Артикул: ${material.material}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    // Галочка, если план равен факту
                    if (isComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Сверено",
                            tint = Color(0, 150, 0, 255),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Не сверено",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (material.name != null) {
                    Text(
                        "Наименование: ${material.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("План: ${material.count}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Факт: ${material.countFact}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Индикатор ошибки справа, если есть
            if (material.hasError) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Ошибка",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ActionPanel(
    selectedMaterial: InventoryMaterialDto?,
    inputQty: String,
    onQtyChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    isLoading: Boolean
) {
    // Создаем FocusRequester для поля количества
    val quantityFocusRequester = remember { FocusRequester() }

    // Если материал выбран, то переводим фокус на количество
    LaunchedEffect(selectedMaterial) {
        if (selectedMaterial != null) {
            quantityFocusRequester.requestFocus()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = selectedMaterial?.material ?: "Материал не выбран",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selectedMaterial != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputQty,
                    onValueChange = onQtyChange,
                    label = { Text("Количество") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).focusRequester(quantityFocusRequester),
                    enabled = selectedMaterial != null && !isLoading
                )

                Button(
                    onClick = onConfirmClick,
                    enabled = selectedMaterial != null && !isLoading,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Подтвердить")
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить")
                }
            }
        }
    }
}

// --- PREVIEWS ---
private val mockMaterials = listOf(
    InventoryMaterialDto(
        id = "1",
        material = "LA1203900151",
        name = "Принтер этикеток",
        count = "5",
        countFact = "2",
        EA = "шт",
        warehouse = "3051",
        numOrder = "GPS_INV_40"
    ),
    InventoryMaterialDto(
        id = "2",
        material = "LA0602600443",
        name = "СИГНАЛИЗАЦИОННАЯ ЛАМПА",
        count = "4",
        countFact = "0",
        EA = "шт",
        warehouse = "3051",
        numOrder = "GPS_INV_40"
    )
)

@Preview(showBackground = true, name = "Inventory Check - List Only")
@Composable
fun PreviewInventoryList() {
    MaterialTheme {
        Surface {
            InventoryCheckContent(
                orderNumber = "GPS_INV_40",
                uiState = MainViewModel.UiState.InventoryMaterialsLoaded(mockMaterials, "GPS_INV_40"),
                selectedMaterial = null,
                inputQty = "1",
                onQtyChange = {},
                onMaterialSelect = {},
                onConfirmClick = {},
                onBackClick = {},
                onRetryClick = {},
                isOrderActive = true,
                onFinishClick = {}
            )
        }
    }
}