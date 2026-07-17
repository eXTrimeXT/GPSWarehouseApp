package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class PaginatedAssetResponse(
    val items: List<AssetResponseDto>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_previous") val hasPrevious: Boolean
)

data class AssetResponseDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("asset_status") val assetStatus: String,
    val comment: String?,
    @SerializedName("date_issue") val dateIssue: String?,
    @SerializedName("date_purchasing") val datePurchasing: String?,
    @SerializedName("model_id") val modelId: Int?,
    @SerializedName("model_name") val modelName: String?,
    @SerializedName("asset_type_id") val assetTypeId: Int?,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("location_id") val locationId: Int?,
    @SerializedName("prepared_by") val preparedBy: String?,
    @SerializedName("checked_by") val checkedBy: String?,
    @SerializedName("parent_name") val parentName: String?,
    @SerializedName("manufacturer_name") val manufacturerName: String?,
    @SerializedName("vendor_name") val vendorName: String?,
    @SerializedName("os_name") val osName: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("updated_by") val updatedBy: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("asset_type_name") val assetTypeName: String?,
    val location: LocationResponseDto?,
    val users: List<AssetUserResponseDto>?,
    val parent: AssetParentResponseDto?
)

data class LocationResponseDto(
    @SerializedName("location_id") val locationId: Int,
    val name: String?,
    val address: String?
)

data class AssetUserResponseDto(
    val guid: String,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("full_name_ru") val fullNameRu: String,
    @SerializedName("full_name_en") val fullNameEn: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?
)

data class AssetParentResponseDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("asset_status") val assetStatus: String,
    val comment: String?,
    @SerializedName("date_issue") val dateIssue: String?,
    @SerializedName("date_purchasing") val datePurchasing: String?,
    @SerializedName("model_id") val modelId: Int?,
    @SerializedName("model_name") val modelName: String?,
    @SerializedName("asset_type_id") val assetTypeId: Int?,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("location_id") val locationId: Int?,
    @SerializedName("prepared_by") val preparedBy: String?,
    @SerializedName("checked_by") val checkedBy: String?,
    @SerializedName("parent_name") val parentName: String?,
    @SerializedName("manufacturer_name") val manufacturerName: String?,
    @SerializedName("vendor_name") val vendorName: String?,
    @SerializedName("os_name") val osName: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("updated_by") val updatedBy: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("asset_type_name") val assetTypeName: String?
)