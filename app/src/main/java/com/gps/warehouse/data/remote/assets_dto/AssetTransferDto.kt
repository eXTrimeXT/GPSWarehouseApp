package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

// ==================== Запрос на передачу актива ====================

/**
 * Запрос на создание SAP актива (для передачи актива из SAP)
 */
data class SapAssetCreateRequestDto(
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("name") val name: String,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("asset_type_id") val assetTypeId: Int,
    @SerializedName("model_id") val modelId: Int? = null,
    @SerializedName("asset_status_id") val assetStatusId: Int? = 9,  // "На складе"
    @SerializedName("quantity") val quantity: Int = 1
)

/**
 * Запрос на передачу актива другому пользователю
 */
data class AssetTransferRequestDto(
    @SerializedName("asset_id") val assetId: Int? = null,
    @SerializedName("material_id") val materialId: String? = null,
    @SerializedName("target_employee_id") val targetEmployeeId: String,
    @SerializedName("assignment_type") val assignmentType: String = "user",  // "user" или "responsible"
    @SerializedName("comment") val comment: String? = null
)

// ==================== Ответ на передачу актива ====================

/**
 * Информация об активе
 */
data class AssetInfoDto(
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("name") val name: String,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("asset_type_id") val assetTypeId: Int? = null,
    @SerializedName("model_id") val modelId: Int? = null,
    @SerializedName("source") val source: String  // "local" или "sap"
)

/**
 * Краткая информация о сотруднике
 */
data class EmployeeInfoDto(
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("full_name") val fullName: String? = null
)

/**
 * Информация о созданном уведомлении
 */
data class NotificationInfoDto(
    @SerializedName("notification_id") val notificationId: Int,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_type_ru") val eventTypeRu: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String
)

/**
 * Ответ на запрос передачи актива
 */
data class AssetTransferResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("action") val action: String,  // "transfer_initiated"
    @SerializedName("notification") val notification: NotificationInfoDto,
    @SerializedName("asset") val asset: AssetInfoDto,
    @SerializedName("initiator") val initiator: EmployeeInfoDto,
    @SerializedName("target_employee") val targetEmployee: EmployeeInfoDto,
    @SerializedName("assignment_type") val assignmentType: String,
    @SerializedName("assignment_type_ru") val assignmentTypeRu: String,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("created_at") val createdAt: String
)

// ==================== Ответ на принятие/отклонение передачи ====================

/**
 * Запрос на принятие/отклонение передачи
 */
data class TransferActionRequestDto(
    @SerializedName("action") val action: String,  // "accept" или "decline"
    @SerializedName("comment") val comment: String? = null
)

/**
 * Ответ на принятие/отклонение передачи
 */
data class AssetTransferRespondResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("action") val action: String,  // "accept" или "decline"
    @SerializedName("notification") val notification: NotificationInfoDto,
    @SerializedName("asset") val asset: AssetInfoDto,
    @SerializedName("initiator") val initiator: EmployeeInfoDto,
    @SerializedName("responder") val responder: EmployeeInfoDto,
    @SerializedName("assignment_type") val assignmentType: String,
    @SerializedName("assignment_type_ru") val assignmentTypeRu: String,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("responded_at") val respondedAt: String,
    @SerializedName("new_assignment_id") val newAssignmentId: Int? = null,
    @SerializedName("previous_assignment_closed") val previousAssignmentClosed: Boolean = false
)