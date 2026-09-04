package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

// Начальная схема любого ответа с пагинацией
data class PaginatedResponse(
    val items: List<AssetResponseDto>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_previous") val hasPrevious: Boolean
)