package com.gps.warehouse.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Унифицированный ActionBar с возможностью добавления кнопки действия справа.
 *
 * @param onBackClick Действие кнопки "Назад".
 * @param text Текст заголовка.
 * @param actionButton Опциональная кнопка действия справа (например, "Завершить", "Сохранить").
 */
@Composable
fun MyCustomActionBar(
    onBackClick: () -> Unit,
    text: String,
    actionButton: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f) // Занимает все свободное место, толкая actionButton вправо
        )

        // Если передана кнопка действия, отображаем её
        if (actionButton != null) {
            actionButton()
        }

        Spacer(modifier = Modifier.width(8.dp))
    }

    // Разделитель для визуальной четкости
    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
}

@Preview(showBackground = true)
@Composable
fun ActionBarPreview(){
    MyCustomActionBar({}, text = "Preview")
}