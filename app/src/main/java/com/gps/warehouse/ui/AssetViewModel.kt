package com.gps.warehouse.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.gps.warehouse.data.local.LocalStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import com.gps.warehouse.data.remote.assets_dto.ApiErrorResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetHistoryDto
import com.gps.warehouse.data.remote.assets_dto.AssetStatusDto
import com.gps.warehouse.data.remote.assets_dto.AssetTransferRequestDto
import com.gps.warehouse.data.remote.assets_dto.AssetTransferResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetUpdate
import com.gps.warehouse.data.remote.assets_dto.CheckItemRequest
import com.gps.warehouse.data.remote.assets_dto.EmployeeShortResponse
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionCreateRequest
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.data.remote.assets_dto.NotificationDto
import com.gps.warehouse.data.remote.assets_dto.NotificationResponseDto
import com.gps.warehouse.data.remote.assets_dto.PaginatedResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONException
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.jvm.java

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val localStorage: LocalStorage,
    private val assetApiService: AssetApiService,
) : ViewModel() {

    val TAG = "AssetViewModel"
    sealed class AssetUiState {
        object Idle : AssetUiState()
        object Loading : AssetUiState()
        object SessionExpired : AssetUiState() // Явное состояние для истечения сессии
        data class MyAssetsLoaded(val assets: List<AssetResponseDto>) : AssetUiState()
        data class Error(val message: String) : AssetUiState()
        data class MyAssetDetailsLoaded(val asset: AssetResponseDto) : AssetUiState()
        data class MyPcsLoaded(val pcs: List<MyPcDto>) : AssetUiState()
        data class AssetTypesLoaded(val types: List<AssetTypeDto>) : AssetUiState()
        data class AssetDetailsLoaded(val asset: AssetResponseDto) : AssetUiState()

        // Пагинация для списка активов (из типов активов)
        data class AssetsLoadedPaginated(
            val assets: List<AssetResponseDto>,
            val total: Int,
            val page: Int,
            val pageSize: Int,
            val totalPages: Int,
            val hasNext: Boolean,
            val hasPrevious: Boolean
        ) : AssetUiState()

        data class InventorizationItemsLoaded(
            val items: List<InventorizationItemDto>,
            val sessionId: Int
        ) : AssetUiState()

        // ====================== Уведомления ======================
        data class NotificationsLoaded(val notifications: List<NotificationDto>) : AssetUiState()
        // =========================================================
    }

    sealed class InventorizationUiState {
        object Idle : InventorizationUiState()
        object Loading : InventorizationUiState()
        data class SessionsLoaded(val sessions: List<InventorizationSessionDto>) : InventorizationUiState()
        data class Error(val message: String) : InventorizationUiState()
    }

    sealed class AssetTypesUiState{
        object Idle : AssetTypesUiState()
        object Loading : AssetTypesUiState()
        data class Loaded(val assetTypes: List<AssetTypeDto>) : AssetTypesUiState()
        data class Error(val message: String) : AssetTypesUiState()
    }

    sealed class TransferStatus {
        object None : TransferStatus()  // Нет активных передач

        data class Outgoing(  // Я инициатор, ожидает ответа
            val notificationId: Int,
            val targetEmployeeName: String,
            val targetEmployeeId: String,
            val assignmentType: String,
            val comment: String?,
            val createdAt: String
        ) : TransferStatus()

        data class Incoming(  // Я получатель, ожидает моего ответа
            val notificationId: Int,
            val initiatorName: String,
            val initiatorId: String,
            val assetId: Int,
            val assetName: String,
            val assignmentType: String,
            val comment: String?,
            val createdAt: String
        ) : TransferStatus()
    }

    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val sessionMonitorScope = CoroutineScope(SupervisorJob() + viewModelScope.coroutineContext)

    // StateFlow для хранения списка активов (для поиска по ID)
    private val _myAssetsList = MutableStateFlow<List<AssetResponseDto>>(emptyList())
    val myAssetsList = _myAssetsList.asStateFlow()

    private val _assetStatuses = MutableStateFlow<List<AssetStatusDto>>(emptyList())
    val assetStatuses = _assetStatuses.asStateFlow()

    private val _myPcsList = MutableStateFlow<List<MyPcDto>>(emptyList())
    val myPcsList = _myPcsList.asStateFlow()

    private val _assetTypesUiState = MutableStateFlow<AssetTypesUiState>(AssetTypesUiState.Idle)
    val assetTypesUiState = _assetTypesUiState.asStateFlow()

    private val _assetTypes = MutableStateFlow<List<AssetTypeDto>>(emptyList())
    val assetTypes = _assetTypes.asStateFlow()

    private val _assetDetailsUiState = MutableStateFlow<List<AssetResponseDto>>(emptyList())
    val assetDetailUiState = _assetDetailsUiState.asStateFlow()

    // Добавляем отдельный StateFlow для UI-состояния инвентаризации
    private val _inventorizationUiState = MutableStateFlow<InventorizationUiState>(InventorizationUiState.Idle)
    val inventorizationUiState = _inventorizationUiState.asStateFlow()

    private val _inventorizationSessions = MutableStateFlow<List<InventorizationSessionDto>?>(null)
    val inventorizationSessions = _inventorizationSessions.asStateFlow()

    private val _inventorizationItems = MutableStateFlow<List<InventorizationItemDto>>(emptyList())
    val inventorizationItems = _inventorizationItems.asStateFlow()

    private val _notificationItems = MutableStateFlow<List<NotificationDto>>(emptyList())
    val notificationItems = _notificationItems.asStateFlow()

    private var currentFilterAssetId: Int? = null
    private var currentFilterSessionId: Int? = null

    private val _notificationUncheckedCount = MutableStateFlow(0)
    val notificationUncheckedCount = _notificationUncheckedCount.asStateFlow()

    private val _assetHistory = MutableStateFlow<List<AssetHistoryDto>>(emptyList())
    val assetHistory = _assetHistory.asStateFlow()

    private val _employees = MutableStateFlow<PaginatedResponse<EmployeeShortResponse>?>(null)
    val employees = _employees.asStateFlow()

    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.None)
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

    private var eventSource: EventSource? = null

    // Вспомогательная функция парсинга ошибок
    private fun getErrorMessage(e: Exception): String? {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                try {
                    // Пытаемся распарсить JSON вида {"detail": "Текст ошибки"}
                    val gson = Gson()
                    val errorResponse = gson.fromJson(errorBody, ApiErrorResponseDto::class.java)
                    return errorResponse.detail
                } catch (jsonEx: JSONException) {
                    // Если структура JSON другая, возвращаем стандартное сообщение
                }
            }
            return e.message() // Например, "HTTP 403 Forbidden", если body пустой
        }
        return e.message
    }

    // Получить текущий токен
    suspend fun getToken(): String {
        val token = localStorage.getToken()
        if (token.isNullOrEmpty()){
            _uiState.value = AssetUiState.SessionExpired
            throw Exception("Пользователь не авторизован. Автовыход.")
        }
        return token
    }

    // Метод получения ПК текущего пользователя
    fun loadMyPcs() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val pcs = assetApiService.getMyPcs("Bearer ${getToken()}")
                _myPcsList.value = pcs  // Сохраняем в отдельный поток
                _uiState.value = AssetUiState.MyPcsLoaded(pcs)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
            }
        }
    }

    // ================== Загрузка активов ==================
    // Типы активов
    fun loadAssetTypes() {
        viewModelScope.launch {
            _assetTypesUiState.value = AssetTypesUiState.Loading
            try {
                val types = assetApiService.getAssetTypes("Bearer ${getToken()}")
                _assetTypes.value = types
                _assetTypesUiState.value = AssetTypesUiState.Loaded(types)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
                _assetTypesUiState.value = AssetTypesUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
            }
        }
    }

    // Загрузка истории актива
    fun loadAssetHistory(assetId: Int) {
        viewModelScope.launch {
            try {
                val history = assetApiService.getAssetHistory("Bearer ${getToken()}", assetId)
                _assetHistory.value = history
            } catch (e: Exception) {
                // Логируем, но не показываем пользователю
            }
        }
    }

    // Обновление актива
    fun updateAsset(assetId: Int, update: AssetUpdate) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val updated = assetApiService.updateAsset("Bearer ${getToken()}", assetId, update)
                _uiState.value = AssetUiState.AssetDetailsLoaded(updated)
                loadAssetDetails(assetId)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка обновления")
            }
        }
    }

    // Метод загрузки статусов
    fun loadAssetStatuses() {
        viewModelScope.launch {
            try {
                val statuses = assetApiService.getAssetStatuses("Bearer ${getToken()}")
                _assetStatuses.value = statuses
            } catch (e: Exception) {
                // Логируем, но не показываем пользователю — фильтры могут работать и без статусов
                Log.e(TAG, "Ошибка загрузки статусов: ${e.message}")
            }
        }
    }

    // Получить мои активы
    fun loadMyAssets() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val assets = assetApiService.getMyAssignedAssets("Bearer ${getToken()}")
                _myAssetsList.value = assets  // Сохраняем список
                _uiState.value = AssetUiState.MyAssetsLoaded(assets)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
            }
        }
    }

    // Метод загрузки с фильтрами:
    fun loadAssetsByFilters(
        page: Int,
        pageSize: Int = 50,
        name: String? = null,
        inventoryId: String? = null,
        serialNumber: String? = null,
        assetStatus: String? = null,
        modelId: Int? = null,
        assetTypeId: Int? = null,
        parentId: Int? = null,
        locationId: Int? = null,
        onlyMy: Boolean = false
    ) {
        viewModelScope.launch {
//            if (page == 1) {
//                _uiState.value = AssetUiState.Loading
//            }

            try {
                // Если это не первая страница, сохраняем текущие активы из состояния
                val currentState = _uiState.value
                val oldAssets = if (currentState is AssetUiState.AssetsLoadedPaginated && page > 1) {
                    currentState.assets // Берем старые данные!
                } else {
                    emptyList() // Если страница 1, начинаем с пустого списка
                }

                // Делаем запрос
                val response = assetApiService.getAssets(
                    token = "Bearer ${getToken()}",
                    page = page,
                    pageSize = pageSize,
                    name = name,
                    inventoryId = inventoryId,
                    serialNumber = serialNumber,
                    assetStatus = assetStatus,
                    modelId = modelId,
                    assetTypeId = assetTypeId,
                    parentId = parentId,
                    locationId = locationId,
                    onlyMy = onlyMy
                )

                // ОБЪЕДИНЯЕМ старые и новые данные
                val updatedAssets = oldAssets + response.items

                // Обновляем состояние объединенным списком
                _uiState.value = AssetUiState.AssetsLoadedPaginated(
                    assets = updatedAssets,
                    total = response.total,
                    page = page,
                    pageSize = pageSize,
                    totalPages = response.totalPages,
                    hasNext = response.hasNext,
                    hasPrevious = response.hasPrevious
                )

            } catch (e: Exception) {
                // Если ошибка при пагинации, не стираем старые данные, просто показываем ошибку
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    /**
     * Ищет актив по отсканированному значению.
     * Так как мы не знаем, серийник это или инвентарник,
     * пробуем оба варианта последовательно.
     *
     * @return найденный актив или null
     */
    suspend fun findAssetByScanValue(scannedValue: String): AssetResponseDto? {
        return try {
            // Сначала пробуем как СЕРИЙНЫЙ номер
            Log.d("AssetViewModel", "Поиск по serialNumber: $scannedValue")
            val bySerial = assetApiService.getAssets(
                token = "Bearer ${getToken()}",
                serialNumber = scannedValue,
                page = 1,
                pageSize = 1
            )
            bySerial.items.firstOrNull()?.let {
                Log.d("AssetViewModel", "Найдено по серийнику: id=${it.assetId}")
                return it
            }

            // Если не найдено — пробуем как ИНВЕНТАРНЫЙ номер
            Log.d("AssetViewModel", "Поиск по inventoryId: $scannedValue")
            val byInventory = assetApiService.getAssets(
                token = "Bearer ${getToken()}",
                inventoryId = scannedValue,
                page = 1,
                pageSize = 1,
            )
            val found = byInventory.items.firstOrNull()
            if (found != null) {
                Log.d("AssetViewModel", "Найдено по инвентарнику: id=${found.assetId}")
            } else {
                Log.d("AssetViewModel", "Актив не найден ни по одному параметру")
            }
            found
        } catch (e: Exception) {
            Log.e("AssetViewModel", "Ошибка поиска актива: ${e.message}")
            null
        }
    }

    // Метод для загрузки деталей актива
    fun loadAssetDetails(assetId: Int? = null, materialId: String? = null) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                require(assetId != null || materialId != null) { "Должен быть указан assetId или materialId" }

                // Запрашиваем список с pageSize = 1 и нужными фильтрами
                val response = assetApiService.getAssets(
                    token = "Bearer ${getToken()}",
                    page = 1,
                    pageSize = 1, // Нам нужен только 1 конкретный элемент
                    assetId = assetId,
                    materialId = materialId
                )

                if (response.items.isNotEmpty()) {
                    val asset = response.items.first()
                    _uiState.value = AssetUiState.AssetDetailsLoaded(asset)

                    // Как только получили актив, извлекаем его assetId и загружаем историю
                    asset.assetId?.let { id ->
                        loadAssetHistory(id)
                    }
                } else {
                    _uiState.value = AssetUiState.Error("Актив не найден")
                }
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
            }
        }
    }
    // ================== Загрузка активов ==================

    // ================== Инвентаризация ==================
    fun loadInventorizationSessions() {
        viewModelScope.launch {
            _inventorizationUiState.value = InventorizationUiState.Loading
            try {
                val sessions = assetApiService.getInventorizationSessions("Bearer ${getToken()}")
                _inventorizationSessions.value = sessions  // Отдельный поток для данных
                _inventorizationUiState.value = InventorizationUiState.SessionsLoaded(sessions) // Отдельный поток для UI
            } catch (e: Exception) {
                _inventorizationUiState.value = InventorizationUiState.Error(getErrorMessage(e) ?: "Ошибка")
            }
        }
    }

    fun loadInventorizationItems(sessionId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                // response теперь имеет тип PaginatedResponse<InventorizationItemDto>
                val response = assetApiService.getInventorizationSessionItems("Bearer ${getToken()}", sessionId)

                // Сохраняем полный ответ (если нужно) или только список
                _inventorizationItems.value = response.items

                // Передаем именно список items в состояние
                _uiState.value = AssetUiState.InventorizationItemsLoaded(response.items, sessionId)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки элементов")
            }
        }
    }

    fun startInventorizationSession(assetTypeId: Int) {
        viewModelScope.launch {
            try {
                assetApiService.startInventorizationSession(
                    "Bearer ${getToken()}",
                    InventorizationSessionCreateRequest(assetTypeId)
                )
                loadInventorizationSessions() // Перезагружаем список
            } catch (e: Exception) {
                _inventorizationUiState.value = InventorizationUiState.Error(getErrorMessage(e) ?: "Ошибка создания")
            }
        }
    }

    fun checkInventorizationItem(sessionId: Int, assetId: Int, quantityFact: Int?) {
        viewModelScope.launch {
            try {
                val safeQuantity = quantityFact ?: 0

                assetApiService.checkInventorizationItem(
                    token = "Bearer ${getToken()}",
                    sessionId = sessionId,
                    request = CheckItemRequest(assetId, safeQuantity)
                )
                // Перезагружаем элементы
                loadInventorizationItems(sessionId)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка проверки актива")
            }
        }
    }

    fun completeInventorizationSession(sessionId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                assetApiService.completeInventorizationSession("Bearer ${getToken()}", sessionId)
                // Возвращаемся к списку сессий
                loadInventorizationSessions()
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка завершения сессии")
            }
        }
    }
    // ================== Инвентаризация ==================

    // ================== Уведомления ==================
    fun loadNotifications(assetId: Int? = null, sessionId: Int? = null) {
        viewModelScope.launch {
            currentFilterAssetId = assetId
            currentFilterSessionId = sessionId

            _uiState.value = AssetUiState.Loading
            try {

                // Делаем ОДИН обычный запрос через Retrofit для получения начального списка
                val response = assetApiService.getNotifications(
                    token = "Bearer ${getToken()}",
                    assetId = assetId,
                    sessionId = sessionId
                )

                // Сохраняем список в состояние
                _uiState.value = AssetUiState.NotificationsLoaded(response.items)

                _notificationItems.value = response.items
                _notificationUncheckedCount.value = response.uncheckedCount

                // Запускаем OkHttp SSE для прослушивания обновлений в реальном времени
                startSseStream(getToken())

            } catch (e: Exception) {
                Log.e(TAG, "loadNotifications: Ошибка загрузки начальных уведомлений", e)
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    fun readNotification(notificationId: Int){
        viewModelScope.launch {
            try {
                val responseReadNotification = assetApiService.readNotification("Bearer ${getToken()}", notificationId)
                Log.d(TAG, "Уведомление с id = ${responseReadNotification.notificationId} status=${responseReadNotification.status}")
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка чтения уведомления")
            }
        }
    }

    fun startSseStream(token: String) {
        eventSource?.cancel()

        // НАСТРАИВАЕМ ТАЙМАУТЫ: 0 означает "бесконечно", что критично для SSE
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)      // Ждать данные от сервера бесконечно
            .writeTimeout(0, TimeUnit.SECONDS)     // Бесконечный таймаут на запись
            .connectTimeout(10, TimeUnit.SECONDS)  // На подключение даем стандартные 10 сек
            .build()

        val request = Request.Builder()
            // param: direction=all || outgoing || incoming
            .url("${com.gps.warehouse.utils.Constants.ASSET_URL}notifications/stream?direction=incoming")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream") // Обязательно для SSE
            .build()

        Log.d(TAG, "startSseStream: Попытка подключения к SSE потоку...")

        eventSource = EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.d(TAG, "startSseStream: Получены сырые данные SSE (длина: ${data.length})")
                try {
                    val gson = Gson()
                    val responseDto = gson.fromJson(data, NotificationResponseDto::class.java)

                    Log.d(TAG, "startSseStream: Успешно распарсено. Source: ${responseDto.source}, Элементов: ${responseDto.items.size}")

                    val currentState = _uiState.value
                    if (currentState is AssetUiState.NotificationsLoaded) {
                        val currentList = currentState.notifications.toMutableList()

                        // Обновляем существующие или добавляем новые
                        for (incoming in responseDto.items) {
                            // ЗАЩИТА: Если мы смотрим конкретный актив или сессию,
                            // игнорируем SSE-уведомления, которые к ним не относятся
                            if (currentFilterAssetId != null && incoming.assetId != currentFilterAssetId) continue
                            if (currentFilterSessionId != null && incoming.sessionId != currentFilterSessionId) continue


                            val existingIndex = currentList.indexOfFirst { it.notificationId == incoming.notificationId }
                            if (existingIndex != -1) {
                                val existing = currentList[existingIndex]
                                currentList[existingIndex] = existing.copy(
                                    status = incoming.status.ifEmpty { existing.status },
                                    statusRu = incoming.statusRu.ifEmpty { existing.statusRu },
                                    respondedAt = incoming.respondedAt ?: existing.respondedAt
                                )
                            } else {
                                currentList.add(0, incoming)
                            }
                        }
                        _uiState.value = AssetUiState.NotificationsLoaded(currentList)
                        Log.d(TAG, "startSseStream: Список уведомлений обновлен. Всего: ${currentList.size}")
                    } else {
                        _uiState.value = AssetUiState.NotificationsLoaded(responseDto.items)
                        Log.d(TAG, "startSseStream: Список уведомлений инициализирован из SSE. Всего: ${responseDto.items.size}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "startSseStream: Ошибка парсинга JSON из SSE: ${e.message}", e)
                    Log.e(TAG, "startSseStream: Проблемная строка data: $data")
                }
            }

            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                Log.d(TAG, "SSE поток закрыт сервером штатно")
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val code = response?.code
                val errorBody = try { response?.body?.string() } catch (e: Exception) { "Не удалось прочитать" }
                Log.e(TAG, "Ошибка SSE соединения. HTTP Код: $code, Тело: $errorBody, Exception: ${t?.message}")
            }
        })
    }
    // ================== Уведомления ==================

    // ================== Пользователи ==================
    // Загрузка списка сотрудников с фильтрами
    fun loadEmployees(
        page: Int = 1,
        pageSize: Int = 50,
        employeeId: String? = null,
        lastName: String? = null,
        firstName: String? = null,
        middleName: String? = null,
        lastNameEn: String? = null,
        firstNameEn: String? = null,
        middleNameEn: String? = null,
        departmentGuid: String? = null,
        positionGuid: String? = null,
        isActive: Boolean? = true,
        searchDepartment: String? = null,
        searchPosition: String? = null
    ) {
        viewModelScope.launch {
            try {
                val response = assetApiService.getEmployees(
                    token = "Bearer ${getToken()}",
                    page = page,
                    pageSize = pageSize,
                    employeeId = employeeId,
                    lastName = lastName,
                    firstName = firstName,
                    middleName = middleName,
                    lastNameEn = lastNameEn,
                    firstNameEn = firstNameEn,
                    middleNameEn = middleNameEn,
                    departmentGuid = departmentGuid,
                    positionGuid = positionGuid,
                    isActive = isActive,
                    searchDepartment = searchDepartment,
                    searchPosition = searchPosition
                )
                _employees.value = response
            } catch (e: Exception) {
//                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки сотрудников")
                Log.e(TAG, "Ошибка загрузки сотрудников: ${e.message}")
                _employees.value = null
            }
        }
    }
    // ================== Пользователи ==================

    // ================== Передача актива ==================
    fun requestAssetTransfer(
        assetId: Int? = null,
        materialId: String? = null,
        targetEmployeeId: String,
        assignmentType: String = "user",
        comment: String? = null,
        onSuccess: (AssetTransferResponseDto) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                // TODO: Сделать проверку только лишь на одни id:
                //  * asset_id и material_id
                var request: AssetTransferRequestDto

                if (assetId != null){
                    request = AssetTransferRequestDto(
                        assetId = assetId,
                        targetEmployeeId = targetEmployeeId,
                        assignmentType = assignmentType,
                        comment = comment
                    )
                }
                else {
                    request = AssetTransferRequestDto(
                        materialId = materialId,
                        targetEmployeeId = targetEmployeeId,
                        assignmentType = assignmentType,
                        comment = comment
                    )
                }

                val response = assetApiService.requestAssetTransfer(
                    token = "Bearer ${getToken()}",
                    request = request
                )

                Log.d(TAG, "Передача актива успешна: ${response.message}")
                onSuccess(response)

                // Перезагружаем детали актива после успешной передачи
                loadAssetDetails(assetId = assetId)

            } catch (e: Exception) {
                val errorMessage = getErrorMessage(e) ?: "Ошибка передачи актива"
                Log.e(TAG, "Ошибка передачи актива: $errorMessage")
                onError(errorMessage)
                _uiState.value = AssetUiState.Error(errorMessage)
            }
        }
    }
    // ================== Передача актива ==================
}