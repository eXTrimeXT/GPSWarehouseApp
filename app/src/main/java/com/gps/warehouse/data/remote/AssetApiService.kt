package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.MyAssetDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import com.gps.warehouse.data.remote.assets_dto.PaginatedAssetResponse
import com.gps.warehouse.data.remote.assets_dto.map.AssetPosition
import com.gps.warehouse.data.remote.assets_dto.map.Workshop
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


    // ====================== Активы ======================
    @GET("assets/")
    suspend fun getAssets(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("name") name: String? = null,
        @Query("inventory_id") inventoryId: String? = null,
        @Query("serial_number") serialNumber: String? = null,
        @Query("asset_status") assetStatus: String? = null,
        @Query("model_id") modelId: Int? = null,
        @Query("asset_type_id") assetTypeId: Int? = null,
        @Query("parent_id") parentId: Int? = null,
        @Query("location_id") locationId: Int? = null
    ): PaginatedAssetResponse

    // ====================== Детали актива ======================
    @GET("assets/{asset_id}")
    suspend fun getAssetById(
        @Header("Authorization") token: String,
        @Path("asset_id") assetId: Int
    ): AssetResponseDto

    // ====================== Карта активов ======================
    @GET("workshops")
    suspend fun getWorkshops(): List<Workshop>

    @GET("asset-positions")
    suspend fun getAssetPositions(): List<AssetPosition>
}