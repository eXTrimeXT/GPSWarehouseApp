package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetModelDto(
    @SerializedName("model_id") val modelId: Int,
    @SerializedName("model_name") val modelName: String,
    @SerializedName("class_id") val classId: Int,
    val description: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_serial_required") val isSerialRequired: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("asset_class") val assetClass: AssetClassDto?,
    val creator: UserDto?,
    val updater: UserDto?
)