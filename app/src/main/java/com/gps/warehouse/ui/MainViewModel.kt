package com.gps.warehouse.ui

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.TokenStorage
import com.gps.warehouse.data.remote.GPSApiService
import com.gps.warehouse.data.remote.gps_dto.*
import com.gps.warehouse.utils.AppThemeMode
import com.gps.warehouse.utils.Constants.SESSION_DURATION_MS
import com.gps.warehouse.utils.RsaUtils.encryptPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val apiService: GPSApiService,
) : ViewModel() {

    sealed class UiState {

        // ====================== Состояния ======================
        object Idle : UiState()
        object Loading : UiState()
        object SessionExpired : UiState() // Явное состояние для истечения сессии
        data class LoggedIn(val token: String) : UiState()
        data class Error(val message: String) : UiState()
        // ====================== Состояния ======================


        // ====================== Заказы ======================
        data class OrdersLoaded(val orders: List<OrderDto>) : UiState()
        data class ArchiveLoaded(val orders: List<OrderDto>) : UiState()
        // ====================== Заказы ======================


        // ====================== Материалы ======================
        data class MaterialsLoaded(val materials: List<MaterialDto>) : UiState()
        data class WarehouseMaterialsLoaded(val materials: List<WarehouseMaterialDto>) : UiState()
        // ====================== Материалы ======================


        // ====================== Профиль ======================
        data class ProfileLoaded(
            val profile: UserProfileResponse,
        ) : UiState()
        // ====================== Профиль ======================


        // ====================== Упаковка/Приемка материала ======================
        data class Packed(val message: String) : UiState()
        data class OrderCreatedAndReadyForReceive(val orderNumber: String) : UiState()
        // ====================== Упаковка/Приемка материала ======================

        // ====================== Инвентаризация ======================
        data class InventoryMaterialsLoaded(
            val materials: List<InventoryMaterialDto>,
            val orderNumber: String
        ) : UiState()

        data class InventoryFinished(val message: String) : UiState()
        data class InventoryOrdersLoaded(val orders: List<InventoryOrderDto>) : UiState()
        // ====================== Инвентаризация ======================

        // ====================== WMS / Склады ======================
        data class WmsLoaded(val items: List<WmsItemDto>) : UiState()

        // Состояние для успеха перемещения
        data class WmsMoveSuccess(val message: String?) : UiState()

        // Состояние для складского запроса
        data class WmsRequestsLoaded(val requests: List<WmsRequestDto>) : UiState()
        // ====================== WMS / Склады ======================

        // ====================== Приемка WMS ======================
        data class WmsReceiveSuccess(val message: String) : UiState()
        // ====================== Списание материалов WMS ======================
        data class WmsWriteOffSuccess(val message: String) : UiState()
        // ========================================================================

        // ====================== Складские запросы: действия ======================
        data class WmsRequestCancelled(val message: String) : UiState()
        data class WmsRequestAccepted(val message: String) : UiState()
        // ========================================================================
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)

    // Свойство для фонового мониторинга сессии
    private val sessionMonitorScope = CoroutineScope(SupervisorJob() + viewModelScope.coroutineContext)

    val uiState = _uiState.asStateFlow()

    private var currentToken: String? = null
    private var currentLogin: String? = null // Сохраняем логин при успешном входе
    var currentInventoryOrder: String? = null
    var currentInventoryWarehouse: String? = null

    // ================== Пагинация ==================
    private var wmsCurrentPage = 1
    private var wmsTotalPages = 1
    private var totalMaterialsCount = 0

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Реактивное свойство для UI
    val canLoadMore: Boolean get() = wmsCurrentPage < wmsTotalPages && !_isLoadingMore.value
    val totalMaterials: Int get() = totalMaterialsCount
    // ================================================

    // Параметры фильтрации (сохраняются между запросами)
    private var currentStorageFilterId: String? = null
    private var currentWmsSearchQuery: String = ""
    private var currentHideZeroQty: Boolean = false

    private val _availableWarehouses = MutableStateFlow<List<WarehousePermissionDto>>(emptyList())
    val availableWarehouses: StateFlow<List<WarehousePermissionDto>> = _availableWarehouses.asStateFlow()

    // Флаг для инвентаризации
    var isInventoryActive: Boolean = true

    // Поток для темы
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            // Собираем поток токена
            tokenStorage.tokenFlow.collect { token ->
                isSessionValid(token)
            }
            tokenStorage.themeModeFlow.collect { mode ->
                _themeMode.value = mode
            }
        }
        // Запускаем периодическую проверку сессии при инициализации
        startSessionMonitoring()
    }
//
//    // Метод для смены темы
//    fun updateThemeMode(mode: AppThemeMode) {
//        viewModelScope.launch {
//            tokenStorage.saveThemeMode(mode)
//        }
//    }

    /**
     * Запускает фоновую задачу, которая каждые N минут проверяет,
     * не истекла ли сессия по абсолютному времени (utils Constants.SESSION_DURATION_MS с момента входа)
     */
    private fun startSessionMonitoring() {
        sessionMonitorScope.launch {
            while (true) {
                delay(SESSION_DURATION_MS) // Проверка каждые N минут

                val token = tokenStorage.getToken()
                val timestamp = tokenStorage.getLoginTimestamp()

                if (token != null && timestamp != null) {
                    val currentTime = System.currentTimeMillis()
                    if ((currentTime - timestamp) > SESSION_DURATION_MS) {
                        Log.d(
                            "MainViewModel",
                            "Сессия истекла по таймауту. Выполняется автоматический выход."
                        )
                        logout()
                        exitProcess(1)
                    }
                }
            }
        }
    }


    /**
     * Проверяет, валидность времени сессии по токену
     *
     * Если сессия истекла, то автоматически выходим
     *
     * Иначе сохраняем состояние входа
     */
    private suspend fun isSessionValid(token: String?): Boolean {
        // Если нет токена или он пустой - выходим и меняем состояние на авторизацию
        if (token.isNullOrEmpty()) {
            currentToken = null
            _uiState.value = UiState.Idle
            return false
        }

        // Если времени нет, считаем сессию невалидной
        val timestamp = tokenStorage.getLoginTimestamp() ?: return true

        val currentTime = System.currentTimeMillis()
        if ((currentTime - timestamp) > SESSION_DURATION_MS) {
            Log.d("MainViewModel", "Сессия истекла. Выполняется выход.")
            logout() // Автоматический выход
            _uiState.value = UiState.SessionExpired // Устанавливаем явное состояние
            return false
        } else {
            currentToken = token
            if (currentLogin.isNullOrEmpty()) {
                currentLogin = extractLoginFromToken(token)
                Log.d("MainViewModel", "Login extracted from token: $currentLogin")
            }
            _uiState.value = UiState.LoggedIn(token)
            return true
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // 1. Получаем публичный ключ от GPS API
                val publicKey = apiService.getPublicKey()
                val encryptedPassword = encryptPassword(password, publicKey)

                // 2. Логинимся в GPS API
                val gpsResponse = apiService.login(LoginRequest(username, encryptedPassword))

                // 3. Проверяем статус ответа
                if (gpsResponse.status == "success" && gpsResponse.data != null) {
                    val gpsToken = gpsResponse.data.token

                    // 4. Сохраняем токен
                    tokenStorage.saveToken(gpsToken)
                    currentToken = gpsToken
                    currentLogin = username

                    _uiState.value = UiState.LoggedIn(gpsToken)
                } else {
                    // Ошибка авторизации - показываем сообщение от сервера
                    _uiState.value = UiState.Error(gpsResponse.msg.ifEmpty { "Ошибка авторизации" })
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка входа: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    // Метод для сброса состояния (например, при уходе с экрана)
    fun resetStateToIdle() {
        _uiState.value = UiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            tokenStorage.clearToken()
            currentToken = null
            _uiState.value = UiState.Idle
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 1. Загружаем профиль из GPS API
                val gpsProfile = apiService.getUserProfile(GetUserProfileRequest(getTokenOrThrow()))
                val storages = gpsProfile.warehousePermissions
                if (storages != null) {
                    _availableWarehouses.value = storages
                }

                _uiState.value = UiState.ProfileLoaded(gpsProfile)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки профиля", e)
                _uiState.value = UiState.Error(e.message ?: "Не удалось загрузить профиль")
            }
        }
    }

    fun loadAvailableWarehouses() {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                // Загружаем через GPS API из профиля (не Assets API)
                val profile = apiService.getUserProfile(GetUserProfileRequest(token))
                val storages = profile.warehousePermissions ?: emptyList()
                _availableWarehouses.value = storages.map { it } // или как у вас в DTO
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки складов", e)
            }
        }
    }

    /**
     * Выполняет перемещение материала
     */
    fun moveWmsMaterial(material: String, fromStorage: String, toStorage: String, qty: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = MoveWmsRequest(
                    token = token,
                    moveMaterial = material,
                    moveFrom = fromStorage,
                    moveTo = toStorage,
                    moveQty = qty.toString()
                )
                val response = apiService.moveWms(request)
                Log.d("MainViewModel", "response.status ${response.status}")
                if (response.status == "success" || response.status == "ok") {
                    _uiState.value =
                        UiState.WmsMoveSuccess(response.message ?: "Перемещение успешно")
                } else {
                    _uiState.value = UiState.Error(response.message ?: "Ошибка перемещения")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка перемещения: $e")
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети или сервера")
            }
        }
    }

    /**
     * Извлекает логин из JWT токена
     */
    private fun extractLoginFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                // Декодируем Payload (вторую часть)
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                Log.i("PAYLOAD:", payload)
                val json = JSONObject(payload)
                Log.i("JSON:", json.toString())
                json.optString("login", null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error parsing JWT", e)
            null
        }
    }

    fun loadWmsRequests() {
        executeRequest(
            request = {
                apiService.getWmsRequests(GetWmsRequestsRequest(getTokenOrThrow()))
            },
            onSuccess = { allRequests ->
                // Фильтруем запросы по текущему пользователю
                val userLogin = currentLogin
                Log.d("MainViewModel", "Filtering WMS requests for user: $userLogin")

                val filteredRequests = if (userLogin != null) {
                    allRequests.filter { it.fromId == userLogin }
                } else {
                    // Если логин неизвестен, возвращаем пустой список или все (лучше пустой, чтобы не показывать чужие)
//                    Log.w("MainViewModel", "Current login is null, returning empty list")
//                    emptyList()
                }

                _uiState.value = UiState.WmsRequestsLoaded(allRequests)
            },
            errorMsg = "Ошибка загрузки запросов"
        )
    }

    /**
     * Отменяет исходящий запрос или отклоняет входящий
     */
    fun cancelWmsRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = WmsRequestAction(token = token, idPop = requestId, type = "cancel")
                val response = apiService.cancelWmsRequest(request)

                if (response.status == "success" || response.status == "ok") {
                    _uiState.value = UiState.WmsRequestCancelled(response.message ?: "Запрос отменён")
                    loadWmsRequests() // Обновляем список
                } else {
                    _uiState.value = UiState.Error(response.message ?: "Ошибка отмены запроса")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка отмены запроса: $e")
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    /**
     * Принимает входящий складской запрос
     */
    fun acceptWmsRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = WmsRequestAction(token = token, idPop = requestId, type = "accept")
                val response = apiService.acceptWmsRequest(request)

                if (response.status == "success" || response.status == "ok") {
                    _uiState.value = UiState.WmsRequestAccepted(response.message ?: "Запрос принят")
                    loadWmsRequests() // Обновляем список
                } else {
                    _uiState.value = UiState.Error(response.message ?: "Ошибка принятия запроса")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка принятия запроса: $e")
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    /**
     * Сбрасывает состояния действий с запросами
     */
    fun resetWmsRequestActionState() {
        if (_uiState.value is UiState.WmsRequestCancelled ||
            _uiState.value is UiState.WmsRequestAccepted) {
            _uiState.value = UiState.Idle
        }
    }

    // Метод загрузки данных из складов
    fun loadWmsData(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch {
            if (page == 1) {
                _uiState.value = UiState.Loading
            } else if (_isLoadingMore.value) return@launch // Защита от дублей

            _isLoadingMore.value = true // Старт загрузки

            try {
                val token = getTokenOrThrow()
                val request = GetWmsRequest(
                    token = token,
                    numSap = currentWmsSearchQuery,
                    nameSap = "",
                    stloPop = currentStorageFilterId ?: "",
                    isHideStock = if (currentHideZeroQty) 1 else 0,
                    page = page,
                    limit = 20
                )

                val response = apiService.getWmsData(request)

                if (response.data.isNotEmpty()) {
                    val newItems = response.data
                    if (append) {
                        val currentList = when (val state = _uiState.value) {
                            is UiState.WmsLoaded -> state.items
                            else -> emptyList()
                        }
                        _uiState.value = UiState.WmsLoaded(currentList + newItems)
                    } else {
                        _uiState.value = UiState.WmsLoaded(newItems)
                    }
                    // Обновляем состояние пагинации
                    wmsCurrentPage = response.page
                    wmsTotalPages = response.pageQty
                    totalMaterialsCount = response.materialsCount
                } else if (page == 1) {
                    _uiState.value = UiState.Error("Нет данных от сервера")
                }
            } catch (e: retrofit2.HttpException) {
                if (page == 1) _uiState.value = UiState.Error("Ошибка сервера: ${e.code()}")
            } catch (e: Exception) {
                if (page == 1) _uiState.value = UiState.Error(e.message ?: "Ошибка сети")
            } finally {
                _isLoadingMore.value = false // Сброс флага в любом случае
            }
        }
    }

    fun loadMoreWmsData() {
        if (canLoadMore) {
            loadWmsData(page = wmsCurrentPage + 1, append = true)
        }
    }

    // Методы для обновления фильтров с перезагрузкой данных
    fun updateWmsFilters(
        storageId: String?,
        searchQuery: String,
        hideZeroQty: Boolean
    ) {
        currentStorageFilterId = storageId
        currentWmsSearchQuery = searchQuery
        currentHideZeroQty = hideZeroQty
        wmsCurrentPage = 1 // Сбрасываем на первую страницу при изменении фильтров
        loadWmsData(page = 1, append = false)
    }

    // Метод для серверного поиска по QR (артикул)
    fun searchWmsByMaterial(materialCode: String) {
        currentWmsSearchQuery = materialCode
        wmsCurrentPage = 1
        loadWmsData(page = 1, append = false)
    }

    // Сброс состояния
    fun resetWmsState() {
        wmsCurrentPage = 1
        wmsTotalPages = 1
        currentStorageFilterId = null
        currentWmsSearchQuery = ""
        currentHideZeroQty = false
    }

    fun loadOrders() {
        loadOrdersGeneric(type = "status", isArchive = false)
    }

    fun loadArchive() {
        loadOrdersGeneric(type = "archive", isArchive = true)
    }

    private fun loadOrdersGeneric(type: String, isArchive: Boolean) {
        executeRequest(
            request = {
                apiService.getOrders(GetOrdersRequest(getTokenOrThrow(), type, null))
            },
            onSuccess = { orders ->
                _uiState.value =
                    if (isArchive) UiState.ArchiveLoaded(orders) else UiState.OrdersLoaded(orders)
            },
            errorMsg = "Не удалось загрузить заказы"
        )
    }


    // В MainViewModel.kt добавить:

    /**
     * Отправляет данные о списанных материалах на сервер
     */
    fun writeOffWmsMaterials(materials: List<WmsWriteOffItem>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = WmsWriteOffRequest(token = token, materialsData = materials)

                // Получаем "сырой" ответ
                val responseBody = apiService.writeOffMaterials(request)
                val responseString = responseBody.string().trim()

                Log.d("MainViewModel", "WMS WriteOff raw response: $responseString")

                // === ОБРАБОТКА ОТВЕТА ===

                // 1. Проверяем на простые текстовые успехи
                if (responseString.equals("ok", ignoreCase = true) ||
                    responseString.equals("success", ignoreCase = true)) {
                    _uiState.value = UiState.WmsWriteOffSuccess("Списание успешно завершено")
                    return@launch
                }

                // 2. Пытаемся распарсить как JSON
                if (responseString.startsWith("{")) {
                    try {
                        val json = org.json.JSONObject(responseString)
                        val status = json.optString("status", "")
                        val message = json.optString("message", json.optString("msg", ""))

                        if (status.equals("success", ignoreCase = true) || status.equals("ok", ignoreCase = true)) {
                            _uiState.value = UiState.WmsWriteOffSuccess(message.ifEmpty { "Списание успешно завершено" })
                        } else {
                            _uiState.value = UiState.Error(message.ifEmpty { "Ошибка сервера" })
                        }
                    } catch (e: org.json.JSONException) {
                        _uiState.value = UiState.Error("Ошибка парсинга ответа сервера")
                    }
                } else {
                    // Это plain text. Проверяем, не ошибка ли это
                    val errorMsg = if (responseString.contains("SQLSTATE", ignoreCase = true) ||
                        responseString.contains("error", ignoreCase = true) ||
                        responseString.contains("exception", ignoreCase = true)) {
                        "Ошибка сервера: ${responseString.take(150)}"
                    } else {
                        _uiState.value = UiState.WmsWriteOffSuccess(responseString.ifEmpty { "Списание успешно завершено" })
                        return@launch
                    }
                    _uiState.value = UiState.Error(errorMsg)
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка списания WMS: $e")
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети или сервера")
            }
        }
    }

    /**
     * Сбрасывает состояние после успешного списания
     */
    fun resetWriteOffState() {
        if (_uiState.value is UiState.WmsWriteOffSuccess) {
            _uiState.value = UiState.Idle
        }
    }

    /**
     * Обновление количества материала (type=change)
     */
    fun changeMaterialOnServer(
        order: String,
        material: String,
        qty: Int,
        idMat: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                val request = OrderMatRequest(
                    token = token,
                    order = order,
                    material = material,
                    qty = qty.toString(),
                    type = "change",
                    id_mat = idMat
                )

                val responseBody = apiService.updateOrderMaterial(
                    request = request,
                    referer = "http://gps-test.hmmr.ru/m_mat_status/$order"
                )

                val responseString = responseBody.string().trim()
                Log.d("MainViewModel", "Change response: $responseString")

                handleOrderMatResponse(responseString, order, onSuccess, onError)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка changeMaterialOnServer", e)
                onError(e.message ?: "Ошибка сети")
            }
        }
    }

    /**
     * Удаление материала (type=delete)
     */
    fun deleteMaterialFromServer(
        order: String,
        material: String,
        idMat: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                val request = OrderMatRequest(
                    token = token,
                    order = order,
                    material = material,
                    qty = "0",  // не используется при delete
                    type = "delete",
                    id_mat = idMat
                )

                val responseBody = apiService.updateOrderMaterial(
                    request = request,
                    referer = "http://gps-test.hmmr.ru/m_mat_status/$order"
                )

                val responseString = responseBody.string().trim()
                Log.d("MainViewModel", "Delete response: $responseString")

                handleOrderMatResponse(responseString, order, onSuccess, onError)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка deleteMaterialFromServer", e)
                onError(e.message ?: "Ошибка сети")
            }
        }
    }

    /**
     * Добавление НОВОГО материала в заказ через API (type=change, id_mat="0")
     */
    fun addNewMaterialToOrder(
        order: String,
        material: String,
        qty: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()

                // Формируем request-объект для отправки в теле запроса как JSON
                val request = OrderMatRequest(
                    token = token,
                    order = order,
                    material = material,
                    qty = qty.toString(),
                    type = "add",  // сервер создаст запись, если id_mat = "0"
                    id_mat = "0"      // или "" — зависит от поведения API
                )

                // Вызываем API с request в теле и динамическим Referer в заголовке
                val responseBody = apiService.updateOrderMaterial(
                    request = request,
                    referer = "http://gps-test.hmmr.ru/m_mat_status/$order"
                )

                val responseString = responseBody.string().trim()
                Log.d("MainViewModel", "Add new material response: $responseString")

                // Обрабатываем ответ через общий хелпер
                handleOrderMatResponse(responseString, order, onSuccess, onError)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка addNewMaterialToOrder", e)
                onError(e.message ?: "Ошибка сети")
            }
        }
    }

    /**
     * Общий обработчик ответа от order_mats (вынесите в отдельный private метод)
     */
    private fun handleOrderMatResponse(
        responseString: String,
        orderNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        when {
            responseString.contains("success", ignoreCase = true) ||
                    responseString.contains("успешно", ignoreCase = true) ||
                    responseString.contains("изменен", ignoreCase = true) ||
                    responseString.contains("добавлен", ignoreCase = true) -> {
                loadMaterials(orderNumber)  // перезагружаем список
                onSuccess()
            }

            responseString.contains("Доступ запрещен", ignoreCase = true) -> {
                onError("Доступ запрещён. Проверьте права и токен.")
            }

            responseString.startsWith("{") -> {
                try {
                    val json = JSONObject(responseString)
                    val status = json.optString("status", "")
                    val message = json.optString("message", json.optString("msg", ""))
                    if (status.equals("success", ignoreCase = true)) {
                        loadMaterials(orderNumber)
                        onSuccess()
                    } else {
                        onError(message.ifEmpty { "Ошибка сервера" })
                    }
                } catch (e: Exception) {
                    onError(responseString.ifEmpty { "Неизвестная ошибка" })
                }
            }

            else -> {
                onError(responseString.ifEmpty { "Неизвестная ошибка" })
            }
        }
    }


    // --- Логика управления списком материалов в заказе (Receive) ---
    /**
     * Обновляет количество материала в локальном списке материалов заказа.
     * Используется при редактировании перед отправкой на сервер.
     */
//    fun updateMaterialQuantity(materialArticle: String, newQty: Int) {
//        val currentState = _uiState.value
//        if (currentState is UiState.MaterialsLoaded) {
//            val updatedMaterials = currentState.materials.map {
//                if (it.material == materialArticle) {
//                    it.copy(qty = newQty.toString())
//                } else {
//                    it
//                }
//            }
//            _uiState.value = UiState.MaterialsLoaded(updatedMaterials)
//        }
//    }

    /**
     * Добавляет новый материал в список или обновляет существующий.
     * Если материал есть: увеличивает qty и ОБНОВЛЯЕТ scannedCode.
     */
//    fun addOrUpdateMaterialInOrder(
//        material: String,
//        name: String?,
//        qty: Int,
//        code: String? = null
//    ) {
//        val currentState = _uiState.value
//        if (currentState is UiState.MaterialsLoaded) {
//            val existingIndex = currentState.materials.indexOfFirst { it.material == material }
//
//            if (existingIndex != -1) {
//                // Материал уже есть.
//                // 1. Увеличиваем количество.
//                // 2. Обновляем scannedCode, так как мы отсканировали новую упаковку этого товара.
//                val currentMat = currentState.materials[existingIndex]
//                val newQty = (currentMat.qty.toIntOrNull() ?: 0) + qty
//
//                val updatedMaterials = currentState.materials.toMutableList()
//                updatedMaterials[existingIndex] = currentMat.copy(
//                    qty = newQty.toString(),
//                    scannedCode = code
//                        ?: currentMat.scannedCode // Если код пришел, ставим его, иначе оставляем старый
//                )
//
//                _uiState.value = UiState.MaterialsLoaded(updatedMaterials)
//            } else {
//                // Материала нет, добавляем новый
//                val newDto = MaterialDto(
//                    id = System.currentTimeMillis().toString(),
//                    material = material,
//                    qty = qty.toString(),
//                    name = name,
//                    status = "new",
//                    scannedCode = code
//                )
//                val updatedList = currentState.materials + newDto
//                _uiState.value = UiState.MaterialsLoaded(updatedList)
//            }
//        }
//    }

    /**
     * Удаляет материал из списка по артикулу.
     */
//    fun removeMaterialFromOrder(materialArticle: String) {
//        val currentState = _uiState.value
//        if (currentState is UiState.MaterialsLoaded) {
//            val updatedMaterials = currentState.materials.filter { it.material != materialArticle }
//            _uiState.value = UiState.MaterialsLoaded(updatedMaterials)
//        }
//    }

    // --- Конец логики управления списком ---

    fun loadMaterials(orderNumber: String) {
        executeRequest(
            request = {
                apiService.getOrderMaterials(
                    GetOrderMaterialsRequest(
                        getTokenOrThrow(),
                        orderNumber,
                        "receive"
                    )
                )
            },
            onSuccess = { response ->
                val materials = response.firstOrNull()?.materials ?: emptyList()
                _uiState.value = UiState.MaterialsLoaded(materials)
            },
            errorMsg = "Ошибка загрузки материалов"
        )
    }

    fun packMaterial(material: String, qty: Int, code: String) {
        executeRequest(
            request = {
                apiService.packMaterial(PackMaterialRequest(getTokenOrThrow(), material, qty, code))
            },
            onSuccess = { response ->
                if (response.status == "success") {
                    _uiState.value = UiState.Packed(response.message)
                } else {
                    _uiState.value = UiState.Error(response.message)
                }
            },
            errorMsg = "Ошибка упаковки"
        )
    }

    // Упаковать материал (отправить на сервер)
//    fun packMaterialByDto(materialDto: MaterialDto) {
//        val code = materialDto.scannedCode
//        if (code.isNullOrBlank()) {
//            _uiState.value =
//                UiState.Error("Необходимо отсканировать штрихкод для материала ${materialDto.material}")
//            return
//        }
//
//        val qty = materialDto.qty.toIntOrNull() ?: 1
//
//        executeRequest(
//            request = {
//                apiService.packMaterial(
//                    PackMaterialRequest(
//                        getTokenOrThrow(),
//                        materialDto.material,
//                        qty,
//                        code
//                    )
//                )
//            },
//            onSuccess = { response ->
//                if (response.status == "success") {
//                    _uiState.value = UiState.Packed(response.message)
//                } else {
//                    _uiState.value = UiState.Error(response.message)
//                }
//            },
//            errorMsg = "Ошибка упаковки"
//        )
//    }

    fun createOrder(materials: List<Pair<String, Int>>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val materialsData = materials.map { MaterialQty(it.first, it.second) }
                val response = apiService.createOrder(CreateOrderRequest(token, materialsData))

                if (response.success == "ok") {
                    _uiState.value = UiState.OrderCreatedAndReadyForReceive(response.order)
                } else {
                    _uiState.value = UiState.Error(response.success ?: "Не удалось создать заказ")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка создания заказа: $e")
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети или сервера")
            }
        }
    }

    fun loadWarehouseMaterials(startDate: String, endDate: String, sapNum: String = "") {
        executeRequest(
            request = {
                apiService.getWarehouseMaterials(
                    GetWarehouseMaterialsRequest(
                        getTokenOrThrow(),
                        sapNum,
                        "",
                        startDate,
                        endDate
                    )
                )
            },
            onSuccess = { _uiState.value = UiState.WarehouseMaterialsLoaded(it) },
            errorMsg = "Ошибка загрузки склада"
        )
    }

    /**
     * Отправляет данные о принятых материалах на складе на сервер
     */
    fun receiveWmsMaterials(orderNumber: String, materials: List<WmsReceiveItem>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = WmsReceiveRequest(token = token, materialsData = materials)

                // Получаем "сырой" ответ
                val responseBody = apiService.receiveWmsMaterials(request)
                val responseString = responseBody.string().trim()

                Log.d("MainViewModel", "WMS Receive raw response: $responseString")

                // === ОБРАБОТКА ОТВЕТА ===

                // 1. Проверяем на простые текстовые успехи (сервер может возвращать "ok", "success")
                if (responseString.equals("ok", ignoreCase = true) ||
                    responseString.equals("success", ignoreCase = true)) {
                    _uiState.value = UiState.WmsReceiveSuccess("Приемка успешно завершена")
                    return@launch
                }

                // 2. Пытаемся распарсить как JSON
                if (responseString.startsWith("{")) {
                    try {
                        val json = org.json.JSONObject(responseString)
                        val status = json.optString("status", "")
                        val message = json.optString("message", json.optString("msg", ""))

                        if (status.equals("success", ignoreCase = true) || status.equals("ok", ignoreCase = true)) {
                            _uiState.value = UiState.WmsReceiveSuccess(message.ifEmpty { "Приемка успешно завершена" })
                        } else {
                            _uiState.value = UiState.Error(message.ifEmpty { "Ошибка сервера" })
                        }
                    } catch (e: org.json.JSONException) {
                        _uiState.value = UiState.Error("Ошибка парсинга ответа сервера")
                    }
                } else {
                    // Это plain text. Проверяем, не ошибка ли это
                    val errorMsg = if (responseString.contains("SQLSTATE", ignoreCase = true) ||
                        responseString.contains("error", ignoreCase = true) ||
                        responseString.contains("exception", ignoreCase = true)) {
                        "Ошибка сервера: ${responseString.take(150)}"
                    } else {
                        // Неизвестный текстовый ответ — считаем успехом, если он короткий и не похож на ошибку
                        // Но лучше всё же показывать предупреждение для отладки
                        _uiState.value = UiState.WmsReceiveSuccess(responseString.ifEmpty { "Приемка успешно завершена" })
                        return@launch
                    }
                    _uiState.value = UiState.Error(errorMsg)
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка приемки WMS: $e")
                _uiState.value = UiState.Error(e.message ?: "Ошибка сети или сервера")
            }
        }
    }

    /**
     * Сбрасывает состояние после успешной приемки на складе
     */
    fun resetReceiveState() {
        if (_uiState.value is UiState.WmsReceiveSuccess) {
            _uiState.value = UiState.Idle
        }
    }

    // ====================== Инвентаризация ======================
    fun loadInventoryOrders() {
        executeRequest(
            request = {
                apiService.getInventoryOrders(
                    GetInventoryOrdersRequest(
                        getTokenOrThrow(),
                        "status"
                    )
                )
            },
            onSuccess = { _uiState.value = UiState.InventoryOrdersLoaded(it) },
            errorMsg = "Не удалось загрузить заказы инвентаризации"
        )
    }

    fun checkInventoryMaterial(material: String, order: String, qty: Int) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                // 1. Отправляем запрос на сверку
                val responseList = apiService.checkInventoryMaterial(
                    CheckInventoryMaterialRequest(
                        token,
                        material,
                        order,
                        qty
                    )
                )

                // 2. Если ответ успешный, перезагружаем список материалов, чтобы получить актуальный count_fact с сервера
                if (responseList.isNotEmpty()) {
                    // Опционально: можно показать кратковременное уведомление об успехе, но не блокировать экран

                    // Перезагружаем материалы для этого заказа
                    loadInventoryMaterials(order)
                } else {
                    _uiState.value = UiState.Error("Пустой ответ от сервера")
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка сверки", e)

                // При ошибке сети/сервера можно подсветить материал красным, но не менять количество
                val currentState = _uiState.value
                if (currentState is UiState.InventoryMaterialsLoaded) {
                    val updatedMaterials = currentState.materials.map {
                        if (it.material == material) {
                            it.copy(hasError = true, isJustChecked = false)
                        } else {
                            it.copy(hasError = false, isJustChecked = false)
                        }
                    }
                    _uiState.value = UiState.InventoryMaterialsLoaded(updatedMaterials, order)
                } else {
                    _uiState.value = UiState.Error(e.message ?: "Неизвестная ошибка")
                }
            }
        }
    }

    fun finishInventoryOrder(orderNumber: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val warehouse = currentInventoryWarehouse ?: throw Exception("Склад не определен")
                // Формируем текущую дату в формате, который ожидает сервер (например, ISO или как в curl)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val request = FinishInventoryRequest(
                    token = token,
                    warehouse = warehouse,
                    dateCreate = currentDate,
                    order = orderNumber
                )

                val response = apiService.finishInventoryOrder(request)

                if (response.isNotEmpty()) {
                    _uiState.value = UiState.InventoryFinished("Инвентаризация завершена")
                    isInventoryActive = false
                    // Опционально: можно перезагрузить список заказов или обновить статус текущего заказа
//                    loadInventoryMaterials(orderNumber)
                } else {
                    _uiState.value = UiState.Error("Ошибка завершения инвентаризации")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка завершения инвентаризации", e)
                _uiState.value = UiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun loadInventoryMaterials(orderNumber: String) {
        executeRequest(
            request = {
                apiService.getInventoryMaterials(
                    GetInventoryMaterialsRequest(
                        getTokenOrThrow(),
                        orderNumber
                    )
                )
            },
            onSuccess = { materials ->
                // Здесь мы не знаем warehouse и isActive, так как API inv_order_view их не возвращает.
                // Поэтому лучше передавать их из списка заказов.
                // Для примера предположим, что мы установили их ранее или передали.
                // Если нет, придется доработать навигацию.
                _uiState.value = UiState.InventoryMaterialsLoaded(materials, orderNumber)
            },
            errorMsg = "Ошибка загрузки материалов инвентаризации"
        )
    }

    // Метод для установки контекста инвентаризации
    fun setInventoryContext(orderNumber: String, warehouse: String, isActive: Boolean) {
        currentInventoryOrder = orderNumber
        currentInventoryWarehouse = warehouse
        isInventoryActive = isActive
    }
    // ====================== Инвентаризация ======================


    // --- Helpers ---
    private suspend fun getTokenOrThrow(): String {
        return currentToken ?: tokenStorage.getToken()
        ?: throw Exception("Пользователь не авторизован")
    }

    private fun <T> executeRequest(
        request: suspend () -> T,
        onSuccess: (T) -> Unit,
        errorMsg: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = request()
                onSuccess(result)
            } catch (e: Exception) {
                Log.e("MainViewModel", errorMsg, e)
                val msg = if (e.message?.contains("BEGIN_OBJECT") == true) {
                    "Ошибка сервера: Неверный формат ответа."
                } else {
                    e.message ?: errorMsg
                }
                _uiState.value = UiState.Error(msg)
            }
        }
    }
}