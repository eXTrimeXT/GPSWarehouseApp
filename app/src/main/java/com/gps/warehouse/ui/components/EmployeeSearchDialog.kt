package com.gps.warehouse.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gps.warehouse.data.remote.assets_dto.EmployeeShortResponse
import com.gps.warehouse.data.remote.assets_dto.PaginatedResponse
import com.gps.warehouse.data.remote.assets_dto.PositionResponse
import com.gps.warehouse.data.remote.assets_dto.UserType
import com.gps.warehouse.data.remote.assets_dto.WorkplaceResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.collections.emptyList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSearchDialog(
    userType: UserType,
    onDismiss: () -> Unit,
    onEmployeeSelected: (UserType, EmployeeShortResponse) -> Unit,
    onSearch: (searchName: String?, employeeId: String?, searchDepartment: String?, page: Int) -> Unit,
    paginatedEmployees: PaginatedResponse<EmployeeShortResponse>?,
    isLoading: Boolean = false,
    currentPage: Int = 1
) {
    var searchName by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var searchDepartment by remember { mutableStateOf("") }
    var currentDialogPage by remember { mutableIntStateOf(currentPage) }

    var showAdvancedFilters by remember { mutableStateOf(false) }

    // Сброс страницы при изменении фильтров
    fun performSearch(page: Int = 1) {
        currentDialogPage = page
        onSearch(
            searchName.ifEmpty { null },
            employeeId.ifEmpty { null },
            searchDepartment.ifEmpty { null },
            currentDialogPage
        )
    }

    LaunchedEffect(searchName, employeeId, searchDepartment) {
        performSearch()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (userType) {
                        UserType.USER -> Icons.Default.Person
                        UserType.RESPONSIBLE -> Icons.Default.VerifiedUser
                        UserType.SERVING -> Icons.Default.Build
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поиск сотрудника")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. ОСНОВНОЙ ПОИСК (Всегда виден)
                OutlinedTextField(
                    value = searchName,
                    onValueChange = { searchName = it },
                    label = { Text("ФИО сотрудника") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchName.isNotEmpty()) {
                            IconButton(onClick = { searchName = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. ПЕРЕКЛЮЧАТЕЛЬ РАСШИРЕННЫХ ФИЛЬТРОВ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedFilters = !showAdvancedFilters }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAdvancedFilters) "Скрыть фильтры" else "Расширенные фильтры",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (showAdvancedFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3. РАСКРЫВАЮЩИЕСЯ ПОЛЯ (с плавной анимацией)
                if (showAdvancedFilters) {
                    Column(
                        modifier = Modifier.animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = employeeId,
                            onValueChange = { employeeId = it },
                            label = { Text("Табельный номер") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = searchDepartment,
                            onValueChange = { searchDepartment = it },
                            label = { Text("Подразделение") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                // Список результатов
                if (isLoading) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SplashLoadingScreen(showTextLoading = false)
                    }
                } else if (paginatedEmployees?.items.isNullOrEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Сотрудники не найдены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column {
                        // Заголовок списка
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Найдено: ${paginatedEmployees.total}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Стр. ${paginatedEmployees.page}/${paginatedEmployees.totalPages}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(paginatedEmployees.items) { employee ->
                                EmployeeSearchItem(
                                    employee = employee,
                                    userType = userType,  // Передаём тип
                                    onClick = {
                                        onEmployeeSelected(userType, employee)  // Вызываем с UserType
                                        onDismiss()  // Закрываем диалог после выбора
                                    }
                                )
                            }
                        }

                        // Пагинация
                        if ((paginatedEmployees.totalPages) > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(
                                    onClick = { performSearch(currentDialogPage - 1) },
                                    enabled = paginatedEmployees.hasPrevious,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Назад")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { performSearch(currentDialogPage + 1) },
                                    enabled = paginatedEmployees.hasNext,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Вперёд")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } },
        modifier = Modifier.widthIn(max = 400.dp)
    )
}

@Composable
private fun EmployeeSearchItem(
    employee: EmployeeShortResponse,
    userType: UserType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Аватарка с инициалами
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = getEmployeeInitials(employee.fullNameRu ?: employee.employeeId),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.fullNameRu ?: employee.employeeId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                employee.position?.name?.let { position ->
                    Text(
                        text = position,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                employee.department?.shortName?.let { dept ->
                    Text(
                        text = dept,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Бейдж типа привязки
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (userType) {
                    UserType.USER -> MaterialTheme.colorScheme.secondaryContainer
                    UserType.RESPONSIBLE -> MaterialTheme.colorScheme.primaryContainer
                    UserType.SERVING -> MaterialTheme.colorScheme.tertiaryContainer
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = when (userType) {
                        UserType.USER -> "Польз."
                        UserType.RESPONSIBLE -> "Отв."
                        UserType.SERVING -> "Обсл."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (userType) {
                        UserType.USER -> MaterialTheme.colorScheme.onSecondaryContainer
                        UserType.RESPONSIBLE -> MaterialTheme.colorScheme.onPrimaryContainer
                        UserType.SERVING -> MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// Вспомогательная функция для инициалов
private fun getEmployeeInitials(fullName: String): String {
    return fullName
        .split(" ", "–", "-")
        .filter { it.isNotEmpty() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .takeIf { it.isNotEmpty() } ?: "?"
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, showSystemUi = false, name = "EmployeeSearchDialog (Данные загружены)")
@Composable
fun EmployeeSearchDialogPreview_Loaded() {
    // Мокаем должность и подразделение для реалистичности
    val mockPosition = PositionResponse(name = "Инженер-программист", nameEn = "Software Engineer")
    val mockDepartment = WorkplaceResponse(
        guid = "dept-1",
        name = "Департамент информационных технологий",
        nameEn = "IT Department",
        shortName = "ДИТ",
        creationDate = null,
        closureDate = null,
        parentGuid = null
    )

    // Создаем список тестовых сотрудников
    val mockEmployees = listOf(
        EmployeeShortResponse(
            guid = "emp-1",
            employeeId = "0000012345",
            fullNameRu = "Иванов Иван Иванович",
            fullNameEn = "Ivanov Ivan Ivanovich",
            email = "i.ivanov@hmmr.ru",
            phone = "+79001234567",
            comment = null,
            society = null,
            department = mockDepartment,
            division = null,
            group = null,
            position = mockPosition
        ),
        EmployeeShortResponse(
            guid = "emp-2",
            employeeId = "0000067890",
            fullNameRu = "Петрова Анна Сергеевна",
            fullNameEn = "Petrova Anna Sergeevna",
            email = "a.petrova@hmmr.ru",
            phone = "+79009876543",
            comment = "Удаленный сотрудник",
            society = null,
            department = mockDepartment,
            division = null,
            group = null,
            position = PositionResponse(name = "Системный аналитик", nameEn = "System Analyst")
        )
    )

    // Создаем объект пагинации
    val paginatedResponse = PaginatedResponse(
        items = mockEmployees,
        total = 2,
        page = 1,
        pageSize = 20,
        totalPages = 1,
        hasNext = false,
        hasPrevious = false
    )

    // Рендерим диалог
    MaterialTheme {
        Surface {
            EmployeeSearchDialog(
                userType = UserType.USER, // UserType.RESPONSIBLE или UserType.SERVING
                onDismiss = { },
                onEmployeeSelected = { type, employee -> },
                onSearch = { sName, empId, dept, page -> },
                paginatedEmployees = paginatedResponse,
                isLoading = false,
                currentPage = 1
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = false, name = "EmployeeSearchDialog (Загрузка)")
@Composable
fun EmployeeSearchDialogPreview_Loading() {
    MaterialTheme {
        Surface {
            EmployeeSearchDialog(
                userType = UserType.RESPONSIBLE,
                onDismiss = { },
                onEmployeeSelected = { type, employee -> },
                onSearch = { sName, empId, dept, page -> },
                paginatedEmployees = null, // Или пустой список
                isLoading = true,          // Включаем индикатор загрузки
                currentPage = 1
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = false, name = "EmployeeSearchDialog (Пусто)")
@Composable
fun EmployeeSearchDialogPreview_EmptyList() {
    MaterialTheme {
        Surface {
            EmployeeSearchDialog(
                userType = UserType.USER,
                onDismiss = { },
                onEmployeeSelected = { type, employee -> },
                onSearch = { sName, empId, dept, page -> },
                paginatedEmployees = null, // Или пустой список
                isLoading = false,
                currentPage = 1
            )
        }
    }
}