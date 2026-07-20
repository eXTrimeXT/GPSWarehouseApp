package com.gps.warehouse.data.remote.assets_dto.map

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Workshop(
    @SerialName("workshop_id") val workshopId: Int,
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    @SerialName("description") val description: String?,
    @SerialName("background_image_url") val backgroundImageUrl: String?,
    @SerialName("geometry") val geometry: Geometry?,
    @SerialName("workshop_width") val workshopWidth: Int?,
    @SerialName("workshop_height") val workshopHeight: Int?,
    @SerialName("offset_x") val offsetX: Int,
    @SerialName("offset_y") val offsetY: Int,
    @SerialName("workshop_scale") val workshopScale: Float,
    @SerialName("color") val color: String,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class Geometry(
    @SerialName("type") val type: String,
    @SerialName("coordinates") val coordinates: List<List<Int>>
)