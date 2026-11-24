package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

// ==================== SAP: Классы активов ====================

data class SapAssetClassResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("response") val response: SapAssetClassListResponseDto?
)

data class SapAssetClassListResponseDto(
    @SerializedName("total") val total: Int,
    @SerializedName("count") val count: Int,
    @SerializedName("data") val data: List<SapAssetClassDto>
)

data class SapAssetClassDto(
    @SerializedName("material_class") val materialClass: String,
    @SerializedName("language") val language: String?,
    @SerializedName("material_class_short") val materialClassShort: String?,
    @SerializedName("material_class_name") val materialClassName: String
)