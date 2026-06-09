package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class WarehouseDto(
    @SerializedName("warehouse_id") val warehouseId: Int,
    val name: String,
    @SerializedName("location_id") val locationId: Int?,
    @SerializedName("prepared_by") val preparedBy: Int?,
    val location: LocationDto?,
    val preparer: UserShortDto?
)

data class WarehouseShortDto(
    @SerializedName("warehouse_id") val warehouseId: Int,
    val name: String,
    @SerializedName("location_id") val locationId: Int?,
    @SerializedName("prepared_by") val preparedBy: Int?
)

data class LocationDto(
    @SerializedName("location_id")
    val locationId: Int,

    @SerializedName("country")
    val country: String?,

    @SerializedName("city")
    val city: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("room")
    val room: String?,

    @SerializedName("floor")
    val floor: String?

    // Примечание: Поля relationships (companies, warehouses) намеренно опущены.
    // Включение списков связанных объектов в DTO локации создаст огромную нагрузку
    // и риск циклических ссылок (Circular Reference) при сериализации JSON.
)