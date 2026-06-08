package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    val token: String,
    @SerializedName("materials_data") val materialsData: List<MaterialQty>
)

data class CreateOrderResponse(
    val success: String,
    val order: String
)

data class OrderDto(
    val id: String,
    @SerializedName("norder") val orderNumber: String,
    @SerializedName("nsaporder") val sapOrder: String?,
    @SerializedName("ndispatch") val dispatch: String?,
    @SerializedName("ninvoice") val invoice: String?,
    @SerializedName("ndoc") val doc: String?,
    @SerializedName("fi_num") val fiNum: String?,
    @SerializedName("date_create") val dateCreate: String,
    @SerializedName("date_sent") val dateSent: String?,
    @SerializedName("date_inway") val dateInWay: String?,
    @SerializedName("date_done") val dateDone: String?,
    val status: String,
    val qty: String,
    val rules: Rules?
)

data class Rules(
    val ps: String?,
    val pda: String?
)

data class GetOrdersRequest(
    val token: String,
    val type: String,           // "status", "archive" или "receive"
    val order: String? = null   // Нужен только если type = "receive"
)