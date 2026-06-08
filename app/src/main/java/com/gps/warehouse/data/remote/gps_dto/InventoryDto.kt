package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// --- Запрос списка заказов инвентаризации ---
data class GetInventoryOrdersRequest(
    val token: String,
    val type: String = "status" // или "archive"
)

// --- Ответ: Заказ инвентаризации ---
data class InventoryOrderDto(
    @SerializedName("is_active") val isActive: String, // "1" - активен
    val id: String,
    @SerializedName("num_order") val orderNumber: String, // Например GPS_INV_39
    val warehouse: String,
    val count: String, // Плановое количество
    @SerializedName("count_fact") val countFact: String, // Фактическое количество
    @SerializedName("date_create") val dateCreate: String,
    @SerializedName("date_finish") val dateFinish: String?
) : Serializable

// --- Запрос просмотра материалов в заказе ---
data class GetInventoryMaterialsRequest(
    val token: String,
    val order: String
)

// --- Ответ: Материал в заказе инвентаризации ---
data class InventoryMaterialDto(
    val id: String,
    val material: String,      // Артикул
    val name: String?,         // Наименование
    val count: String,         // Плановое количество
    @SerializedName("count_fact") val countFact: String, // Фактическое количество
    val EA: String?,           // Единица измерения (возможно)
    val warehouse: String?,    // Склад
    @SerializedName("num_order") val numOrder: String?,   // Номер заказа (дублируется)
    // Временное поле для UI подсветки успеха
    val isJustChecked: Boolean = false,
    // Временное поле для UI подсветки ошибки (опционально, можно обрабатывать через Snackbar)
    val hasError: Boolean = false
) : Serializable

// Ответ от API inv_mob_set (теперь это список объектов)
data class CheckInventoryApiResponse(
    val id: String,
    val material: String,
    @SerializedName("is_check") val isCheck: String // "1" - сверено, "0" - нет
)

// --- Запрос на сверку материала ---
data class CheckInventoryMaterialRequest(
    val token: String,
    val material: String,
    val order: String,
    val qty: Int
)

// Запрос на завершение инвентаризации
data class FinishInventoryRequest(
    val token: String,
    val warehouse: String,
    @SerializedName("date_create") val dateCreate: String, // Текущая дата/время
    val order: String
)

// Ответ на завершение (предположительно простой статус)
data class FinishInventoryResponse(
    @SerializedName("is_active") val isActive: String? = null
)