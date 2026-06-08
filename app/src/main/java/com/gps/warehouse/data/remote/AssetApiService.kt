package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.assets_dto.*
import retrofit2.http.*

interface AssetApiService {

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