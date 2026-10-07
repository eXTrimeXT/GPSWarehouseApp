package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class InventorizationSessionDto(
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("asset_type_id") val assetTypeId: Int?,
    @SerializedName("department_codes") val departmentCodes: String?, // коды департаментов через ;
    @SerializedName("asset_type_name") val assetTypeName: String,
    @SerializedName("asset_type_en_name") val assetTypeEnName: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_by_full_name") val createdByFullName: String? = null
)

data class InventorizationItemDto(
    @SerializedName("inventorization_id") val inventorizationId: Int,
    @SerializedName("session_id") val sessionId: Int,
    @SerializedName("asset_id") val assetId: Int?, // Может быть null т.к. есть material_id
    @SerializedName("material_id") val materialId: String?,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("inventory_id") val inventoryId: String?,
    @SerializedName("asset_name") val assetName: String,
    @SerializedName("is_checked") val isChecked: Boolean,
    @SerializedName("quantity") val quantity: Int?,
    @SerializedName("quantity_fact") val quantityFact: Int?,
)

data class InventorizationSessionCreateRequest(
    @SerializedName("asset_type_id") val assetTypeId: Int? = null,
    @SerializedName("department_code") val departmentCode: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null
)

data class CheckItemRequest(
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("material_id") val materialId: String? = null,
    @SerializedName("quantity_fact") val quantityFact: Int? = null
)