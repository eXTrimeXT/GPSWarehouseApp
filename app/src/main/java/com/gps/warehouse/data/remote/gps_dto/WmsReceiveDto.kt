package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName

// Запрос на приемку материалов
data class WmsReceiveRequest(
    @SerializedName("token") val token: String,
    @SerializedName("materials_data") val materialsData: List<WmsReceiveItem>
)

// Элемент списка приемки
data class WmsReceiveItem(
    @SerializedName("mat_num_scan") val matNumScan: String,     // Отсканированный номер материала
    @SerializedName("mat_num_order") val matNumOrder: String,   // Номер заказа (передаётся один для всех)
    @SerializedName("mat_qty_scan") val matQtyScan: Int,        // Фактическое количество
    @SerializedName("check_quality") val checkQuality: Boolean, // Чекбокс "Качество"
//    @SerializedName("check_expi") val checkExpi: Boolean              // Чекбокс "Срок годности"
    @SerializedName("date_expi") val Expi: String               // Дата "Срок годности"
)

// Ответ сервера
data class WmsReceiveResponse(
    val status: String,  // "success" или "error"
    val message: String?
)

data class WmsResponseDto(
    @SerializedName("data") val data: List<WmsItemDto>,
    @SerializedName("page") val page: Int,
    @SerializedName("page_qty") val pageQty: Int,                   // Общее количество страниц
    @SerializedName("materials_count") val materialsCount: Int = 0  // Общее количество записей
)