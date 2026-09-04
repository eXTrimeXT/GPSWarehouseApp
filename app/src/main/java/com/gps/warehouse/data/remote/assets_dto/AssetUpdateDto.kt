package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class AssetUserUpdate(
    @SerializedName("employee_id") val employeeId: String
)

data class AssetLocationUpdate(
    @SerializedName("workshop_id") val workshopId: Int,
    @SerializedName("place") val place: String? = null,
    @SerializedName("level") val level: Int? = null,
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int,
)


// ПОЛНАЯ КОПИЯ AssetResponseDto — все поля опциональны
data class AssetUpdate(
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("inventory_id") val inventoryId: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("asset_status") val assetStatus: String? = null,
    @SerializedName("asset_status_id") val assetStatusId: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("date_issue") val dateIssue: String? = null,
    @SerializedName("date_purchasing") val datePurchasing: String? = null,
    @SerializedName("model_id") val modelId: Int? = null,
    @SerializedName("model_name") val modelName: String? = null,
    @SerializedName("asset_type_id") val assetTypeId: Int? = null,
    @SerializedName("parent_id") val parentId: Int? = null,
    @SerializedName("location_id") val locationId: Int? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("prepared_by") val preparedBy: String? = null,
    @SerializedName("checked_by") val checkedBy: String? = null,
    @SerializedName("parent_name") val parentName: String? = null,
    @SerializedName("manufacturer_name") val manufacturerName: String? = null,
    @SerializedName("vendor_name") val vendorName: String? = null,
    @SerializedName("os_name") val osName: String? = null,

    // Сервисная информация
    @SerializedName("every_week_check") val everyWeekCheck: Boolean? = null,
    @SerializedName("next_service") val nextService: String? = null,
    @SerializedName("service_period") val servicePeriod: Int? = null,

    // Мета
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("updated_by") val updatedBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("asset_type_name") val assetTypeName: String? = null,

    // Вложенные объекты
    @SerializedName("location") val location: AssetLocationUpdate? = null,

    // Пользователи
    @SerializedName("users") val users: List<AssetUserUpdate>? = null,
    @SerializedName("responsible_users") val responsibleUsers: List<AssetUserUpdate>? = null,
    @SerializedName("serving_users") val servingUsers: List<AssetUserUpdate>? = null,

    // Текущий пользователь
    @SerializedName("current_user") val currentUser: String? = null,
    @SerializedName("current_user_full_name") val currentUserFullName: String? = null,

    // Родительский актив
    @SerializedName("parent") val parent: AssetParentResponseDto? = null
)