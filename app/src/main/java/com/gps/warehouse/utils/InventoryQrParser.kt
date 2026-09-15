package com.gps.warehouse.utils

//object InventoryQrParser {
//    /**
//     * Парсит QR-код формата: "ID&SerialNumber"
//     * @param scannedData строка вида "123&SN-ABC-001"
//     * @return SerialNumber или null если формат неверный
//     */
//    fun parseSerialNumber(scannedData: String): String? {
//        return try {
//            val parts = scannedData.split("&", limit = 2)
//            if (parts.size == 2) {
//                parts[1].trim().takeIf { it.isNotEmpty() }
//            } else {
//                null
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
//}

object InventoryQrParser {

    /**
     * Парсит QR-код и возвращает отсканированное значение.
     * Поддерживает форматы:
     * - "ID&SerialNumber" (старый формат) - возвращает SerialNumber
     * - "SerialNumber" (простой серийник)
     * - "INV-12345" (инвентарный номер)
     *
     * @param scannedData отсканированная строка
     * @return отсканированное значение или null если строка пустая
     */
    fun parseAny(scannedData: String): String? {
        if (scannedData.isBlank()) return null

        val trimmed = scannedData.trim()

        // Если содержит "&" - старый формат "ID&SerialNumber"
        if (trimmed.contains("&")) {
            val parts = trimmed.split("&", limit = 2)
            return if (parts.size == 2) {
                parts[1].trim().takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }

        // Иначе - просто возвращаем всю строку как отсканированное значение
        return trimmed.takeIf { it.isNotEmpty() }
    }

    /**
     * @deprecated Используйте parseAny() вместо этого метода
     */
    @Deprecated(
        "Use parseAny() instead",
        ReplaceWith("parseAny(scannedData)")
    )
    fun parseSerialNumber(scannedData: String): String? {
        return parseAny(scannedData)
    }
}