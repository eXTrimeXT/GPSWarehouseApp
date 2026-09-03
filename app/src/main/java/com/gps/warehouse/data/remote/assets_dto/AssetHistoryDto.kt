package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName
import kotlin.time.Instant

data class AssetHistoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("action_type") val actionType: String,
    @SerializedName("field_name") val fieldName: String?,
    @SerializedName("old_value") val oldValue: String?,
    @SerializedName("new_value") val newValue: String?,
    @SerializedName("changed_by") val changedBy: String?,
    @SerializedName("changed_at") val changedAt: String?,
    @SerializedName("comment") val comment: String?,
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("changer_full_name_ru") val changerFullNameRu: String?,
    @SerializedName("changer_full_name_en") val changerFullNameEn: String?
)