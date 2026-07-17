package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetTypeDto(
    @SerializedName("asset_type_id") val assetTypeId: Int,
    val name: String,
    @SerializedName("en_name") val enName: String,
    @SerializedName("created_by") val createdBy: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)