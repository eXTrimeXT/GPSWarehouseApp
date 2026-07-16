package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName

// Запрос на списание материалов
data class WmsWriteOffRequest(
    @SerializedName("token") val token: String,
    @SerializedName("materials_data") val materialsData: List<WmsWriteOffItem>
)

// Элемент списка списания
data class WmsWriteOffItem(
    @SerializedName("material") val material: String,           // Артикул (из скана)
    @SerializedName("qty") val qty: String,                     // Количество
    @SerializedName("storage") val storage: String,             // Склад
    @SerializedName("costcenter") val costcenter: String?,      // Cost center (опционально)
    @SerializedName("galaccount") val galaccount: String?,      // Счет ГЛ (опционально)
    @SerializedName("pos_text") val posText: String?,           // Текст позиции (опционально)
    @SerializedName("int_order") val intOrder: String?          // Внутренний заказ (опционально)
)

// Ответ сервера
data class WmsWriteOffResponse(
    val status: String,
    val message: String?
)