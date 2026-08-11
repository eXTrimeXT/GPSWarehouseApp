package com.gps.warehouse.ui.gps_screens.packaging

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.ui.components.CustomLoadingView
import com.gps.warehouse.ui.components.MyCustomActionBar
import com.gps.warehouse.utils.BarcodeParser
import com.gps.warehouse.utils.ScannerManager

// Реальный экран
@Composable
fun PackagingScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    var orderMaterials by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Состояние для диалога редактирования/добавления
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Состояние для диалога подтверждения удаления
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }

    // Поля диалога
    var dialogMaterial by remember { mutableStateOf("") }
    var dialogQty by remember { mutableStateOf("1") }

    // Инициализируем хелпер Honeywell AIDC
    val honeywellHelper = remember { ScannerManager(context) }

    // Слушаем поток сканера
    LaunchedEffect(Unit) {
        honeywellHelper.barcodeFlow.collect { scannedData ->
            if (scannedData.isNotEmpty()) {
                // Используем новый парсер
                val parsedData = BarcodeParser.parse(scannedData)

                if (parsedData != null) {
                    val material = parsedData.material
                    val qty = parsedData.qty

                    if (showDialog) {
                        // Если диалог открыт, заполняем ТОЛЬКО поле артикула
                        dialogMaterial = material
                    } else {
                        // Если диалог закрыт, добавляем в список
                        val newList = addOrUpdateMaterial(orderMaterials, material, qty)
                        orderMaterials = newList
                    }
                } else {
                    Toast.makeText(context, "Ошибка распознавания штрихкода", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Инициализация и управление сканером
    DisposableEffect(Unit) {
        honeywellHelper.init()

        onDispose {
            honeywellHelper.release()
            viewModel.resetStateToIdle()
        }
    }

    // Диалог редактирования/добавления
    if (showDialog) {
        EditMaterialDialog(
            material = dialogMaterial,
            qty = dialogQty,
            isEditing = editingIndex != null,
            onDismiss = { showDialog = false },
            onConfirm = { mat, q ->
                val qtyInt = q.toIntOrNull() ?: 1
                if (qtyInt >= 0 && mat.isNotBlank()) {
                    orderMaterials = if (editingIndex != null) {
                        orderMaterials.toMutableList().apply {
                            set(editingIndex!!, Pair(mat, qtyInt))
                        }
                    } else {
                        // Добавление: ищем и суммируем или добавляем новый
                        addOrUpdateMaterial(orderMaterials, mat, qtyInt)
                    }
                    showDialog = false
                }
            }
        )
    }

    if (deleteConfirmIndex != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmIndex = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Подтвердите удаление",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Вы действительно хотите удалить этот материал из списка?\nЭто действие нельзя отменить.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmIndex?.let { idx ->
                            orderMaterials = orderMaterials.toMutableList().apply { removeAt(idx) }
                            deleteConfirmIndex = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить", style = MaterialTheme.typography.titleSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmIndex = null }) {
                    Text("Отмена", style = MaterialTheme.typography.titleSmall)
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        )
    }

    // === ДИАЛОГ УСПЕХА СОЗДАНИЯ ЗАКАЗА ===
    if (uiState is MainViewModel.UiState.OrderCreatedAndReadyForReceive) {
        val successState = uiState as MainViewModel.UiState.OrderCreatedAndReadyForReceive

        AlertDialog(
            onDismissRequest = {
                // Запрещаем закрытие по клику вне области, чтобы пользователь явно нажал кнопку
            },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("Заказ успешно создан!")
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Номер заказа:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = successState.orderNumber,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Очищаем локальный список материалов
                        orderMaterials = emptyList()
                        // Сбрасываем состояние ViewModel (чтобы скрыть диалог и вернуть Idle)
                        viewModel.resetStateToIdle()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ок")
                }
            }
        )
    }

    PackagingScreenContent(
        orderMaterials = orderMaterials,
        onEditClick = { index ->
            val item = orderMaterials[index]
            dialogMaterial = item.first
            dialogQty = item.second.toString()
            editingIndex = index
            showDialog = true
        },
//        onRemoveMaterial = { index ->
//            val newList = orderMaterials.toMutableList()
//            newList.removeAt(index)
//            orderMaterials = newList
//        },
        onRequestDelete = { index ->
            deleteConfirmIndex = index
        },
        onAddManualClick = {
            dialogMaterial = ""
            dialogQty = "1"
            editingIndex = null
            showDialog = true
        },
        uiState = uiState,
        onCreateOrderClick = {
            if (orderMaterials.isNotEmpty()) {
                viewModel.createOrder(orderMaterials)
            } else {
                Toast.makeText(context, "Добавьте хотя бы один материал", Toast.LENGTH_SHORT).show()
            }
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
}

///**
//Вспомогательная функция для добавления или обновления материала в списке.
// */
//private fun addOrUpdateMaterial(
//    currentOrder: List<Pair<String, Int>>,
//    material: String,
//    qtyToAdd: Int
//): List<Pair<String, Int>> {
//    val existingIndex = currentOrder.indexOfFirst { it.first == material }
//    return if (existingIndex != -1) {
//        val mutableList = currentOrder.toMutableList()
//        val oldQty = mutableList[existingIndex].second
//        mutableList[existingIndex] = Pair(material, oldQty + qtyToAdd)
//        mutableList.toList()
//    } else {
//        currentOrder + Pair(material, qtyToAdd)
//    }
//}

/**
 * Добавляет материал или увеличивает количество, если он уже есть.
 * Используется ТОЛЬКО при добавлении нового материала (не при редактировании).
 */
private fun addOrUpdateMaterial(
    currentOrder: List<Pair<String, Int>>,
    material: String,
    qtyToAdd: Int
): List<Pair<String, Int>> {
    val existingIndex = currentOrder.indexOfFirst { it.first == material }
    return if (existingIndex != -1) {
        val mutableList = currentOrder.toMutableList()
        val oldQty = mutableList[existingIndex].second
        mutableList[existingIndex] = Pair(material, oldQty + qtyToAdd)
        mutableList.toList()
    } else {
        currentOrder + Pair(material, qtyToAdd)
    }
}

// 2. Чистый UI компонент
@Composable
fun PackagingScreenContent(
    orderMaterials: List<Pair<String, Int>>,
    onEditClick: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
    onAddManualClick: () -> Unit,
    uiState: MainViewModel.UiState,
    onCreateOrderClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Заголовок с кнопкой назад через кастомный ActionBar
        MyCustomActionBar(
            onBackClick = onBackClick,
            text = "Формирование заказа",
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

        // Панель действий внизу (всегда видна, если не Loading/Error/Success)
        // Для состояния Success она будет перекрыта диалогом, что нормально.
        if (uiState !is MainViewModel.UiState.OrderCreatedAndReadyForReceive &&
            uiState !is MainViewModel.UiState.Loading &&
            uiState !is MainViewModel.UiState.Error) {

            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = onCreateOrderClick,
                    enabled = orderMaterials.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Создать заказ (${orderMaterials.sumOf { it.second }} шт.)")
                }
            }
        }

        // Основной контент через when
        when (uiState) {
            is MainViewModel.UiState.Loading -> { CustomLoadingView() }

            is MainViewModel.UiState.Error -> {
                ErrorStateView(
                    message = uiState.message,
                    onRetry = null, // Пользователь может исправить данные и нажать "Создать" снова
                    modifier = Modifier.weight(1f)
                )
            }

            // Состояние OrderCreatedAndReadyForReceive теперь обрабатывается AlertDialog'ом выше,
            // поэтому здесь мы можем либо показать заглушку, либо оставить список видимым под диалогом.
            // Так как AlertDialog перекрывает экран, здесь можно ничего не делать или показать Loading,
            // пока диалог не отобразится. Но лучше просто оставить список как есть, он будет виден на фоне.
            is MainViewModel.UiState.OrderCreatedAndReadyForReceive -> {
                // Этот блок выполняется, но поверх него лежит AlertDialog.
                // Мы просто рендерим список, как обычно, чтобы он был виден на фоне полупрозрачного затемнения диалога (если оно есть)
                // Или можно показать CustomLoadingView(), но лучше оставить список.
                RenderMaterialsList(
                    orderMaterials = orderMaterials,
                    onEditClick = onEditClick,
                    onRequestDelete = onRequestDelete,
                    onAddManualClick = onAddManualClick
                )

                // Панель действий внизу блокируется или скрывается, так как процесс завершен
                // Но так как диалог перекрывает всё, это не критично.
            }

            else -> {
                // Idle или другие состояния - показываем список материалов
                RenderMaterialsList(
                    orderMaterials = orderMaterials,
                    onEditClick = onEditClick,
                    onRequestDelete = onRequestDelete,
                    onAddManualClick = onAddManualClick
                )
            }
        }
    }
}

@Composable
fun RenderMaterialsList(
    orderMaterials: List<Pair<String, Int>>,
    onEditClick: (Int) -> Unit,
    onRequestDelete: (Int) -> Unit,
    onAddManualClick: () -> Unit
) {
    if (orderMaterials.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Список пуст.\nОтсканируйте QR или добавьте вручную.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            items(orderMaterials) { (material, qty) ->
                // Находим индекс для корректной работы кнопок
                val index = orderMaterials.indexOfFirst { it.first == material && it.second == qty }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                material,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Количество: $qty", style = MaterialTheme.typography.bodyMedium)
                        }

                        // Кнопка редактирования
                        IconButton(onClick = { if (index != -1) onEditClick(index) }) {
                            Icon(Icons.Default.Edit, "Редактировать", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Кнопка удаления
                        IconButton(onClick = { if (index != -1) onRequestDelete(index) }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditMaterialDialog(
    material: String, // Приходит из родителя
    qty: String,      // Приходит из родителя
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    // Используем TextFieldValue для контроля курсора
    var materialFieldValue by remember {
        mutableStateOf(TextFieldValue(text = material, selection = TextRange(material.length)))
    }
    var qtyFieldValue by remember {
        mutableStateOf(TextFieldValue(text = qty, selection = TextRange(qty.length)))
    }
    val materialFocusRequester = remember { FocusRequester() }

    // Следим за изменением входящего параметра material (например, при сканировании)
    LaunchedEffect(material) {
        materialFieldValue = TextFieldValue(
            text = material,
            selection = TextRange(material.length) // Курсор в конец
        )
    }

    // Следим за изменением входящего параметра qty
    LaunchedEffect(qty) {
        qtyFieldValue = TextFieldValue(
            text = qty,
            selection = TextRange(qty.length)
        )
    }

    // Запрос фокуса при открытии диалога
    LaunchedEffect(isEditing) {
        materialFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Редактирование" else "Новый материал") },
        text = {
            Column {
                OutlinedTextField(
                    value = materialFieldValue,
                    onValueChange = { newValue ->
                        // Обновляем значение и принудительно ставим курсор в конец
                        materialFieldValue = newValue.copy(
                            selection = TextRange(newValue.text.length)
                        )
                    },
                    label = { Text("Артикул / Материал") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(materialFocusRequester), // Привязываем фокус к артикулу
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = qtyFieldValue,
                    onValueChange = { newValue ->
                        // Разрешаем ввод только цифр и ограничиваем длину до 3 знаков
                        if (newValue.text.all { it.isDigit() } && newValue.text.length <= 3) {
                            qtyFieldValue = newValue.copy(
                                selection = TextRange(newValue.text.length)
                            )
                        }
                    },
                    label = { Text("Количество (макс. 999)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                // Передаем чистый текст без информации о курсоре
                onConfirm(materialFieldValue.text, qtyFieldValue.text)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// --- ПРЕВЬЮ ---
@Preview(showBackground = true, name = "Packaging - Empty")
@Composable
fun PackagingPreviewEmpty() {
    PackagingScreenContent(
        orderMaterials = listOf(),
        uiState = MainViewModel.UiState.Idle,
        onEditClick = {},
        onRequestDelete = {},
        onAddManualClick = {},
        onCreateOrderClick = {},
        onBackClick = {}
    )
}

@Preview(showBackground = true, name = "Packaging - Success Dialog Overlay")
@Composable
fun PackagingPreviewSuccess() {
    // В превью мы не можем легко показать AlertDialog поверх другого контента без NavHost,
    // но этот превью показывает, как выглядит список под диалогом (если бы он был прозрачным)
    // Или просто проверяет рендеринг списка.
    PackagingScreenContent(
        orderMaterials = listOf(Pair("MAT-001", 5), Pair("2", 3)),
        uiState = MainViewModel.UiState.OrderCreatedAndReadyForReceive("GPS_ORDER_282"),
        onEditClick = {},
        onRequestDelete = {},
        onAddManualClick = {},
        onCreateOrderClick = {},
        onBackClick = {}
    )
}