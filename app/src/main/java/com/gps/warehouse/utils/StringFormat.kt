package com.gps.warehouse.utils

import android.util.Log
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.Instant

/**
 * Проверяет, что переданная дата-время отличается от текущего времени устройства менее чем на 1 минуту.
 * @return true, если разница < 60 секунд, false в противном случае или при ошибке парсинга.
 */
fun String.isRecentWithinOneMinute(): Boolean {
    return try {
        // Формат строки: "2026-07-22 14:15:50"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val parsedTime = LocalDateTime.parse(this, formatter)
        val currentTime = LocalDateTime.now()

        // Вычисляем абсолютную разницу во времени
        val duration = Duration.between(parsedTime, currentTime).abs()

        // Возвращаем true, если разница меньше 5 минут
        duration.seconds < 5 * 60
    } catch (e: DateTimeParseException) {
        // Если строка не соответствует формату, безопасно возвращаем false
        false
    } catch (e: Exception) {
        // Ловим любые другие непредвиденные ошибки
        false
    }
}

/**
 * Простой парсинг ISO-строки без java.time.
 * Работает с форматами: "2026-09-03T06:26:16.840853Z" или без "Z"
 */
fun String?.formatIsoToReadable(pattern: String = "dd.MM HH:mm"): String? {
    return this?.let { raw ->
        try {
            // Разбиваем по разделителю даты и времени
            val parts = raw.split("T")
            if (parts.size < 2) return raw.replace('T', ' ').take(16)

            val dateParts = parts[0].split("-")
            if (dateParts.size < 3) return raw

            val time = parts[1].take(5) // HH:MM (игнорируем секунды и миллисекунды)
            val (year, month, day) = dateParts

            when (pattern) {
                "dd.MM HH:mm" -> "$day.$month $time"
                "dd.MM.yyyy HH:mm" -> "$day.$month.$year $time"
                else -> "$day.$month $time"
            }
        } catch (e: Exception) {
            // Fallback: просто заменяем T на пробел и обрезаем до HH:MM
            raw.replace('T', ' ').take(16)
        }
    }
}