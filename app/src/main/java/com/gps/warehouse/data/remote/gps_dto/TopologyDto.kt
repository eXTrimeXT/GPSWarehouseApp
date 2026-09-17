package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName

// Поля топологий
data class TopologyDto(
    @SerializedName("id") val id: String,
    // Название позиции (BUFF, 2-3-3 и т.д.)
    @SerializedName("position") val position: String,
    // Название позиции при сканировании штрихкода M&U
    @SerializedName("position_scan") val positionScan: String
)

// Запрос на получение топологий
data class GetTopologyRequest(
    @SerializedName("storage_id") val storageId: String,
    @SerializedName("token") val token: String
)

// Запрос на обновление материала
data class UpdateWmsRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("material") val material: String,
    @SerializedName("max") val max: Int,
    @SerializedName("min") val min: Int,
    @SerializedName("position_id") val positionId: String,
    @SerializedName("position") val position: String,
    @SerializedName("price") val price: String,
    @SerializedName("qty") val qty: Int,
    @SerializedName("sap_a") val sapA: Int,
    @SerializedName("storage_id") val storageId: String,
    @SerializedName("storage") val storage: String,
    @SerializedName("name") val name: String,
    @SerializedName("token") val token: String
)