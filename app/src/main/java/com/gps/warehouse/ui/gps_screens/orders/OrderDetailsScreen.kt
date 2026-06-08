package com.gps.warehouse.ui.gps_screens.orders

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gps.warehouse.data.remote.gps_dto.MaterialDto
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager
import kotlinx.coroutines.delay

@Composable
fun ReceiveMaterialsScreen(
    orderNumber: String,
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Состояние для поиска
    var searchQuery by remember { mutableStateOf("") }

    // Состояния для редактирования / добавления
    var editingMaterial by remember { mutableStateOf<MaterialDto?>(null) }
    var isNewMaterial by remember { mutableStateOf(false) }  // флаг: новый материал или редактирование
    var dialogQty by remember { mutableStateOf("1") }
    var showEditConfirmDialog by remember { mutableStateOf(false) }

    // Состояния для ручного добавления материала в заказ
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualMaterialArticle by remember { mutableStateOf("") }

    // Состояния для удаления
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var materialToDelete by remember { mutableStateOf<MaterialDto?>(null) }

    // Состояние для подсветки отсканированного материала
    var highlightedArticle by remember { mutableStateOf<String?>(null) }

    // Автоматически снимаем подсветку через 2 секунды
    LaunchedEffect(highlightedArticle) {
        if (highlightedArticle != null) {
            delay(2000)
            highlightedArticle = null
        }
    }

    // Инициализация сканера Honeywell
    val honeywellHelper = remember { ScannerManager(context) }

    LaunchedEffect(orderNumber) {
        viewModel.loadMaterials(orderNumber)
    }

    // Слушаем сканер
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                handleScanForReceive(
                    scannedData = scannedData,
                    viewModel = viewModel,
                    context = context,
                    currentMaterials = (uiState as? MainViewModel.UiState.MaterialsLoaded)?.materials ?: emptyList(),
                    onMaterialNotFound = { article ->
                        // Материал не найден → открываем диалог добавления
                        isNewMaterial = true
                        editingMaterial = MaterialDto(
                            id = "",  // будет заполнен сервером
                            material = article,
                            qty = "1",
                            name = null,
                            status = "new"
                        )
                        dialogQty = "1"
                        // Сначала ввод количества, потом подтверждение
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        honeywellHelper.init(
//            onInitialized = { honeywellHelper.enableScanner(true) },
//            onError = { e ->
//                Toast.makeText(context, "Ошибка сканера: ${e.message}", Toast.LENGTH_LONG).show()
//            }
        )
        onDispose {
//            honeywellHelper.enableScanner(false)
            honeywellHelper.release()
        }
    }

// === НОВОЕ: Диалог ручного ввода артикула ===
    if (showManualAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualAddDialog = false
                manualMaterialArticle = ""
            },
            title = { Text("Добавить материал") },
            text = {
                Column {
                    Text(
                        text = "Введите артикул материала или отсканируйте штрихкод",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = manualMaterialArticle,
                        onValueChange = { manualMaterialArticle = it },
                        label = { Text("Артикул материала") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                // Здесь можно добавить логику сканирования
                                Toast.makeText(context, "Отсканируйте штрихкод", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Сканировать")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualMaterialArticle.isNotBlank()) {
                            // Открываем диалог ввода количества
                            isNewMaterial = true
                            editingMaterial = MaterialDto(
                                id = "",
                                material = manualMaterialArticle.trim(),
                                qty = "1",
                                name = null,
                                status = "new"
                            )
                            dialogQty = "1"
                            showManualAddDialog = false
                        }
                    },
                    enabled = manualMaterialArticle.isNotBlank()
                ) {
                    Text("Далее")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualAddDialog = false
                    manualMaterialArticle = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог ВВОДА количества (для нового материала или редактирования)
    if (editingMaterial != null && !showEditConfirmDialog) {
        EditQtyDialog(
            qty = dialogQty,
            onDismiss = {
                editingMaterial = null
                isNewMaterial = false
            },
            onConfirm = { newQty ->
                if (newQty.toIntOrNull() != null && newQty.toInt() > 0) {
                    dialogQty = newQty
                    showEditConfirmDialog = true
                } else {
                    Toast.makeText(context, "Введите корректное число > 0", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
    }

    // Диалог ПОДТВЕРЖДЕНИЯ изменения / добавления
    if (showEditConfirmDialog && editingMaterial != null) {
        val actionText = if (isNewMaterial) "добавить" else "изменить"
        AlertDialog(
            onDismissRequest = {
                showEditConfirmDialog = false
                editingMaterial = null
                isNewMaterial = false
            },
            title = { Text("Подтвердите $actionText") },
            text = {
                if (isNewMaterial) {
                    Text("Добавить материал ${editingMaterial!!.material} в заказ в количестве ${dialogQty}?")
                } else {
                    Text("Изменить количество для ${editingMaterial!!.material} на ${dialogQty}?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (isNewMaterial) {
                        viewModel.addNewMaterialToOrder(
                            order = orderNumber,
                            material = editingMaterial!!.material,
                            qty = dialogQty.toIntOrNull() ?: 1,
                            onSuccess = {
                                showEditConfirmDialog = false
                                editingMaterial = null
                                isNewMaterial = false
                                Toast.makeText(context, "Материал добавлен", Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                Toast.makeText(context, "Ошибка: $msg", Toast.LENGTH_SHORT).show()
                                showEditConfirmDialog = false
                            }
                        )
                    } else {
                        viewModel.changeMaterialOnServer(
                            order = orderNumber,
                            material = editingMaterial!!.material,
                            qty = dialogQty.toIntOrNull() ?: 1,
                            idMat = editingMaterial!!.id,
                            onSuccess = {
                                showEditConfirmDialog = false
                                editingMaterial = null
                                Toast.makeText(context, "Количество обновлено", Toast.LENGTH_SHORT)
                                    .show()
                            },
                            onError = { msg ->
                                Toast.makeText(context, "Ошибка: $msg", Toast.LENGTH_SHORT).show()
                                showEditConfirmDialog = false
                            }
                        )
                    }
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditConfirmDialog = false
                    editingMaterial = null
                    isNewMaterial = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог ПОДТВЕРЖДЕНИЯ удаления
    if (showDeleteConfirmDialog && materialToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                materialToDelete = null
            },
            title = { Text("Подтвердите удаление") },
            text = { Text("Удалить материал ${materialToDelete!!.material} из заказа?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteMaterialFromServer(
                        order = orderNumber,
                        material = materialToDelete!!.material,
                        idMat = materialToDelete!!.id,
                        onSuccess = {
                            showDeleteConfirmDialog = false
                            materialToDelete = null
                            Toast.makeText(context, "Материал удален", Toast.LENGTH_SHORT).show()
                        },
                        onError = { msg ->
                            Toast.makeText(context, "Ошибка: $msg", Toast.LENGTH_SHORT).show()
                            showDeleteConfirmDialog = false
                        }
                    )
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    materialToDelete = null
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    ReceiveMaterialsContent(
        orderNumber = orderNumber,
        uiState = uiState,
        searchQuery = searchQuery,
        highlightedArticle = highlightedArticle,
        listState = listState,
        onSearchQueryChange = { searchQuery = it },
        onEditClick = { material ->
            isNewMaterial = false
            editingMaterial = material
            dialogQty = material.qty
        },
        onAddManualClick = {
            manualMaterialArticle = ""
            showManualAddDialog = true
        },
        onDeleteClick = { material ->
            materialToDelete = material
            showDeleteConfirmDialog = true
        },
        onRetry = {
            viewModel.loadMaterials(orderNumber)
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
}

/**
 * Обработка сканирования: поиск материала в списке или подготовка к добавлению нового
 */
private fun handleScanForReceive(
    scannedData: String,
    viewModel: MainViewModel,
    context: Context,
    currentMaterials: List<MaterialDto>,
//    onMaterialFound: (MaterialDto) -> Unit,
    onMaterialNotFound: (String) -> Unit
) {
    val parsedData = BarcodeParser.parse(scannedData)
    if (parsedData == null) {
        Toast.makeText(context, "Ошибка распознавания данных", Toast.LENGTH_SHORT).show()
        return
    }

    val article = parsedData.material.trim()
    if (article.isEmpty()) {
        Toast.makeText(context, "Неверный код: отсутствует артикул", Toast.LENGTH_SHORT).show()
        return
    }

    // Ищем материал в текущем списке
    val existing = currentMaterials.firstOrNull { it.material.equals(article, ignoreCase = true) }

    if (existing == null) {
    // Материал не найден → готовим к добавлению
    onMaterialNotFound(article)
//    Toast.makeText(context, "Материал не найден. Добавьте его в заказ.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ReceiveMaterialsContent(
    orderNumber: String,
    uiState: MainViewModel.UiState,
    searchQuery: String,
    highlightedArticle: String?,
    listState: LazyListState,
    onSearchQueryChange: (String) -> Unit,
    onEditClick: (MaterialDto) -> Unit,
    onAddManualClick: () -> Unit,
    onDeleteClick: (MaterialDto) -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Заказ: $orderNumber",
            actionButton = {
                IconButton(onClick = onAddManualClick) {
                    Icon(
                        Icons.Default.AddBox,
                        contentDescription = "Добавить вручную",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        )

        // Панель поиска
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Поиск...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
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

        when (uiState) {
            is MainViewModel.UiState.Loading -> CustomLoadingView()

            is MainViewModel.UiState.MaterialsLoaded -> {
                val allMaterials = uiState.materials

                // === ФИЛЬТРАЦИЯ: убираем элементы с null/пустыми обязательными полями ===
                val validMaterials = allMaterials.filter {
                    !it.material.isNullOrBlank() && !it.qty.isNullOrBlank()
                }

                val filteredMaterials = if (searchQuery.isBlank()) {
                    validMaterials
                } else {
                    val query = searchQuery.lowercase()
                    validMaterials.filter {
                        // Безопасный поиск: используем ?: "" для null-значений
                        it.material?.lowercase()?.contains(query) == true ||
                                (it.name?.lowercase()?.contains(query) ?: false)
                    }
                }

                if (filteredMaterials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (validMaterials.isEmpty())
                                    "В заказе нет материалов"  // Показываем, если вообще нет валидных материалов
                                else
                                    "Ничего не найдено по запросу \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (validMaterials.isEmpty()) {
                                Button(onClick = onRetry) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Обновить")
                                }
                            } else if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { onSearchQueryChange("") }) {
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
                                text = "Найдено: ${filteredMaterials.size} из ${validMaterials.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        items(filteredMaterials, key = { it.id }) { material ->
                            val isHighlighted = material.material == highlightedArticle
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = if (isHighlighted)
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                else
                                    null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // === БЕЗОПАСНОЕ ОТОБРАЖЕНИЕ С ?? ДЛЯ ВСЕХ ПОЛЕЙ ===
                                        Text(
                                            text = material.material ?: "—",  // Если null, покажем "—"
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!material.name.isNullOrBlank()) {
                                            Text(
                                                text = material.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        // qty может быть null — преобразуем в число безопасно
                                        val qtyValue = material.qty?.toIntOrNull() ?: 0
                                        Text(
                                            text = "Количество: $qtyValue",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (material.status == "new") {
                                        IconButton(onClick = { onEditClick(material) }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                "Изменить кол-во",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteClick(material) },
                                            colors = IconButtonDefaults.iconButtonColors()
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is MainViewModel.UiState.Packed -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Успешно: ${uiState.message}",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) { Text("Назад к списку") }
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

            else -> CustomLoadingView()
        }
    }
}

@Composable
fun EditQtyDialog(
    qty: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var qtyFieldValue by remember(qty) {
        mutableStateOf(TextFieldValue(text = qty, selection = TextRange(qty.length)))
    }
    val quantityFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { quantityFocusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Введите количество") },
        text = {
            OutlinedTextField(
                value = qtyFieldValue,
                onValueChange = { newValue ->
                    if (newValue.text.all { it.isDigit() } || newValue.text.isEmpty()) {
                        qtyFieldValue = newValue.copy(selection = TextRange(newValue.text.length))
                    }
                },
                label = { Text("Количество") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(quantityFocusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = qtyFieldValue.text.toIntOrNull()?.let { it <= 0 } == true
            )
            if (qtyFieldValue.text.toIntOrNull()?.let { it <= 0 } == true) {
                Text(
                    "Введите число больше 0",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qtyInt = qtyFieldValue.text.toIntOrNull()
                    if (qtyInt != null && qtyInt > 0) {
                        onConfirm(qtyFieldValue.text)
                    }
                },
                enabled = qtyFieldValue.text.toIntOrNull()?.let { it > 0 } == true
            ) {
                Text("Далее")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Preview(showBackground = true, name = "Receive - List")
@Composable
fun ReceivePreviewList() {
    val fakeMaterials = listOf(
        MaterialDto(id = "1", material = "MAT-001", qty = "5", name = "Деталь А", status = "new"),
        MaterialDto(id = "2", material = "MAT-002", qty = "10", name = "Деталь Б", status = "new")
    )
    ReceiveMaterialsContent(
        orderNumber = "GPS_ORDER_123",
        uiState = MainViewModel.UiState.MaterialsLoaded(fakeMaterials),
        searchQuery = "",
        highlightedArticle = "MAT-001",
        listState = rememberLazyListState(),
        onSearchQueryChange = {},
        onEditClick = {},
        onAddManualClick = {},
        onDeleteClick = {},
        onRetry = {},
        onBackClick = {}
    )
}

@Preview(showBackground = true, name = "Receive - Empty")
@Composable
fun ReceivePreviewEmpty() {
    ReceiveMaterialsContent(
        orderNumber = "GPS_ORDER_123",
        uiState = MainViewModel.UiState.MaterialsLoaded(emptyList()),
        searchQuery = "",
        highlightedArticle = null,
        listState = rememberLazyListState(),
        onSearchQueryChange = {},
        onEditClick = {},
        onAddManualClick = {},
        onDeleteClick = {},
        onRetry = {},
        onBackClick = {}
    )
}