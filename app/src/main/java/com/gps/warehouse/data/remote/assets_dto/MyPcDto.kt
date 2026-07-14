package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class MyPcDto(
    val id: Int,
    @SerializedName("user_tab_id") val userTabId: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    val user: UserInfoDto?,
    val network: MyPcNetworkInfoDto?,
    val os: MyPcOsInfoDto?,
    val components: ComponentsInfoDto?,
    @SerializedName("office_package") val officePackage: List<String>?,
    val programs: List<String>?
)

data class UserInfoDto(
    val username: String?,
    val userpath: String?,
    val sid: String?
)

data class MyPcNetworkInfoDto(
    @SerializedName("line_speed_mbps") val lineSpeedMbps: String?,
    @SerializedName("ipv6_link_local") val ipv6LinkLocal: String?,
    @SerializedName("ipv4_address") val ipv4Address: String?,
    @SerializedName("default_gateway_ipv4") val defaultGatewayIpv4: String?,
    @SerializedName("dns_servers_ipv4") val dnsServersIpv4: List<String>?,
    val manufacturer: String?,
    val description: String?,
    @SerializedName("driver_version") val driverVersion: String?,
    @SerializedName("mac_address") val macAddress: String?
)

data class MyPcOsInfoDto(
    val os: String?,
    @SerializedName("os_release") val osRelease: String?,
    @SerializedName("os_version") val osVersion: String?,
    @SerializedName("pc_arch") val pcArch: String?,
    @SerializedName("pc_name") val pcName: String?,
    @SerializedName("device_type") val deviceType: String?,
    @SerializedName("product_id") val productId: String?,
    @SerializedName("device_id") val deviceId: String?,
    @SerializedName("serial_number") val serialNumber: String?
)

// Эти классы можно переиспользовать, если они уже есть, но для надежности продублируем структуру
data class ComponentsInfoDto(
    val cpu: CpuInfoDto?,
    val motherboard: String?,
    val ram: RamInfoDto?,
    val gpu: List<GpuInfoDto>?,
    val disks: List<DiskInfoDto>?
)
data class CpuInfoDto(val name: String?, val cores: Int?, val processors: Int?, val speed: String?)
data class RamInfoDto(val total: String?, val sticks: List<String>?)
data class GpuInfoDto(val name: String?, val vram: String?, val driver: String?)
data class DiskInfoDto(val model: String?, val size: String?, @SerializedName("interface") val diskInterface: String?)