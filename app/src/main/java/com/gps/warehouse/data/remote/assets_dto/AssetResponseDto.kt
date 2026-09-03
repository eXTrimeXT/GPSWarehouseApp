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

// Базовые поля актива (используются в AssetResponseDto и AssetUpdate)
interface AssetBase {
    val name: String
    val inventoryId: String
    val serialNumber: String?
    val assetStatus: String?
    val assetStatusId: Int?
    val quantity: Int?
    val comment: String?
    val dateIssue: String?          // "YYYY-MM-DD"
    val datePurchasing: String?     // "YYYY-MM-DD"
    val modelId: Int?
    val modelName: String?
    val assetTypeId: Int?
    val parentId: Int?

    // Еженедельная проверка оборудования
    val everyWeekCheck: Boolean?
    val nextService: String?        // "YYYY-MM-DD"
    val servicePeriod: Int?         // дни

    // Временные/денормализованные поля
    val parentName: String?
    val manufacturerName: String?
    val vendorName: String?
    val osName: String?
}

data class AssetUserFullResponse(
    val guid: String,
    @SerializedName("employee_id") val employeeId: String,

    // Даты
    @SerializedName("birth_date") val birthDate: String?,
    @SerializedName("employment_date") val employmentDate: String?,
    @SerializedName("dismissal_date") val dismissalDate: String?,

    // Контакты
    val phone: String?,
    val email: String?,
    val comment: String?,

    // GUID связей
    @SerializedName("position_guid") val positionGuid: String?,
    @SerializedName("department_guid") val departmentGuid: String?,

    // Мета
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,

    // Вычисляемые имена
    @SerializedName("full_name_ru") val fullNameRu: String?,
    @SerializedName("full_name_en") val fullNameEn: String?,

    // Иерархия (WorkplaceResponse)
    val society: WorkplaceResponse?,
    val department: WorkplaceResponse?,
    val division: WorkplaceResponse?,
    val group: WorkplaceResponse?,

    // Должность (PositionResponse)
    val position: PositionResponse?,

    // Поля из AssetAssignment
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("assignment_type") val assignmentType: String?
)

data class PositionResponse(
    val name: String,
    @SerializedName("name_en") val nameEn: String?
)

data class WorkplaceResponse(
    val guid: String,
    val name: String,
    @SerializedName("name_en") val nameEn: String?,
    @SerializedName("short_name") val shortName: String?,
    @SerializedName("creation_date") val creationDate: String?,
    @SerializedName("closure_date") val closureDate: String?,
    @SerializedName("parent_guid") val parentGuid: String?
)

data class AssetResponseDto(
    @SerializedName("asset_id") val assetId: Int,
    override val name: String,
    @SerializedName("inventory_id") override val inventoryId: String,
    @SerializedName("serial_number") override val serialNumber: String?,
    @SerializedName("asset_status") override val assetStatus: String?,
    @SerializedName("asset_status_id") override val assetStatusId: Int?,
    override val comment: String?,
    @SerializedName("date_issue") override val dateIssue: String?,
    @SerializedName("date_purchasing") override val datePurchasing: String?,
    @SerializedName("model_id") override val modelId: Int?,
    @SerializedName("model_name") override val modelName: String?,
    @SerializedName("asset_type_id") override val assetTypeId: Int?,
    @SerializedName("parent_id") override val parentId: Int?,
    @SerializedName("location_id") val locationId: Int?,
    @SerializedName("quantity") override val quantity: Int?,
    @SerializedName("prepared_by") val preparedBy: String?,
    @SerializedName("checked_by") val checkedBy: String?,
    @SerializedName("parent_name") override val parentName: String?,
    @SerializedName("manufacturer_name") override val manufacturerName: String?,
    @SerializedName("vendor_name") override val vendorName: String?,
    @SerializedName("os_name") override val osName: String?,

    // сервисная информация
    @SerializedName("every_week_check") override val everyWeekCheck: Boolean?,
    @SerializedName("next_service") override val nextService: String?,
    @SerializedName("service_period") override val servicePeriod: Int?,

    // Мета
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("updated_by") val updatedBy: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("asset_type_name") val assetTypeName: String?,

    // Вложенные объекты
    val location: AssetLocationResponse?,
    val users: List<AssetUserFullResponse>?,
    @SerializedName("responsible_users") val responsibleUsers: List<AssetUserFullResponse>?,
    @SerializedName("serving_users") val servingUsers: List<AssetUserFullResponse>?,

    // Текущий пользователь
    @SerializedName("current_user") val currentUser: String?,
    @SerializedName("current_user_full_name") val currentUserFullName: String?,

    // Родительский актив
    val parent: AssetParentResponseDto?,
) : AssetBase

data class AssetLocationResponse(
    @SerializedName("workshop_id") val workshopId: Int,
    @SerializedName("workshop_name") val workshopName: String?,
    val place: String?,
    val level: Int?,
    val x: Int?,
    val y: Int?
)

data class LocationResponseDto(
    @SerializedName("workshop_id") val workshopId: Int?,
    @SerializedName("workshop_name") val workshopName: String?,
    val place: String,
    val level: Int?,
    val x: Int?,
    val y: Int?,
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

