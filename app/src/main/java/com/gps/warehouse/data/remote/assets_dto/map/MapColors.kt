package com.gps.warehouse.data.remote.assets_dto.map

import androidx.compose.ui.graphics.Color

fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)
        val colorInt = when (cleanHex.length) {
            6 -> (0xFF000000 or colorLong).toInt()
            8 -> colorLong.toInt()
            else -> 0xFF000000.toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        Color.Gray
    }
}