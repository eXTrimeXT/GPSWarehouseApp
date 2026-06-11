package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class AssetCatalogDto(
    @SerializedName("catalog_id") val catalogId: Int,
    @SerializedName("asset_id") val assetId: Int?,
    @SerializedName("serial_number") val serialNumber: String?, // ✅ Поднят на верхний уровень
    @SerializedName("owner_id") val ownerId: Int?,
    @SerializedName("created_at") val createdAt: String?,
    val owner: UserDto?,
    val creator: UserDto?,
    val asset: AssetDto?,
    @SerializedName("android_data_obj") val androidData: AndroidDataDto?
)