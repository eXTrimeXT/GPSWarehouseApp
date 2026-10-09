package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class MvzListResponseDto(
    val message: String,
    val success: Boolean,
    val response: MvzDataResponse,
)

data class MvzDataResponse(
    val count: Int,
    val total: Int,
    val data: List<MvzDepartments>
)

data class MvzDepartments(
    @SerializedName("department_code") val departmentCode: String?,
    @SerializedName("department_name") val departmentName: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("parent_department") val parentDepartment: String?,
)