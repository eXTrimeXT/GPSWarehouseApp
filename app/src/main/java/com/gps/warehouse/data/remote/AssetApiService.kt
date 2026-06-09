package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.assets_dto.*
import retrofit2.http.*

// ====================== DTO для авторизации Assets API ======================

/**
 * Ответ от /user_key
 */
data class AssetPublicKeyResponse(
    val key: String,  // PEM-ключ
    val id: String    // ID ключа (обязательно передавать в /user_auth)
)

/**
 * Запрос на /user_auth
 */
data class AssetLoginRequest(
    val login: String,
    val password: String,
    val id: String  // ID ключа из /user_key
)

/**
 * Ответ от /user_auth
 */
data class AssetLoginResponse(
    val status: String,
    val msg: String,
    val data: AssetTokenData
)

data class AssetTokenData(
    val token: String
)

interface AssetApiService {

    // ====================== Активы ======================

    // ====================== Авторизация Assets API ======================

    /**
     * Получение публичного ключа для шифрования пароля (Assets API)
     * Ответ: JSON {"key": "-----BEGIN PUBLIC KEY-----...", "id": "..."}
     */
    @GET("user_key")
    suspend fun getAssetPublicKey(): AssetPublicKeyResponse

    /**
     * Авторизация в Assets API
     * ВАЖНО: endpoint /user_auth (не /login!), и в теле есть поле 'id'
     */
    @POST("user_auth")
    suspend fun assetLogin(@Body request: AssetLoginRequest): AssetLoginResponse

    data class AssetLoginRequest(
        val login: String,
        val password: String
    )

    data class AssetLoginResponse(
        val status: String,
        val msg: String,
        val data: AssetTokenData
    )

    data class AssetTokenData(
        val token: String
    )

    @GET("assets/")
    suspend fun getAssets(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): List<AssetShortDto>

    @GET("assets/{asset_id}")
    suspend fun getAssetById(
        @Header("Authorization") token: String,
        @Path("asset_id") assetId: Int
    ): AssetDto

    // ====================== Типы активов ======================

    @GET("asset-types/")
    suspend fun getAssetTypes(
        @Header("Authorization") token: String
    ): List<AssetTypeDto>

    // ====================== Классы активов ======================

    @GET("asset-classes/")
    suspend fun getAssetClasses(
        @Header("Authorization") token: String,
        @Query("type_id") typeId: Int? = null
    ): List<AssetClassDto>

    // ====================== Модели активов ======================

    @GET("asset-models/")
    suspend fun getAssetModels(
        @Header("Authorization") token: String,
        @Query("class_id") classId: Int? = null
    ): List<AssetModelDto>

    // ====================== Каталог ======================

    @GET("catalog/")
    suspend fun getAssetCatalog(
        @Header("Authorization") token: String
    ): List<AssetCatalogDto>

    // ====================== Пользователи ======================

    @GET("users/")
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): List<UserShortDto>

    @GET("users/{user_id}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): UserDto

    // ====================== Поставщики ======================

    @GET("vendors/")
    suspend fun getVendors(
        @Header("Authorization") token: String
    ): List<VendorShortDto>

    // ====================== Склады ======================

    @GET("warehouses/")
    suspend fun getWarehouses(
        @Header("Authorization") token: String
    ): List<WarehouseShortDto>
}