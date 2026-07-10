package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName

// Запрос на авторизацию
data class LoginRequest(
    val login: String,
    val password: String
)

// Ответ авторизации
data class LoginResponse(
    val status: String,
    val msg: String,
    @SerializedName("data") val data: TokenData
)

// Класс для хранения токена
data class TokenData(
    val token: String
)

// Запрос информации о пользователе
data class GetUserProfileRequest(
    val token: String
)

// Ответ с информацией о пользователе
data class UserProfileResponse(
    val id: String,
    val login: String,
    val section: String?,                                           // Отдел/секция
    @SerializedName("last_time") val lastTime: String?,     // Время последнего входа
    @SerializedName("last_ip") val lastIp: String?,         // Последний IP
    @SerializedName("warehouse_permissions") val warehousePermissions: List<WarehousePermissionDto>?,   // Права на склады
    @SerializedName("permission") val gpsPermissions: List<GpsPermissionDto>?,
    @SerializedName("assets_is_admin") val assetsIsAdmin: Boolean? = false
)

// Класс для элемента списка прав на склады
data class WarehousePermissionDto(
    @SerializedName("idwares") val id: String,
    @SerializedName("name") val name: String,       // Название склада (например, "3051")
    @SerializedName("is_leader") val isLeader: String, // "1" или "0" полные права на склад
    @SerializedName("virtual") val isVirtual: String   // "1" или "0" виртуальные не учитываются в SAP
)

data class GpsPermissionDto(
    @SerializedName("name_group") val nameGroup: String,
    val read: Boolean,
    val write: Boolean
)