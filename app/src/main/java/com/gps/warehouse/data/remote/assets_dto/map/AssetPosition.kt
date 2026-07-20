package com.gps.warehouse.data.remote.assets_dto.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetPosition(
    @SerialName("id") val id: Int,
    @SerialName("asset_id") val assetId: Int,
    @SerialName("workshop_id") val workshopId: Int,
    @SerialName("x") val x: Int,
    @SerialName("y") val y: Int,
    @SerialName("rotation") val rotation: Int,
    @SerialName("scale") val scale: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)