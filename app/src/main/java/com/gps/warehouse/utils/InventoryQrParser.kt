package com.gps.warehouse.utils

object InventoryQrParser {
    /**
     * Парсит QR-код формата: "ID&SerialNumber"
     * @param scannedData строка вида "123&SN-ABC-001"
     * @return SerialNumber или null если формат неверный
     */
    fun parseSerialNumber(scannedData: String): String? {
        return try {
            val parts = scannedData.split("&", limit = 2)
            if (parts.size == 2) {
                parts[1].trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}