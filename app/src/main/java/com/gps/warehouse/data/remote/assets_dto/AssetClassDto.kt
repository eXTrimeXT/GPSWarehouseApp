package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetClassDto(
    @SerializedName("class_id") val classId: Int,
    @SerializedName("class_name") val className: String,
    @SerializedName("class_type_id") val classTypeId: Int,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("asset_type") val assetType: AssetTypeDto?,
    @SerializedName("created_by_user") val createdByUser: UserDto?,
    @SerializedName("updated_by_user") val updatedByUser: UserDto?
)