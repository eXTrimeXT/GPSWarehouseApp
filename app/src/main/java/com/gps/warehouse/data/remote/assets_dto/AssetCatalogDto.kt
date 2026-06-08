package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetCatalogDto(
    @SerializedName("catalog_id") val catalogId: Int,
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("owner_id") val ownerId: Int?,
    @SerializedName("warranty_end_date") val warrantyEndDate: String?,
    @SerializedName("created_at") val createdAt: String,
    val asset: AssetDto?,
    val owner: UserDto?,
    val creator: UserDto?
)