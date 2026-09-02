package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetStatusDto(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String
)