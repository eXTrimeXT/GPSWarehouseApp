package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_tab_id") val userTabId: String?,
    val owner: String,
    @SerializedName("user_en_name") val userEnName: String?,
    val permissions: Map<String, Map<String, Boolean>>?,
    @SerializedName("user_position") val userPosition: String?,
    @SerializedName("department_id") val departmentId: Int?,
    val email: String,
    val phone: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

data class UserShortDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_tab_id") val userTabId: String?,
    val owner: String,
    @SerializedName("user_position") val userPosition: String?,
    @SerializedName("department_id") val departmentId: Int?,
    val email: String
)