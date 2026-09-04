package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.assets_dto.AssetHistoryDto
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetStatusDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.AssetUpdate
import com.gps.warehouse.data.remote.assets_dto.CheckItemRequest
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionCreateRequest
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import com.gps.warehouse.data.remote.assets_dto.NotificationResponseDto
import com.gps.warehouse.data.remote.assets_dto.PaginatedResponse
import com.gps.warehouse.data.remote.assets_dto.PlaySoundResponse
import com.gps.warehouse.data.remote.assets_dto.map.AssetPosition
import com.gps.warehouse.data.remote.assets_dto.map.Workshop
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AssetApiService {

    /**
     * Получение списка активов, закрепленных за текущим пользователем.
     */
    @GET("assets/assignments/me")
    suspend fun getMyAssignedAssets(
        @Header("Authorization") token: String
    ): List<AssetResponseDto>

    @GET("assets/{asset_id}")
    suspend fun getMyAssetById(
        @Header("Authorization") token: String,
        @Path("asset_id") assetId: Int
    ): AssetResponseDto

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
    ): PaginatedResponse

    // ====================== Детали актива ======================
    @GET("assets/{asset_id}")
    suspend fun getAssetById(
        @Header("Authorization") token: String,
        @Path("asset_id") assetId: Int
    ): AssetResponseDto

    // ====================== Статус актива ======================
    @GET("asset-status/")
    suspend fun getAssetStatuses(
        @Header("Authorization") token: String
    ): List<AssetStatusDto>

    // ====================== Карта активов ======================
    @GET("workshops")
    suspend fun getWorkshops(
        @Header("Authorization") token: String
    ): List<Workshop>

    @GET("asset-positions")
    suspend fun getAssetPositions(
        @Header("Authorization") token: String
    ): List<AssetPosition>

    @GET("android-data/")
    suspend fun getMobileDevices(
        @Header("Authorization") token: String,
        @Query("serial_number") serialNumber: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): List<DeviceResponse>

    // ====================== Инвентаризация ======================
    @GET("inventorization/sessions/")
    suspend fun getInventorizationSessions(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): List<InventorizationSessionDto>

    @GET("inventorization/sessions/{session_id}/items/")
    suspend fun getInventorizationSessionItems(
        @Header("Authorization") token: String,
        @Path("session_id") sessionId: Int
    ): List<InventorizationItemDto>

    @POST("inventorization/sessions/")
    suspend fun startInventorizationSession(
        @Header("Authorization") token: String,
        @Body request: InventorizationSessionCreateRequest
    ): InventorizationSessionDto

    @POST("inventorization/sessions/{session_id}/check")
    suspend fun checkInventorizationItem(
        @Header("Authorization") token: String,
        @Path("session_id") sessionId: Int,
        @Body request: CheckItemRequest
    ): Map<String, String>

    @POST("inventorization/sessions/{session_id}/complete")
    suspend fun completeInventorizationSession(
        @Header("Authorization") token: String,
        @Path("session_id") sessionId: Int
    ): InventorizationSessionDto

    // Запрос на отправку сигнала для устройства с serial_number
    @POST("android-data/{serial_number}/play-sound")
    suspend fun playDeviceSound(
        @Header("Authorization") token: String,
        @Path("serial_number") serialNumber: String
    ): Response<PlaySoundResponse>

    // Уведомления
    @GET("notifications/my")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("direction") direction: String  = "all" // default = all, incoming, outgoing
    ): NotificationResponseDto

    @GET("asset-history/{assetId}")
    suspend fun getAssetHistory(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: Int
    ): List<AssetHistoryDto>

    // Обновление актива
    @PATCH("assets/{assetId}")
    suspend fun updateAsset(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: Int,
        @Body update: AssetUpdate
    ): AssetResponseDto
}