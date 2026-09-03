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
    @SerializedName("rotation") val rotation: Int? = 0,
    @SerializedName("scale") val scale: Int? = 100
)


data class AssetUpdate(
    @SerializedName("name") val name: String? = null,
    @SerializedName("inventory_id") val inventoryId: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("asset_status_id") val assetStatusId: Int? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("date_issue") val dateIssue: LocalDate? = null,
    @SerializedName("date_purchasing") val datePurchasing: LocalDate? = null,
    @SerializedName("model_id") val modelId: Int? = null,
    @SerializedName("model_name") val modelName: String? = null,
    @SerializedName("asset_type_id") val assetTypeId: Int? = null,
    @SerializedName("parent_id") val parentId: Int? = null,

    // Локация на карте
    @SerializedName("location") val location: AssetLocationUpdate? = null,

    // Еженедельная проверка оборудования
    @SerializedName("every_week_check") val everyWeekCheck: Boolean? = null,
    @SerializedName("next_service") val nextService: LocalDate? = null,
    @SerializedName("service_period") val servicePeriod: Int? = null,

    // Временные поля (не отправляются на сервер, но могут использоваться в UI)
    @SerializedName("parent_name") val parentName: String? = null,
    @SerializedName("manufacturer_name") val manufacturerName: String? = null,
    @SerializedName("vendor_name") val vendorName: String? = null,
    @SerializedName("os_name") val osName: String? = null,

    // Текущий пользователь
    @SerializedName("current_user") val currentUser: String? = null,

    // Для синхронизации привязок пользователей
    @SerializedName("users") val users: List<AssetUserUpdate>? = null,
    @SerializedName("responsible_users") val responsibleUsers: List<AssetUserUpdate>? = null,
    @SerializedName("serving_users") val servingUsers: List<AssetUserUpdate>? = null
)