package com.gps.warehouse.data.remote.gps_dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MaterialDto(
    val id: String,
    val material: String,
    val qty: String,
    val name: String?,
    val status: String,
    // Поле для хранения отсканированного кода.
    // Оно не приходит с сервера в списке материалов, поэтому default null.
    var scannedCode: String? = null
) : Serializable

data class OrderMatRequest(
    @SerializedName("token") val token: String,
    @SerializedName("order") val order: String,
    @SerializedName("material") val material: String,
    @SerializedName("qty") val qty: String,
    @SerializedName("type") val type: String,      // "change" или "delete"
    @SerializedName("id_mat") val id_mat: String
)

data class OrderMatResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?
)

data class OrderWithMaterials(
    @SerializedName("norder") val norder: String,
    val materials: List<MaterialDto>
)

data class GetOrderMaterialsRequest(
    val token: String,
    val order: String,
    val type: String = "receive"
)

data class PackMaterialRequest(
    val token: String,          // Токен авторизации
    val material: String,       // Номер материала
    val qty: Int,               // Количество
    val code: String            // Уникальный код (штрихкод)
)

data class PackMaterialResponse(
    val status: String,
    val message: String
)

data class MaterialQty(
    val material: String,
    val qty: Int
)

// Класс для списка материалов на складе (getqrps)
data class WarehouseMaterialDto(
    @SerializedName("material") val material: String,               // Артикул материала
    @SerializedName("name") val name: String?,                      // Наименование
    @SerializedName("storage") val storage: String?,                // Склад/Ячейка
    @SerializedName("model") val model: String?,                    // Модель устройства
    @SerializedName("qty_packed") val qtyPacked: Int?,              // Упаковано всего
    @SerializedName("qty_packed_shift") val qtyPackedShift: Int?,   // Упаковано за смену
    @SerializedName("qty_dispatched") val qtyDispatched: Int?,      // Отгружено
    @SerializedName("qty_plan") val qtyPlan: Int?                   // План/Остаток
) : Serializable

// Request для получения списка материалов
data class GetWarehouseMaterialsRequest(
    val token: String,
    @SerializedName("num_sap") val numSap: String = "",             // Фильтр по SAP номеру (опционально)
    @SerializedName("name_s") val nameSearch: String = "",          // Поиск по имени (опционально)
    @SerializedName("s_start_date") val startDate: String,          // Дата начала (ISO формат)
    @SerializedName("s_end_date") val endDate: String               // Дата конца (ISO формат)
)


// ========================== Склады ==========================
// Запрос для получения данных WMS
data class GetWmsRequest(
    val token: String,
    @SerializedName("num_sap") val numSap: String = "",         // Поиск по артикулу
    @SerializedName("name_sap") val nameSap: String = "",       // Поиск по названию
    @SerializedName("stlo_pop") val stloPop: String = "",       // Фильтр по id склада *
    @SerializedName("is_hide_stock") val isHideStock: Int = 0,  // Скрыть нулевые остатки
    @SerializedName("page") val page: Int = 1,                  // Номер страницы
    @SerializedName("limit") val limit: Int = 20                // Явный лимит 20 записей
)

// Ответ от API getwms (один элемент списка)
data class WmsItemDto(
    val id: Int,
    val material: String,       // Артикул
    val max: Int,               // Максимальный остаток
    val min: Int,               // Минимальный остаток
    val position: String,       // Позиция (например, BUFF)
    val price: Double,          // Цена
    val qty: Double,            // Количество
    @SerializedName("sap_a") val sapA: Int,
    val storage: String,        // Склад
    @SerializedName("storage_id") val storageId: Int?, // id склада
    val name: String            // Наименование
) : Serializable

// Запрос на перемещение материала
data class MoveWmsRequest(
    val token: String,
    @SerializedName("id_pop") val idPop: String = "0",
    @SerializedName("material") val material: String = "",      // Пусто, т.к. используем move_material
    @SerializedName("stlo_pop") val stloPop: String = "",
    @SerializedName("name_pop") val namePop: String = "",
    @SerializedName("qty_pop") val qtyPop: String = "",
    @SerializedName("pos_pop") val posPop: String = "",
    @SerializedName("sap_a_pop") val sapAPop: Int = 0,
    @SerializedName("min_pop") val minPop: String = "",
    @SerializedName("max_pop") val maxPop: String = "",

    @SerializedName("move_material") val moveMaterial: String,  // Артикул
    @SerializedName("move_from") val moveFrom: String,          // Текущий склад
    @SerializedName("move_to") val moveTo: String,              // Целевой склад
    @SerializedName("move_qty") val moveQty: String,            // Количество
    @SerializedName("type") val type: String = "stock"
)

// Ответ от API movewms
data class MoveWmsResponse(
    val status: String, // success/error
    val message: String? = null
)

// DTO для складских запросов (getwmsrequests)
data class WmsRequestDto(
    val id: String,
    @SerializedName("from_id") val fromId: String,              // Кто создал
    @SerializedName("user_accept") val userAccept: String?,     // Кто принял (null если нет)
    val material: String,                                               // Артикул
    @SerializedName("from_storage") val fromStorage: String,    // Откуда
    @SerializedName("to_storage") val toStorage: String,        // Куда
    val qty: String,                                                    // Количество
    val name: String,                                                   // Наименование
    @SerializedName("is_active") val isActive: String,          // 1 - активен, 0 - выполнен/отменен
    val type: String,                                                   // тип (stock)
    @SerializedName("is_incoming") val isIncoming: String       // 0 - исходящий, 1 - входящий
) : Serializable

// Запрос для API getwmsrequests
data class GetWmsRequestsRequest(
    val token: String,
    @SerializedName("num_sap") val numSap: String = ""
)

// Действие со складским запросом (отмена/принятие)
data class WmsRequestAction(
    val token: String,
    @SerializedName("id_pop") val idPop: String,  // ID запроса
    val type: String  // "cancel" (отмена/отклонить) или "accept" (принять)
)

// Ответ от сервера на действие с запросом
data class WmsRequestActionResponse(
    val status: String,  // "success" или "error"
    val message: String?
)


data class GetNameMaterialRequest(
    val token: String,
    val material: String
)

data class GetNameMaterialResponse(
    val material: String,
    val name: String
)