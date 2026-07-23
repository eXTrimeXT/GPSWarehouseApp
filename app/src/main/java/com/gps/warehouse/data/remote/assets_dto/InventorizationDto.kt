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
    @SerializedName("asset_name") val assetName: String,
    @SerializedName("asset_inventory_id") val assetInventoryId: String,
    @SerializedName("asset_serial_number") val assetSerialNumber: String,
    @SerializedName("asset_status") val assetStatus: String?,
    @SerializedName("is_checked") val isChecked: Boolean
)

data class InventorizationSessionCreateRequest(
    @SerializedName("asset_type_id") val assetTypeId: Int
)

data class CheckItemRequest(
    @SerializedName("asset_id") val assetId: Int
)