package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

// Enum для статусов (аналог Literal в Python)
@Serializable
enum class AssetConfirmationStatus {
    @SerializedName("pending") PENDING,
    @SerializedName("confirmed") CONFIRMED,
    @SerializedName("not_found") NOT_FOUND,
    @SerializedName("damaged") DAMAGED
}

// CreateConfirmationRequest
@Serializable
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

// ConfirmAssetRequest
@Serializable
data class ConfirmAssetRequestDto(
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("qr_data") val qrData: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("comment") val comment: String? = null
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