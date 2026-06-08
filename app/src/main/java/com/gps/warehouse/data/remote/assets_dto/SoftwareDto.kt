package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class SoftwareDto(
    @SerializedName("software_id") val softwareId: Int,
    val name: String,
    val version: String?,
    val licenseKey: String?
)