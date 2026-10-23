package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName
import com.honeywell.Message
import kotlinx.serialization.Serializable

// Enum для статусов (аналог Literal в Python)
enum class AssetConfirmationStatus {
    @SerializedName("pending") PENDING,
    @SerializedName("confirmed") CONFIRMED,
    @SerializedName("not_found") NOT_FOUND,
    @SerializedName("damaged") DAMAGED
}

// CreateConfirmationRequest
data class CreateConfirmationRequestDto(
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("material_id") val materialId: String? = null,
    @SerializedName("employee_id") val employeeId: String? = null,
    @SerializedName("comment") val comment: String? = null
) {
    // Валидатор
    init {
        require(assetId != null || materialId != null || employeeId != null) {
            "Необходимо указать assetId, materialId или employeeId"
        }
    }
}

// Подтвердить
data class ConfirmAssetRequestDto(
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("serial_number") val serialNumber: String? = null, // Было qr_data, стало serial_number
    @SerializedName("inventory_id") val inventoryId: String? = null,   // Новое поле из схемы
    @SerializedName("status") val status: String,                      // "confirmed", "not_found", "damaged"
    @SerializedName("comment") val comment: String? = null
) {
    // Аналог @model_validator(mode='after')
    init {
        // Проверка: хотя бы один идентификатор должен быть указан
        require(assetId != null || serialNumber != null || inventoryId != null) {
            "Необходимо указать хотя бы один идентификатор: assetId, serialNumber или inventoryId"
        }

        // Проверка: комментарий обязателен для определенных статусов
        if (status == "not_found" || status == "damaged") {
            require(!comment.isNullOrBlank()) {
                "Комментарий обязателен для статусов 'not_found' или 'damaged'"
            }
        }
    }
}

// Ответ на подтверждение
data class ProcessConfirmationResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("asset_name") val assetName: String? = null,
    @SerializedName("inventory_id") val inventoryId: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("status") val status: String? = null
)

// Получение списка подтверждений
data class AssetConfirmationResponseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("asset_name") val assetName: String? = null,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("employee_full_name") val employeeFullName: String? = null,
    @SerializedName("requested_by") val requestedBy: String? = null,
    @SerializedName("requested_full_name") val requestedFullName: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("status_ru") val statusRu: String? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("confirmed_at") val confirmedAt: String? = null
)