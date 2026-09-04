package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

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
    @SerializedName("full_name_ru") val fullNameRu: String,
    @SerializedName("full_name_en") val fullNameEn: String,

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

data class WorkplaceResponse(
    val guid: String,
    val name: String,
    @SerializedName("name_en") val nameEn: String?,
    @SerializedName("short_name") val shortName: String?,
    @SerializedName("creation_date") val creationDate: String?,
    @SerializedName("closure_date") val closureDate: String?,
    @SerializedName("parent_guid") val parentGuid: String?,
)

data class PositionResponse(
    val name: String,
    @SerializedName("name_en") val nameEn: String?
)

// Краткая схема сотрудника для списков (соответствует ответу API /zup/employees)
data class EmployeeShortResponse(
    @SerializedName("guid") val guid: String,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("full_name_ru") val fullNameRu: String?,
    @SerializedName("full_name_en") val fullNameEn: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("comment") val comment: String?,

    // Иерархия подразделения
    @SerializedName("society") val society: WorkplaceResponse?,
    @SerializedName("department") val department: WorkplaceResponse?,
    @SerializedName("division") val division: WorkplaceResponse?,
    @SerializedName("group") val group: WorkplaceResponse?,

    // Должность
    @SerializedName("position") val position: PositionResponse?
)