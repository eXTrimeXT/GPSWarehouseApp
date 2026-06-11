package com.gps.warehouse.utils

import android.util.Base64
import android.util.Log
import org.json.JSONObject

/**
 * Универсальный результат сканирования.
 * Поддерживает оба формата: WMS (приемка) и Packaging (упаковка).
 */
data class ScannedData(
    val material: String,      // Артикул (material или mat_num_scan)
    val code: String = "",     // Уникальный код упаковки (только для Packaging)
    val qty: Int = 1,          // Количество (qty или mat_qty_scan)
    val orderNumber: String? = null // Номер заказа (только для WMS / mat_num_order)
)

object BarcodeParser {
    private const val TAG = "BarcodeParser"

    /**
     * Парсит данные со сканера.
     * Поддерживает:
     * 1. JSON в Base64 с WMS-полями: { "mat_num_scan": "...", "mat_qty_scan": "...", "mat_num_order": "..." }
     * 2. JSON в Base64 с Packaging-полями: { "material": "...", "qty": "...", "code": "..." }
     * 3. Plain text (обычный штрихкод артикула)
     *
     * @param scannedData Сырая строка со сканера.
     * @return ScannedData или null, если данные некорректны.
     */
    fun parse(scannedData: String): ScannedData? {
        return try {
            val trimmed = scannedData.trim()

            // Попытка декодировать Base64
            val decodedBytes = try {
                Base64.decode(trimmed, Base64.NO_WRAP)
            } catch (e: Exception) {
                null
            }

            if (decodedBytes != null) {
                try {
                    val jsonStr = String(decodedBytes, Charsets.UTF_8)

                    // Простая проверка, похоже ли это на JSON объект
                    if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                        val json = JSONObject(jsonStr)

                        // === ВАРИАНТ 1: WMS формат (приемка) ===
                        // Проверяем наличие специфичного ключа mat_num_scan
                        if (json.has("mat_num_scan")) {
                            val matScan = json.optString("mat_num_scan", "")
                            val matQty = json.optString("mat_qty_scan", "1").toIntOrNull() ?: 1
                            val matOrder = if (json.has("mat_num_order")) {
                                json.optString("mat_num_order", "").takeIf { it.isNotBlank() }
                            } else null

                            Log.d(TAG, "Parsed WMS JSON: mat=$matScan, qty=$matQty, order=$matOrder")
                            return ScannedData(
                                material = matScan,
                                qty = matQty,
                                orderNumber = matOrder
                            )
                        }

                        // === ВАРИАНТ 2: Packaging формат (упаковка) ===
                        // Проверяем наличие специфичного ключа material
                        if (json.has("material")) {
                            val material = json.optString("material", "")
                            val qtyStr = json.optString("qty", "1")
                            val qty = qtyStr.toIntOrNull() ?: 1
                            val code = json.optString("code", "")

                            Log.d(TAG, "Parsed Packaging JSON: mat=$material, qty=$qty, code=$code")
                            return ScannedData(
                                material = material,
                                code = code,
                                qty = qty
                            )
                        }

                        // Если ни один формат не подошел, но есть поле "material" — используем его как артикул
                        if (json.has("material")) {
                            val material = json.optString("material", "")
                            return ScannedData(material = material)
                        }
                    }

                    // Декодировалось из Base64, но это не JSON или не распознанный формат
                    Log.d(TAG, "Decoded Base64 is not recognized JSON, treating as plain barcode: $jsonStr")
                    return ScannedData(material = trimmed)

                } catch (e: Exception) {
                    // Ошибка парсинга JSON. Считаем обычным штрихкодом.
                    Log.e(TAG, "JSON parsing error, fallback to plain barcode", e)
                    return ScannedData(material = trimmed)
                }
            } else {
                // Не Base64. Считаем обычным штрихкодом (Plain Text).
                Log.d(TAG, "Plain barcode scanned: $trimmed")
                return ScannedData(material = trimmed)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical parsing error", e)
            null
        }
    }
}