package com.gps.warehouse.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Универсальный диалог выбора даты.
 *
 * @param initialIsoDate ISO-дата ("2026-11-19") или null — если пусто, календарь откроется на текущей дате.
 * @param onDismiss Вызывается при отмене или закрытии диалога.
 * @param onDateSelected Возвращает выбранную дату как ISO-строку ("2026-11-19").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDatePickerDialog(
    initialIsoDate: String?,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    // Парсим ISO → millis безопасно: если строка пустая/битая — null, календарь откроется на "сегодня"
    val initialMillis = remember(initialIsoDate) {
        initialIsoDate
            ?.takeIf { it.isNotBlank() }
            ?.let { iso ->
                runCatching {
                    LocalDate.parse(iso)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val isoDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                    onDateSelected(isoDate)
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}