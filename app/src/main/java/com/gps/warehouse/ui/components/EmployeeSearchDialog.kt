package com.gps.warehouse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gps.warehouse.data.remote.assets_dto.EmployeeShortResponse
import com.gps.warehouse.data.remote.assets_dto.PaginatedResponse
import com.gps.warehouse.data.remote.assets_dto.UserType

// В EmployeeSearchDialog.kt

@Composable
fun EmployeeSearchDialog(
    userType: UserType,  // ✅ Новый параметр
    onDismiss: () -> Unit,
    onEmployeeSelected: (UserType, EmployeeShortResponse) -> Unit,  // ✅ Теперь принимает UserType
    onSearch: (employeeId: String?, searchDepartment: String?, page: Int) -> Unit,  // ✅ Добавляем page
    paginatedEmployees: PaginatedResponse<EmployeeShortResponse>?,  // ✅ Теперь PaginatedResponse
    isLoading: Boolean = false,
    currentPage: Int = 1
) {
    var employeeId by remember { mutableStateOf("") }
    var searchDepartment by remember { mutableStateOf("") }
    var currentDialogPage by remember { mutableStateOf(currentPage) }

    // Сброс страницы при изменении фильтров
    fun performSearch(page: Int = 1) {
        currentDialogPage = page
        onSearch(
            employeeId.ifEmpty { null },
            searchDepartment.ifEmpty { null },
            page
        )
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
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Фильтры
                OutlinedTextField(
                    value = employeeId,
                    onValueChange = { employeeId = it },
                    label = { Text("Таб. номер") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { performSearch() }) {
                            Icon(Icons.Default.Search, "Поиск")
                        }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    value = searchDepartment,
                    onValueChange = { searchDepartment = it },
                    label = { Text("Подразделение") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Список результатов
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (paginatedEmployees?.items.isNullOrEmpty()) {
                    Text("Сотрудники не найдены", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    Column {
                        // Заголовок списка
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Найдено: ${paginatedEmployees?.total ?: 0}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Стр. ${paginatedEmployees?.page ?: 1}/${paginatedEmployees?.totalPages ?: 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(paginatedEmployees?.items.orEmpty()) { employee ->
                                EmployeeSearchItem(
                                    employee = employee,
                                    userType = userType,  // ✅ Передаём тип
                                    onClick = {
                                        onEmployeeSelected(userType, employee)  // ✅ Вызываем с UserType
                                        onDismiss()  // ✅ Закрываем диалог после выбора
                                    }
                                )
                            }
                        }

                        // Пагинация
                        if ((paginatedEmployees?.totalPages ?: 1) > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                OutlinedButton(
                                    onClick = { performSearch(currentDialogPage - 1) },
                                    enabled = paginatedEmployees?.hasPrevious == true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Назад")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { performSearch(currentDialogPage + 1) },
                                    enabled = paginatedEmployees?.hasNext == true,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Вперёд")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
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
    userType: UserType,  // ✅ Новый параметр
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