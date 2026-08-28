package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

// Основной DTO одного уведомления
data class NotificationDto(
    @SerializedName("notification_id") val notificationId: Int,
    @SerializedName("employee_id") val employeeId: String?,
    @SerializedName("employee_full_name") val employeeFullName: String?,
    @SerializedName("asset_id") val assetId: Int?,
    @SerializedName("session_id") val sessionId: Int?,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("initiator_id") val initiatorId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("responded_at") val respondedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("asset_name") val assetName: String?,
    @SerializedName("asset_inventory_id") val assetInventoryId: String?,
    @SerializedName("initiator_full_name") val initiatorFullName: String?,
    @SerializedName("direction") val direction: String,
    @SerializedName("direction_ru") val directionRu: String,
    @SerializedName("event_type_ru") val eventTypeRu: String,
    @SerializedName("status_ru") val statusRu: String
)

// Новый DTO для обертки ответа API с пагинацией
data class NotificationResponseDto(
    val items: List<NotificationDto>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("has_previous") val hasPrevious: Boolean,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("unchecked_count") val uncheckedCount: Int,
    @SerializedName("checked_count") val checkedCount: Int,
    @SerializedName("declined_count") val declinedCount: Int,
    val source: String? = null
)