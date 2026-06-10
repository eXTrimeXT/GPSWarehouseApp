package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.assets_dto.*
import retrofit2.http.*

interface AssetApiService {

    // ====================== Авторизация Assets API ======================
    /**
     * Регистрация токена GPS API в Assets API
     * Создает сессию в Redis Assets API
     */
    @POST("auth_token")
    suspend fun registerToken(@Body request: RegisterTokenRequest): RegisterTokenResponse

    // ====================== Активы ======================
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
    @GET("assets-types/")
    suspend fun getAssetTypes(
        @Header("Authorization") token: String
    ): List<AssetTypeDto>

    // ====================== Классы активов ======================
    @GET("catalog/classes/")
    suspend fun getAssetClasses(
        @Header("Authorization") token: String,
        @Query("type_id") typeId: Int? = null
    ): List<AssetClassDto>

    @GET("catalog/classes/{class_id}")
    suspend fun getAssetClassById(
        @Header("Authorization") token: String,
        @Path("class_id") classId: Int
    ): AssetClassDto

    // ====================== Модели активов ======================
    @GET("catalog/models/")
    suspend fun getAssetModels(
        @Header("Authorization") token: String,
        @Query("class_id") classId: Int? = null
    ): List<AssetModelDto>

    @GET("catalog/models/{model_id}")
    suspend fun getAssetModelById(
        @Header("Authorization") token: String,
        @Path("model_id") modelId: Int
    ): AssetModelDto

    // ====================== Каталог ======================
    @GET("catalog/items/")
    suspend fun getCatalogItems(
        @Header("Authorization") token: String
    ): List<AssetCatalogDto>

    @GET("catalog/items/{catalog_id}")
    suspend fun getCatalogItemById(
        @Header("Authorization") token: String,
        @Path("catalog_id") catalogId: Int
    ): AssetCatalogDto

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

    // Текущий пользователь (или мы)
    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): AssetsUserProfileDto

    // Данные ПК пользователя
    @GET("pc-data/{username}")
    suspend fun getPcData(
        @Header("Authorization") token: String,
        @Path("username") username: String
    ): PcDataDto
}