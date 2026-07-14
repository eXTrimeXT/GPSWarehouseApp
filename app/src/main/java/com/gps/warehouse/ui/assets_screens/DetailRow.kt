package com.gps.warehouse.ui.assets_screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Строка детальной информации.
 * Всегда отображает label. Если value == null, выводится пустая строка "".
 */
@Composable
fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value ?: "",
            style = MaterialTheme.typography.bodyMedium,
            // Если значение пустое, делаем его чуть бледнее для визуального отличия, но текст остаётся ""
            color = if (value.isNullOrBlank())
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDetailRow(){
    DetailRow("label", "value")
}