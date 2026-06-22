package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String?,
    @SerializedName("affixed_inventory_id") val affixedInventoryId: String?,
    @SerializedName("asset_status") val assetStatus: String?,
    @SerializedName("model_id") val modelId: Int?,
    @SerializedName("warehouse_id") val warehouseId: Int?,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("software_id") val softwareId: Int?,
    @SerializedName("manufacturer_id") val manufacturerId: Int?,
    @SerializedName("vendor_id") val vendorId: Int?,
    @SerializedName("type_domain") val typeDomain: String?,
    @SerializedName("info_storage_location") val infoStorageLocation: String?,
    @SerializedName("date_issue") val dateIssue: String?,
    @SerializedName("date_purchasing") val datePurchasing: String?,
    val comment: String?,
    @SerializedName("deleted_at") val deletedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    val model: AssetModelDto?,
    @SerializedName("warehouse_obj") val warehouse: WarehouseDto?,
    val preparer: UserDto?,
    val checker: UserDto?,
    val software: SoftwareDto?,
    val manufacturer: VendorDto?,
    val vendor: VendorDto?
)

data class AssetShortDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("asset_status") val assetStatus: String,
    @SerializedName("model_id") val modelId: Int?,
    @SerializedName("type_asset") val typeAsset: String?,
    @SerializedName("warehouse_id") val warehouseId: Int?,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("software_id") val softwareId: Int?,
    @SerializedName("manufacturer_id") val manufacturerId: Int?,
    @SerializedName("vendor_id") val vendorId: Int?
)

// Карта
data class WorkshopDto(
    @SerializedName("workshop_id") val workshopId: Int,
    val name: String,
    val code: String,
    val is_active: Boolean,
    val geometry: GeometryDto?,
    val workshop_width: Int?,
    val workshop_height: Int?,
    val offset_x: Double,
    val offset_y: Double,
    val workshop_scale: Double,
    val color: String
)

data class GeometryDto(
    val type: String, // "polygon"
    val coordinates: List<List<Double>> // [[x, y], [x, y], ...]
)

data class AssetPositionDto(
    val id: Int,
    @SerializedName("asset_id") val assetId: Int,
    @SerializedName("workshop_id") val workshopId: Int,
    val x: Double,
    val y: Double,
    val rotation: Double,
    val scale: Double,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)