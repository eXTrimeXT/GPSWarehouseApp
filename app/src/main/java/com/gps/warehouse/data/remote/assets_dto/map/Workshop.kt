package com.gps.warehouse.data.remote.assets_dto.map

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Workshop(
    @SerializedName("workshop_id") val workshopId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("description") val description: String?,
    @SerializedName("background_image_url") val backgroundImageUrl: String?,
    @SerializedName("geometry") val geometry: Geometry?,
    @SerializedName("workshop_width") val workshopWidth: Int?,
    @SerializedName("workshop_height") val workshopHeight: Int?,
    @SerializedName("offset_x") val offsetX: Int,
    @SerializedName("offset_y") val offsetY: Int,
    @SerializedName("workshop_scale") val workshopScale: Float,
    @SerializedName("color") val color: String,
    @SerializedName("is_active") val isActive: Boolean
)

@Serializable
data class Geometry(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: List<List<Int>>
)