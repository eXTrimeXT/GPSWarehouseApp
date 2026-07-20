package com.gps.warehouse.data.remote.assets_dto.map

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AssetPosition(
    @SerializedName("id") val id: Int,
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("workshop_id") val workshopId: Int,
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int,
    @SerializedName("rotation") val rotation: Int,
    @SerializedName("scale") val scale: Int,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)