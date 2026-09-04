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
    private val assetApiService: AssetApiService
) : ViewModel() {

    val TAG = "AssetViewModel"
    sealed class AssetUiState {
        object Idle : AssetUiState()
        object Loading : AssetUiState()
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

    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    // StateFlow для хранения списка активов (для поиска по ID)
    private val _myAssetsList = MutableStateFlow<List<AssetResponseDto>>(emptyList())
    val myAssetsList: StateFlow<List<AssetResponseDto>> = _myAssetsList.asStateFlow()

    private val _assetStatuses = MutableStateFlow<List<AssetStatusDto>>(emptyList())
    val assetStatuses: StateFlow<List<AssetStatusDto>> = _assetStatuses.asStateFlow()

    private val _myPcsList = MutableStateFlow<List<MyPcDto>>(emptyList())
    val myPcsList: StateFlow<List<MyPcDto>> = _myPcsList.asStateFlow()

    private val _assetTypes = MutableStateFlow<List<AssetTypeDto>>(emptyList())
    val assetTypes: StateFlow<List<AssetTypeDto>> = _assetTypes.asStateFlow()

    // Добавляем отдельный StateFlow для UI-состояния инвентаризации
    private val _inventorizationUiState = MutableStateFlow<InventorizationUiState>(InventorizationUiState.Idle)
    val inventorizationUiState: StateFlow<InventorizationUiState> = _inventorizationUiState.asStateFlow()

    private val _inventorizationSessions = MutableStateFlow<List<InventorizationSessionDto>>(emptyList())
    val inventorizationSessions: StateFlow<List<InventorizationSessionDto>> = _inventorizationSessions.asStateFlow()

    private val _inventorizationItems = MutableStateFlow<List<InventorizationItemDto>>(emptyList())
    val inventorizationItems: StateFlow<List<InventorizationItemDto>> = _inventorizationItems.asStateFlow()

    private val _notificationItems = MutableStateFlow<List<NotificationDto>>(emptyList())
    val notificationItems: StateFlow<List<NotificationDto>> = _notificationItems.asStateFlow()

    private val _notificationUncheckedCount = MutableStateFlow(0)
    val notificationUncheckedCount: StateFlow<Int> = _notificationUncheckedCount.asStateFlow()

    private val _assetHistory = MutableStateFlow<List<AssetHistoryDto>>(emptyList())
    val assetHistory: StateFlow<List<AssetHistoryDto>> = _assetHistory.asStateFlow()

    private val _employees = MutableStateFlow<PaginatedResponse<EmployeeShortResponse>?>(null)
    val employees: StateFlow<PaginatedResponse<EmployeeShortResponse>?> = _employees.asStateFlow()

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
        return localStorage.getToken() ?: throw Exception("Отсутствует GPS токен авторизации. Выполните вход заново.")
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
            _uiState.value = AssetUiState.Loading
            try {
                val types = assetApiService.getAssetTypes("Bearer ${getToken()}")
                _assetTypes.value = types
                _uiState.value = AssetUiState.AssetTypesLoaded(types)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
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

    // Метод получения актива по ID:
    fun loadMyAssetDetails(assetId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val asset = assetApiService.getMyAssetById("Bearer ${getToken()}", assetId)
                _uiState.value = AssetUiState.MyAssetDetailsLoaded(asset)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
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
        page: Int = 1,
        pageSize: Int = 50,
        name: String? = null,
        inventoryId: String? = null,
        serialNumber: String? = null,
        assetStatus: String? = null,
        modelId: Int? = null,
        assetTypeId: Int? = null,
        parentId: Int? = null,
        locationId: Int? = null
    ) {
        viewModelScope.launch {
            if (page == 1) {
                _uiState.value = AssetUiState.Loading
            }
            try {
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
                    locationId = locationId
                )
                _uiState.value = AssetUiState.AssetsLoadedPaginated(
                    assets = response.items,
                    total = response.total,
                    page = response.page,
                    pageSize = response.pageSize,
                    totalPages = response.totalPages,
                    hasNext = response.hasNext,
                    hasPrevious = response.hasPrevious
                )
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки")
            }
        }
    }

    // Метод для загрузки деталей актива
    fun loadAssetDetails(assetId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val asset = assetApiService.getAssetById("Bearer ${getToken()}", assetId)
                _uiState.value = AssetUiState.AssetDetailsLoaded(asset)
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
                val items = assetApiService.getInventorizationSessionItems("Bearer ${getToken()}", sessionId)
                _inventorizationItems.value = items
                _uiState.value = AssetUiState.InventorizationItemsLoaded(items, sessionId)
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
    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {

                // Делаем ОДИН обычный запрос через Retrofit для получения начального списка
                val response = assetApiService.getNotifications("Bearer ${getToken()}")
//                Log.d(TAG, "loadNotifications: response = $response")

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
            .url("${com.gps.warehouse.utils.Constants.ASSET_URL}notifications/stream")
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

    override fun onCleared() {
        super.onCleared()
        eventSource?.cancel()
        Log.d(TAG, " onCleared: ViewModel уничтожен, SSE соединение разорвано")
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
        isActive: Boolean? = null,
        searchDepartment: String? = null,
        searchPosition: String? = null
    ) {
        viewModelScope.launch {
            if (page == 1) {
                _uiState.value = AssetUiState.Loading
            }
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
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки сотрудников")
            }
        }
    }
    // ================== Пользователи ==================
}