package com.gps.warehouse.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject

/**
 * Результат парсинга штрихкода/QR-кода.
 * @param material Артикул материала.
 * @param code Уникальный код упаковки (может быть пустым для простых штрихкодов).
 * @param qty Количество (по умолчанию 1, если не указано в QR).
 */
data class ScannedData(
    val material: String,
    val code: String = "",
    val qty: Int = 1
)

object BarcodeParser {

    private const val TAG = "BarcodeParser"

    /**
     * Парсит данные со сканера.
     * Поддерживает:
     * 1. JSON в Base64 (QR-код с данными {"material": "...", "code": "...", "qty": "..."})
     * 2. Plain text (обычный штрихкод артикула)
     *
     * @param scannedData Сырая строка со сканера.
     * @return ScannedData или null, если данные некорректны.
     */
    fun parse(scannedData: String): ScannedData? {
        return try {
            // Попытка декодировать Base64
            val decodedBytes = try {
                Base64.decode(scannedData, Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }

            if (decodedBytes != null) {
                try {
                    val jsonStr = String(decodedBytes, Charsets.UTF_8)
                    // Простая проверка, похоже ли это на JSON объект
                    if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                        val jsonObject = JSONObject(jsonStr)

                        val material = jsonObject.optString("material", "")
                        val code = jsonObject.optString("code", "")
                        val qtyStr = jsonObject.optString("qty", "1")
                        val qty = qtyStr.toIntOrNull() ?: 1

                        if (material.isNotEmpty()) {
                            Log.d(TAG, "Parsed QR JSON: mat=$material, code=$code, qty=$qty")
                            return ScannedData(material = material, code = code, qty = qty)
                        } else {
                            // Если в JSON нет поля material, считаем всю строку материалом (редкий кейс)
                            Log.w(TAG, "JSON has no 'material' field, treating whole string as material")
                            return ScannedData(material = scannedData, code = "", qty = 1)
                        }
                    } else {
                        // Декодировалось из Base64, но это не JSON.
                        // Скорее всего, это просто строка, которая случайно похожа на Base64, или специфичный формат.
                        // Обрабатываем как обычный штрихкод.
                        Log.d(TAG, "Decoded Base64 is not JSON, treating as plain barcode")
                        return ScannedData(material = scannedData, code = "", qty = 1)
                    }
                } catch (e: Exception) {
                    // Ошибка парсинга JSON. Считаем обычным штрихкодом.
                    Log.e(TAG, "JSON parsing error, fallback to plain barcode", e)
                    return ScannedData(material = scannedData, code = "", qty = 1)
                }
            } else {
                // Не Base64. Считаем обычным штрихкодом (Plain Text).
                Log.d(TAG, "Plain barcode scanned: $scannedData")
                return ScannedData(material = scannedData, code = "", qty = 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical parsing error", e)
            null
        }
    }
}