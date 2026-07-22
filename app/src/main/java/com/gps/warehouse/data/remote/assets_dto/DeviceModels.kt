package com.gps.warehouse.data.remote.assets_dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceResponse(
    val id: Int,
    val serial_number: String,
    val request_time: String,
    val info: DeviceInfo,
    val system: SystemInfo,
    val hardware: HardwareInfo,
    val network: NetworkInfo,
    val battery: BatteryInfo
)

@Serializable
data class DeviceInfo(val model: String?, val name: String?)

@Serializable
data class SystemInfo(
    val android_version: String?,
    val android_api_version: String?,
    val build_number: String?,
    val language: String?,
    val timezone: String?,
    val uptime: String?
)

@Serializable
data class HardwareInfo(
    val processor: String?,
    val processor_architecture: String?,
    val ram_total: String?,
    val ram_free: String?,
    val storage_total: String?,
    val storage_free: String?,
    val cameras: String?,
    val screen_resolution: String?
)

@Serializable
data class NetworkInfo(
    val connection_type: String?,
    val wifi_ssid: String?,
    val wifi_bssid: String?,
    val wifi_gateway: String?,
    val mac_address: String?,
    val ip_addresses: String?,
    val bluetooth: String?
)

@Serializable
data class BatteryInfo(
    val level: String?,
    val status: String?,
    val temperature: String?
)