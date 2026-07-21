package com.gps.warehouse.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Универсальный компонент для отображения состояния ошибки.
 * @param message Текст ошибки.
 * @param onRetry Лямбда-выражение, вызываемое при нажатии кнопки "Повторить" или автоматически.
 * @param autoRetry Если true, попытка повторится автоматически через 10 секунд.
 * @param modifier Модификаторы для внешнего контейнера.
 */
@Composable
fun ErrorStateView(
    message: String,
    onRetry: (() -> Unit)? = null,
    autoRetry: Boolean = true, // Новый параметр
    modifier: Modifier = Modifier
) {
    // Количество секунд для повтора
    val secondsDelay = 10
    // Состояние для обратного отсчета
    var secondsLeft by remember { mutableIntStateOf(secondsDelay) }

    // Единый эффект для управления таймером и авто-повтором
    LaunchedEffect(message) {
        // Сбрасываем таймер при каждом новом сообщении об ошибке
        secondsLeft = secondsDelay

        // Отсчет времени
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }

        // Если включен авто-повтор и время вышло, выполняем действие
        if (autoRetry && onRetry != null) {
            onRetry()
        }
    }


    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Произошла ошибка",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.replace(Regex("<[^>]*>"), "").trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Показываем кнопку, если передана onRetry.
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text("Повторить попытку")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrevErrorState() {
    ErrorStateView(
        message = "Ошибка соединения с сервером",
        onRetry = {},
        autoRetry = true
    )
}