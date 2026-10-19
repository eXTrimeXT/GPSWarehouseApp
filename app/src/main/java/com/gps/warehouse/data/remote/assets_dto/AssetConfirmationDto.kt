package com.gps.warehouse.data.remote.assets_dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Enum для статусов (аналог Literal в Python)
@Serializable
enum class AssetConfirmationStatus {
    @SerialName("pending") PENDING,
    @SerialName("confirmed") CONFIRMED,
    @SerialName("not_found") NOT_FOUND,
    @SerialName("damaged") DAMAGED
}

// CreateConfirmationRequest
@Serializable
data class CreateConfirmationRequestDto(
    @SerialName("asset_id") val assetId: Int? = null,
    @SerialName("material_id") val materialId: String? = null,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("comment") val comment: String? = null
) {
    // Валидатор
    init {
        require(assetId != null || materialId != null || employeeId != null) {
            "Необходимо указать assetId, materialId или employeeId"
        }
    }
}

// ConfirmAssetRequest
@Serializable
data class ConfirmAssetRequestDto(
    @SerialName("asset_id") val assetId: Int? = null,
    @SerialName("qr_data") val qrData: String? = null,
    @SerialName("status") val status: String,
    @SerialName("comment") val comment: String? = null
) {
    // Валидация: комментарий обязателен для определенных статусов
    init {
        if (status == "not_found" || status == "damaged") {
            require(!comment.isNullOrBlank()) {
                "Комментарий обязателен для статусов 'not_found' или 'damaged'"
            }
        }
    }
}

// AssetConfirmationResponse
@Serializable
data class AssetConfirmationResponseDto(
    @SerialName("id") val id: Int,
    @SerialName("asset_id") val assetId: Int,
    @SerialName("asset_name") val assetName: String? = null,
    @SerialName("employee_id") val employeeId: String,
    @SerialName("employee_full_name") val employeeFullName: String? = null,
    @SerialName("requested_by") val requestedBy: String? = null,
    @SerialName("status") val status: String,
    @SerialName("status_ru") val statusRu: String? = null,
    @SerialName("comment") val comment: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("confirmed_at") val confirmedAt: String? = null
)