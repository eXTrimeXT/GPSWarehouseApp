package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class MyAssetDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("asset_status") val assetStatus: String,
    @SerializedName("asset_status_id") val assetStatusId: Int?,
    @SerializedName("asset_type_name") val assetTypeName: String?,
    @SerializedName("model_name") val modelName: String?,
    val comment: String?,
    @SerializedName("date_issue") val dateIssue: String?,
    @SerializedName("date_purchasing") val datePurchasing: String?,
    @SerializedName("parent_name") val parentName: String?,
    val location: LocationInfoDto?,
    val users: List<AssignedUserDto>?,
    val parent: ParentAssetDto?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class LocationInfoDto(
//    @SerializedName("location_id") val locationId: Int?,
//    val city: String?,
//    val address: String?,
//    val room: String?,
//    val floor: String?
    @SerializedName("workshop_id") val workshopId: Int?,
    val place: String,
    val level: Int?,
    val x: Int?,
    val y: Int?,
)

data class AssignedUserDto(
    val guid: String,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("full_name_ru") val fullNameRu: String,
    @SerializedName("full_name_en") val fullNameEn: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?
)

data class ParentAssetDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("asset_status") val assetStatus: String,
    @SerializedName("asset_type_name") val assetTypeName: String?,
    @SerializedName("model_name") val modelName: String?,
    val comment: String?,
    @SerializedName("date_issue") val dateIssue: String?,
    @SerializedName("date_purchasing") val datePurchasing: String?,
    @SerializedName("parent_name") val parentName: String?,
    val location: LocationInfoDto?,
    val users: List<AssignedUserDto>?,
    val parent: ParentAssetDto?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)