package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

// Схема для android_id
data class AndroidDataDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("android_id") val androidId: String?,
    val device: DeviceInfoDto?,
    val system: SystemInfoDto?,
    val hardware: HardwareInfoDto?,
    val network: NetworkInfoDto?,
    val battery: BatteryInfoDto?
)

data class DeviceInfoDto(
    val model: String?,
    val name: String?
)

data class SystemInfoDto(
    @SerializedName("android_version") val androidVersion: String?,
    @SerializedName("android_api_version") val androidApiVersion: String?,
    @SerializedName("build_number") val buildNumber: String?,
    val language: String?,
    val timezone: String?,
    val uptime: String?,
    @SerializedName("request_time") val requestTime: String?
)

data class HardwareInfoDto(
    val processor: String?,
    @SerializedName("processor_architecture") val processorArchitecture: String?,
    @SerializedName("ram_total") val ramTotal: String?,
    @SerializedName("ram_free") val ramFree: String?,
    @SerializedName("storage_total") val storageTotal: String?,
    @SerializedName("storage_free") val storageFree: String?,
    val cameras: String?,
    @SerializedName("screen_resolution") val screenResolution: String?
)

data class NetworkInfoDto(
    @SerializedName("connection_type") val connectionType: String?,
    @SerializedName("wifi_ssid") val wifiSsid: String?,
    @SerializedName("wifi_bssid") val wifiBssid: String?,
    @SerializedName("wifi_gateway") val wifiGateway: String?,
    @SerializedName("mac_address") val macAddress: String?,
    @SerializedName("ip_addresses") val ipAddresses: String?,
    val bluetooth: String?
)

data class BatteryInfoDto(
    val level: String?,
    val status: String?,
    val temperature: String?
)

data class AssetCatalogItemDto(
    @SerializedName("asset_id") val assetId: Int,
    val name: String,
    @SerializedName("inventory_id") val inventoryId: String,
    @SerializedName("serial_number") val serialNumber: String?,
    @SerializedName("asset_status") val assetStatus: String,
    @SerializedName("type_domain") val typeDomain: String?,
    @SerializedName("affixed_inventory_id") val affixedInventoryId: Boolean?,
    @SerializedName("info_storage_location") val infoStorageLocation: String?,
    @SerializedName("date_issue") val dateIssue: String?,
    @SerializedName("date_purchasing") val datePurchasing: String?,
    val comment: String?,
    @SerializedName("model_id") val modelId: Int?,
    @SerializedName("warehouse_id") val warehouseId: Int?,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("software_id") val softwareId: Int?,
    @SerializedName("prepared_by") val preparedBy: Int?,
    @SerializedName("checked_by") val checkedBy: Int?,
    @SerializedName("manufacturer_id") val manufacturerId: Int?,
    @SerializedName("vendor_id") val vendorId: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("deleted_at") val deletedAt: String?,
    val model: AssetModelDto?,
    @SerializedName("warehouse_obj") val warehouse: WarehouseDto?,
    val preparer: UserDto?,
    val checker: UserDto?,
    val software: SoftwareDto?,
    val manufacturer: VendorDto?,
    val vendor: VendorDto?
)