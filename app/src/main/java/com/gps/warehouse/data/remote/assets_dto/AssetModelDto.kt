package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetModelDto(
    @SerializedName("model_id") val modelId: Int,
    @SerializedName("model_name") val modelName: String,
    @SerializedName("class_id") val classId: Int,
    @SerializedName("description") val description: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_serial_required") val isSerialRequired: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    // Внешние ключи (ID пользователей)
    @SerializedName("created_by") val createdBy: Int?,
    @SerializedName("updated_by") val updatedBy: Int?,
    // Связанные объекты (Relationships)
    @SerializedName("asset_class") val assetClass: AssetClassDto?,
    @SerializedName("creator") val creator: UserShortDto?, // Используем ShortDto, чтобы не тянуть тяжелые объекты пользователей
    @SerializedName("updater") val updater: UserShortDto?
)