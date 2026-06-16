package com.gps.warehouse.data.remote.assets_dto

import com.google.gson.annotations.SerializedName

data class AssetClassDto(
    @SerializedName("class_id") val classId: Int,
    @SerializedName("class_name") val className: String,
    @SerializedName("class_type_id") val classTypeId: Int,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    // Внешние ключи (ID пользователей, если бэкенд их возвращает)
    @SerializedName("created_by") val createdBy: Int?,
    @SerializedName("updated_by") val updatedBy: Int?,
    // === Связанные объекты (Relationships) ===
    // В Python: asset_type = relationship("AssetType", foreign_keys=[class_type_id], lazy="joined")
    @SerializedName("asset_type") val assetType: AssetTypeDto?,
    // В Python: creator = relationship("User", foreign_keys=[created_by], lazy="joined")
    // ВАЖНО: поле называется "creator", а НЕ "created_by_user"!
    val creator: UserDto?,
    // В Python: updater = relationship("User", foreign_keys=[updated_by], lazy="joined")
    // ВАЖНО: поле называется "updater", а НЕ "updated_by_user"!
    val updater: UserDto?
    // Примечание: relationship "models" (List<AssetModel>) намеренно опущен,
    // чтобы избежать циклических ссылок при сериализации JSON.
    // Если нужно получить модели класса — используйте отдельный endpoint.
)