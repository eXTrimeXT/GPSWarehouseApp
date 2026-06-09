package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class SoftwareDto(
    @SerializedName("software_id") val softwareId: Int?,

    // === Офисное ПО ===
    @SerializedName("office_type") val officeType: String?,      // Тип офиса (MS Office, LibreOffice...)
    @SerializedName("office_key") val officeKey: String?,        // Ключ лицензии офиса

    // === Операционная система ===
    @SerializedName("os_type") val osType: String?,              // Тип ОС (Windows 10, Ubuntu...)
    @SerializedName("os_key") val osKey: String?,                // Ключ лицензии ОС

    // === Удалённое управление ===
    @SerializedName("remote_control") val remoteControl: String?, // ПО удалённого управления (TeamViewer, AnyDesk...)

    // === Права доступа ===
    @SerializedName("admin_permission") val adminPermission: Boolean?, // Админ права

    // === Установка ===
    @SerializedName("who_installed") val whoInstalled: Int?,     // ID пользователя, который установил (ForeignKey users.user_id)
    // Примечание: Если бэкенд возвращает здесь объект пользователя, замените Int? на UserShortDto?

    @SerializedName("installed_at") val installedAt: String?,    // Дата установки

    // === Служебные поля ===
    val comment: String?,                                        // Комментарий
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)