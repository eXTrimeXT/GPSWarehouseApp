package com.gps.warehouse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Универсальная панель поиска и фильтрации.
 * @param searchQuery Текущий текст поиска.
 * @param onSearchQueryChange Колбэк изменения текста поиска.
 * @param placeholder Текст подсказки в поле поиска.
 * @param isFiltersExpanded Состояние раскрытия блока фильтров.
 * @param onToggleFilters Колбэк переключения видимости фильтров.
 * @param filterContent Контент блока фильтров (чипсы, даты и т.д.).
 */
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    placeholder: String = "Поиск...",
    isFiltersExpanded: Boolean,
    onToggleFilters: () -> Unit,
    modifier: Modifier = Modifier,
    filterContent: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Верхняя строка: Поиск + Кнопка фильтров
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text(placeholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопка переключения фильтров
                FilterChip(
                    selected = isFiltersExpanded,
                    onClick = onToggleFilters,
                    label = { Text("Фильтры") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }

            // Анимированный блок с дополнительными фильтрами
            AnimatedVisibility(
                visible = isFiltersExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .verticalScroll(rememberScrollState()), // На случай если фильтров много
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterContent()
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SearchAndFilterBarPreview(){
    SearchAndFilterBar(
        searchQuery = "",
        onSearchQueryChange = {},
        isFiltersExpanded = true,
        onToggleFilters = {},
        modifier = Modifier,
        filterContent = {}
    )
}