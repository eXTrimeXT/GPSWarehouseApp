package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.MyAssetDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AssetApiService {

    /**
     * Получение списка активов, закрепленных за текущим пользователем.
     */
    @GET("assets/assignments/me")
    suspend fun getMyAssignedAssets(
        @Header("Authorization") token: String
    ): List<MyAssetDto>

    @GET("assets/{asset_id}")
    suspend fun getMyAssetById(
        @Header("Authorization") token: String,
        @Path("asset_id") assetId: Int
    ): MyAssetDto

    @GET("assets/assignments/my-pc")
    suspend fun getMyPcs(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<MyPcDto>

    // ====================== Типы активов ======================
    @GET("assets-types/")
    suspend fun getAssetTypes(
        @Header("Authorization") token: String
    ): List<AssetTypeDto>

}