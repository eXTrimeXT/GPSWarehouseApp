package com.gps.warehouse.data.remote

import com.gps.warehouse.data.remote.gps_dto.*
import com.gps.warehouse.utils.Constants.BASE_URL_API
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface GPSApiService {
    // Получение публичного ключа шифрования
    @GET("getkey")
    suspend fun getPublicKey(): String

    // Авторизация
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // Список заказов / Архив заказов
    @POST("getordersps")
    suspend fun getOrders(@Body request: GetOrdersRequest): List<OrderDto>

    // Создание заказа
    @POST("makeorderps")
    suspend fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse

    // Материалы конкретного заказа
    @POST("getordersps")
    suspend fun getOrderMaterials(@Body request: GetOrderMaterialsRequest): List<OrderWithMaterials>

    // Упаковка материала
    @POST("matdoneps")
    suspend fun packMaterial(@Body request: PackMaterialRequest): PackMaterialResponse

    // Список материалов на складе / Доступные материалы
    @POST("getqrps")
    suspend fun getWarehouseMaterials(@Body request: GetWarehouseMaterialsRequest): List<WarehouseMaterialDto>

    // Получение информации о пользователе
    @POST("getinfouser")
    suspend fun getUserProfile(@Body request: GetUserProfileRequest): UserProfileResponse


    // ====================== Инвентаризация ======================
    // Инвентаризация: Получить список заказов
    @POST("inv_order/get")
    suspend fun getInventoryOrders(@Body request: GetInventoryOrdersRequest): List<InventoryOrderDto>

    // Инвентаризация: Получить материалы заказа
    @POST("inv_order_view")
    suspend fun getInventoryMaterials(@Body request: GetInventoryMaterialsRequest): List<InventoryMaterialDto>

    // Инвентаризация: Сверить материал
    @POST("inv_mob_set")
    suspend fun checkInventoryMaterial(@Body request: CheckInventoryMaterialRequest): List<CheckInventoryApiResponse>

    // Инвентаризация: Завершить заказ
    @POST("inv_order_set")
    suspend fun finishInventoryOrder(@Body request: FinishInventoryRequest): List<FinishInventoryResponse>

    // Получение данных WMS (Склады)
    @POST("getwms")
//    suspend fun getWmsData(@Body request: GetWmsRequest): List<WmsItemDto>
    suspend fun getWmsData(@Body request: GetWmsRequest): WmsResponseDto

    // Перемещение материала между складами
    @POST("movewms")
    suspend fun moveWms(@Body request: MoveWmsRequest): MoveWmsResponse

    // Получение складских запросов
    @POST("getwmsrequests")
    suspend fun getWmsRequests(@Body request: GetWmsRequestsRequest): List<WmsRequestDto>

    // Запрос на изменение или удаление материала из заказа
    @POST("order_mats")
    @Headers(
        "Content-Type: application/json",
        "X-KL-kes-Ajax-Request: Ajax_Request",
        "X-Requested-With: XMLHttpRequest",
        "Origin: $BASE_URL_API"
    )
    suspend fun updateOrderMaterial(
        @Body request: OrderMatRequest,
    ): ResponseBody

    // Приемка материалов (вкладка Склад)
    @POST("wms_set_mat_order")
    suspend fun receiveWmsMaterials(@Body request: WmsReceiveRequest): ResponseBody

    // ====================== Складские запросы: действия ======================

    // Отмена исходящего запроса или отклонение входящего
    @POST("actwmsrequests")
    @Headers(
        "Content-Type: application/json",
        "X-KL-kes-Ajax-Request: Ajax_Request",
        "X-Requested-With: XMLHttpRequest",
        "Origin: $BASE_URL_API"
    )
    suspend fun cancelWmsRequest(@Body request: WmsRequestAction): WmsRequestActionResponse

    // Принятие входящего запроса
    @POST("actwmsrequests")
    @Headers(
        "Content-Type: application/json",
        "X-KL-kes-Ajax-Request: Ajax_Request",
        "X-Requested-With: XMLHttpRequest",
        "Origin: $BASE_URL_API"
    )
    suspend fun acceptWmsRequest(@Body request: WmsRequestAction): WmsRequestActionResponse

     // Списание материалов со склада
    @POST("wms_write_off")
    suspend fun writeOffMaterials(@Body request: WmsWriteOffRequest): ResponseBody

    @POST("get_info_material")
    suspend fun getNameMaterial(@Body request: GetNameMaterialRequest): GetNameMaterialResponse
}