package com.gps.warehouse.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject

/**
 * Результат декодирования скана для приемки WMS
 */
data class WmsReceiveScan(
    val matNumScan: String,      // Номер материала
    val matQtyScan: Int,         // Количество
    val matNumOrder: String,     // Номер заказа
    val matPosition: String      // Позиция заказа из SAP
)

data class WmsWriteOffScan(
    val matNumScan: String,      // Номер материала
)

/**
 * Декодирует Base64-строку в объект WmsScanResult
 * @param base64String Строка в формате Base64 с JSON внутри
 * @return WmsScanResult или null при ошибке
 */
fun decodeWmsReceiveScreen(base64String: String): WmsReceiveScan? {
    return try {
        // Декодируем Base64 в строку
        val jsonString = String(
            Base64.decode(base64String.trim(), Base64.NO_WRAP),
            Charsets.UTF_8
        )

        // Парсим JSON
        val json = JSONObject(jsonString)
        Log.d("decodeWmsReceiveScreen", json.toString())

        WmsReceiveScan(
            matNumScan = json.getString("mat_num_scan"),
            matQtyScan = json.optString("mat_qty_scan", "1").toIntOrNull() ?: 1,
            matNumOrder = json.getString("mat_num_order"),
            matPosition = json.getString("mat_position"),
        )
    } catch (e: Exception) {
        // Логирование для отладки
        Log.e("decodeWmsReceiveScreen", "Ошибка декодирования: $base64String", e)
        null
    }
}

fun decodeWmsWriteOffScreen(base64String: String): WmsWriteOffScan? {
    return try {
        // Декодируем Base64 в строку
        val jsonString = String(
            Base64.decode(base64String.trim(), Base64.NO_WRAP),
            Charsets.UTF_8
        )

        // Парсим JSON
        val json = JSONObject(jsonString)
        Log.d("decodeWmsWriteOffScreen", json.toString())

        WmsWriteOffScan(
            matNumScan = json.getString("mat_num_scan"),
        )
    } catch (e: Exception) {
        // Логирование для отладки
        Log.e("decodeWmsWriteOffScreen", "Ошибка декодирования: $base64String", e)
        null
    }
}

/**
 * Проверяет, является ли строка потенциально Base64-кодированным JSON
 */
fun isBase64EncodedJson(input: String): Boolean {
    return input.length > 20 && // Минимальная длина для полезного Base64
            input.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' } &&
            try {
                val decoded = String(Base64.decode(input.trim(), Base64.NO_WRAP), Charsets.UTF_8)
                decoded.trim().startsWith("{") && decoded.trim().endsWith("}")
            } catch (e: Exception) {
                false
            }
}