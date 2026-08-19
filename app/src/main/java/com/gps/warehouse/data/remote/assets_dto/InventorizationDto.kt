package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class InventorizationSessionDto(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("asset_type_id") val assetTypeId: Int,
    @SerializedName("asset_type_name") val assetTypeName: String,
    @SerializedName("asset_type_en_name") val assetTypeEnName: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class InventorizationItemDto(
    @SerializedName("inventorization_id") val inventorizationId: Int,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("serial_number") val serialNumber: String,
    @SerializedName("asset_name") val assetName: String,
    @SerializedName("is_checked") val isChecked: Boolean,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("quantity_fact") val quantityFact: Int?,

    )

data class InventorizationSessionCreateRequest(
    @SerializedName("asset_type_id") val assetTypeId: Int
)

data class CheckItemRequest(
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("quantity_fact") val quantityFact: Int? = null
)