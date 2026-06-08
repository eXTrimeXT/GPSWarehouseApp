package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class VendorDto(
    @SerializedName("vendor_id") val vendorId: Int,
    val name: String,
    @SerializedName("vendor_class_id") val vendorClassId: Int,
    @SerializedName("company_id") val companyId: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("created_by") val createdBy: Int,
    @SerializedName("vendor_class") val vendorClass: VendorClassDto?,
    val company: CompanyDto?,
    val creator: UserShortDto?
)

data class VendorShortDto(
    @SerializedName("vendor_id") val vendorId: Int,
    val name: String,
    @SerializedName("vendor_class_id") val vendorClassId: Int,
    @SerializedName("company_id") val companyId: Int?
)

data class VendorClassDto(
    @SerializedName("vendor_class_id") val vendorClassId: Int,
    val name: String,
    val description: String?
)

data class CompanyDto(
    @SerializedName("company_id") val companyId: Int,
    val name: String,
    val inn: String?,
    val kpp: String?
)