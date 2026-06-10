package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class AssetCatalogDto(
    @SerializedName("catalog_id") val catalogId: Int,
    @SerializedName("asset_id") val assetId: Int?,
    @SerializedName("android_id") val androidId: String?,
    @SerializedName("owner_id") val ownerId: Int?,
    @SerializedName("created_at") val createdAt: String,
    val asset: AssetCatalogItemDto?,
    @SerializedName("android_data") val androidData: AndroidDataDto?,
    val owner: UserDto?,
    val creator: UserDto?
)