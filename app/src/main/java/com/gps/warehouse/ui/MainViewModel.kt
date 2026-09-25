package com.gps.warehouse.ui

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.LocalStorage
import com.gps.warehouse.data.remote.GPSApiService
import com.gps.warehouse.data.remote.NotificationSseManager
import com.gps.warehouse.data.remote.gps_dto.*
import com.gps.warehouse.utils.AppThemeMode
import com.gps.warehouse.utils.NetworkMonitor
import com.gps.warehouse.utils.RsaUtils.encryptPassword
import com.gps.warehouse.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val localStorage: LocalStorage,
    private val apiService: GPSApiService,
    private val notificationSseManager: NotificationSseManager,
    private val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager
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
        data class PackToWarehouseIdle(
            val material: String = "",
            val quantity: String = "",
            val uniqueCode: String = ""
        ) : UiState()

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

    val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Свойство для фонового мониторинга сессии
    private val sessionMonitorScope =
        CoroutineScope(SupervisorJob() + viewModelScope.coroutineContext)

    private var currentToken: String? = null
    private var currentLogin: String? = null // Сохраняем логин при успешном входе
    var currentInventoryOrder: String? = null
    var currentInventoryWarehouse: String? = null

    // ================== Пагинация WMS ==================
    private var wmsCurrentPage = 1
    private var wmsTotalPages = 1
    private var totalMaterialsCount = 0

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMoreWms = MutableStateFlow(false)
    val hasMoreWms: StateFlow<Boolean> = _hasMoreWms.asStateFlow()
    // ====================================================

    // Параметры фильтрации (сохраняются между запросами)
    private var currentStorageFilterId: String? = null
    private var currentWmsSearchQuery: String = ""
    private var currentHideZeroQty: Boolean = false

    private val _availableWarehouses = MutableStateFlow<List<WarehousePermissionDto>>(emptyList())
    val availableWarehouses: StateFlow<List<WarehousePermissionDto>> =
        _availableWarehouses.asStateFlow()

    private val _gpsPermissions = MutableStateFlow<List<GpsPermissionDto>>(emptyList())
    val gpsPermissions: StateFlow<List<GpsPermissionDto>> = _gpsPermissions.asStateFlow()

    private val _userIsAssetsAdmin = MutableStateFlow(true)
    val userIsAssetsAdmin: StateFlow<Boolean> = _userIsAssetsAdmin.asStateFlow()

    // Флаг для инвентаризации
    var isInventoryActive: Boolean = true

    // Поток для темы
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _bmList = MutableStateFlow<List<BmListDto>>(emptyList())
    val bmList: StateFlow<List<BmListDto>> = _bmList.asStateFlow()

    val cameraScanEnabled: StateFlow<Boolean> = localStorage.cameraScanEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _pendingHighlightNotificationId = MutableStateFlow<Int?>(null)
    val pendingHighlightNotificationId: StateFlow<Int?> =
        _pendingHighlightNotificationId.asStateFlow()

    // ====================== Состояние для топологий =========================
    private val _topologies = MutableStateFlow<List<TopologyDto>>(emptyList())
    val topologies: StateFlow<List<TopologyDto>> = _topologies.asStateFlow()
    // ========================================================================

    init {
        viewModelScope.launch {
            // Собираем поток токена
            localStorage.tokenFlow.collect { token ->
                isSessionValid(token)
            }
            localStorage.themeModeFlow.collect { mode ->
                _themeMode.value = mode
            }

            // === Слушаем глобальные события 401 от Interceptor ===
            sessionManager.sessionExpiredEvent.collect {
                Log.w("MainViewModel", "Получен сигнал истечения сессии от Interceptor")
                logout()
            }
        }
        // Запускаем периодическую проверку сессии при инициализации
        startSessionMonitoring()

        // Мгновенная инициализация из кэша
        _bmList.value = localStorage.getCachedBmList()
        _gpsPermissions.value = localStorage.getCachedPermissions()
        _userIsAssetsAdmin.value = localStorage.getCachedIsAssetsAdmin()
    }


    // === Надежное извлечение времени истечения (exp) из JWT ===
    private fun getTokenExpirationTime(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
                val json = JSONObject(payload)
                // exp в JWT хранится в секундах, умножаем на 1000 для миллисекунд
                val expSeconds = json.optLong("exp", 0L)
                if (expSeconds > 0) expSeconds * 1000L else null
            } else null
        } catch (e: Exception) {
            Log.e("MainViewModel", "Ошибка парсинга JWT exp", e)
            null
        }
    }

    private fun startSessionMonitoring() {
        sessionMonitorScope.launch {
            while (true) {
                delay(60 * 1000L) // Проверяем раз в минуту (оптимально для батареи и надежности)

                val token = localStorage.getToken()
                if (!token.isNullOrEmpty()) {
                    val expTimeMs = getTokenExpirationTime(token)
                    if (expTimeMs != null) {
                        val currentTime = System.currentTimeMillis()
                        // Добавляем буфер 10 секунд, чтобы не было гонок на грани истечения
                        if (currentTime >= (expTimeMs - 10000)) {
                            Log.d("MainViewModel", "Сессия истекла (по JWT exp). Выполняется выход.")
                            logout()
                        }
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
        if (token.isNullOrEmpty()) {
            currentToken = null
            if (_uiState.value !is UiState.SessionExpired) {
                _uiState.value = UiState.Idle
            }
            return false
        }

        // === Проверяем реальный exp из токена, а не localStorage.getLoginTimestamp() ===
        val expTimeMs = getTokenExpirationTime(token)
        val currentTime = System.currentTimeMillis()

        if (expTimeMs != null && currentTime >= (expTimeMs - 10000)) {
            Log.d("MainViewModel", "Сессия невалидна (истекла по JWT). Выполняется выход.")
            logout()
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
//                if (true) {
                    val gpsToken = gpsResponse.data.token
//                    val gpsToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE3OTAzMjY3MzYsImV4cCI6MTc5MDMyNjg1NiwibG9naW4iOiJndzA3MDE1MzcwIiwibGFzdF9pcCI6IjEwLjE2OC4xNTQuNDIiLCJsYXN0X3RpbWUiOiIxMjo0Nzo1MiAyMS4wNy4yMDI2IiwiZGVwYXJ0bWVudCI6IlJEQyIsInBlcm1pc3Npb25zIjpbeyJuYW1lX2dyb3VwIjoiY29tcHV0ZXIiLCJyZWFkIjp0cnVlLCJ3cml0ZSI6ZmFsc2V9LHsibmFtZV9ncm91cCI6Im1lc19lcXVpcG1lbnQiLCJyZWFkIjpmYWxzZSwid3JpdGUiOmZhbHNlfSx7Im5hbWVfZ3JvdXAiOiJzdXBwbGllcyIsInJlYWQiOmZhbHNlLCJ3cml0ZSI6ZmFsc2V9LHsibmFtZV9ncm91cCI6InBvd2VyX2FkYXB0ZXIiLCJyZWFkIjpmYWxzZSwid3JpdGUiOmZhbHNlfSx7Im5hbWVfZ3JvdXAiOiJkYXRhX2NvbGxlY3Rpb25fZXF1aXBtZW50IiwicmVhZCI6ZmFsc2UsIndyaXRlIjpmYWxzZX0seyJuYW1lX2dyb3VwIjoiQWNjZXNzb3JpZXMiLCJyZWFkIjpmYWxzZSwid3JpdGUiOmZhbHNlfSx7Im5hbWVfZ3JvdXAiOiJuZXR3b3JrX2VxdWlwbWVudCIsInJlYWQiOmZhbHNlLCJ3cml0ZSI6ZmFsc2V9LHsibmFtZV9ncm91cCI6InByaW50aW5nX2VxdWlwbWVudCIsInJlYWQiOmZhbHNlLCJ3cml0ZSI6ZmFsc2V9LHsibmFtZV9ncm91cCI6InNlcnZlcl9oYXJkd2FyZSIsInJlYWQiOmZhbHNlLCJ3cml0ZSI6ZmFsc2V9LHsibmFtZV9ncm91cCI6InVzZXJzIiwicmVhZCI6ZmFsc2UsIndyaXRlIjpmYWxzZX0seyJuYW1lX2dyb3VwIjoiQXNzZXRzTVUiLCJyZWFkIjpmYWxzZSwid3JpdGUiOmZhbHNlfV0sImFzc2V0c19hZG1pbiI6dHJ1ZSwidXNlcl9kYXRhIjp7ImVtYWlsIjoiVGltdXIuTWFseXNoZXZAaG1tci5ydSIsImZ1bGxuYW1lIjoiVGltdXIgTWFseXNoZXYiLCJkZXBhcnRtZW50IjoiU0RHIiwiZGlzdGluZ3Vpc2hlZE5hbWUiOiJDTj1UaW11ciBNYWx5c2hldixPVT1TT0ZUV0FSRSBERVZFTE9QTUVOVCBHUk9VUCAoU0RHKSxPVT1JTkZPUk1BVElPTiBTWVNURU1TIFNVUFBPUlQgU0VDVElPTiAoSVNTUyksT1U9UnVzc2lhbiBEaWdpdGFsIENlbnRlciAoUkRDKSxPVT1Vc2VycyxPVT1ITU1SLERDPWxvY2FsLERDPWhtbXIsREM9cnUiLCJncm91cHMiOltdfX0.Fe72RgeeJlvb2UfhVLwcmcFLwvZC-ZrIAKXEJ8yx-2c"

                    // 4. Сохраняем токен
                    localStorage.saveToken(gpsToken)
                    currentToken = gpsToken
                    currentLogin = username

                    _uiState.value = UiState.LoggedIn(gpsToken)
                    startGlobalNotifications()
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

    /**
     * Метод для сброса состояния (например, при уходе с экрана)
     */
    fun resetStateToIdle() {
        _uiState.value = UiState.Idle
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
     * Сбрасывает состояние после успешной приемки на складе
     */
    fun resetReceiveState() {
        if (_uiState.value is UiState.WmsReceiveSuccess) {
            _uiState.value = UiState.Idle
        }
    }

    suspend fun logout() {
        localStorage.clearToken()
        currentToken = null
        _uiState.value = UiState.SessionExpired
        stopGlobalNotifications()
        Log.e("MainViewModel", "logout: Токен очищен. Выполняется автовыход.")
    }

    // Вызываем этот метод при успешной авторизации
    fun startGlobalNotifications() {
        viewModelScope.launch {
            // Создаем канал уведомлений
            notificationSseManager.startListening()
            Log.d("MainViewModel", "Глобальный SSE менеджер запущен")
        }
    }

    // Вызываем этот метод при выходе из системы (logout)
    fun stopGlobalNotifications() {
        notificationSseManager.stopListening()
    }

    fun setPendingHighlightId(id: Int?) {
        _pendingHighlightNotificationId.value = id
    }

    override fun onCleared() {
        super.onCleared()
        stopGlobalNotifications()
    }

    fun setCameraScanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localStorage.setCameraScanEnabled(enabled)
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            // Проверяем, есть ли уже данные, чтобы не показывать Loading и не стирать экран
            val hasCache = _bmList.value.isNotEmpty() || _gpsPermissions.value.isNotEmpty()
            if (!hasCache) {
                _uiState.value = UiState.Loading
            }

            try {
                val gpsProfile = apiService.getUserProfile(GetUserProfileRequest(getTokenOrThrow()))

                val storages = gpsProfile.warehousePermissions
                val isAssetsAdmin = gpsProfile.assetsIsAdmin ?: false
                val permissions = gpsProfile.permissions ?: emptyList()
                val bmList = gpsProfile.bmList ?: emptyList()

                if (storages != null) _availableWarehouses.value = storages
                _userIsAssetsAdmin.value = isAssetsAdmin
                _gpsPermissions.value = permissions
                _bmList.value = bmList

                // === ИСПРАВЛЕНИЕ: Сохраняем свежие данные в кэш ===
                localStorage.saveProfileCache(bmList, permissions, isAssetsAdmin)

                _uiState.value = UiState.ProfileLoaded(gpsProfile)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки профиля (используем кэш)", e)
                // Показываем ошибку только если кэша вообще нет
                if (!hasCache) {
                    _uiState.value = UiState.Error(e.message ?: "Не удалось загрузить профиль")
                }
            }
        }
    }

    // Загрузка доступных складов
    fun loadAvailableWarehouses() {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                // Загружаем через GPS API из профиля
                val profile = apiService.getUserProfile(GetUserProfileRequest(token))
                val storages = profile.warehousePermissions ?: emptyList()
                _availableWarehouses.value = storages.map { it }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки складов", e)
            }
        }
    }

    fun loadPermissions() {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                // Загружаем через GPS API из профиля
                val profile = apiService.getUserProfile(GetUserProfileRequest(token))
                val permissions = profile.permissions ?: emptyList()
                _gpsPermissions.value = permissions.map { it }
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

    // Загрузка топологий для склада
    fun loadTopologies(storageId: String) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                val request = GetTopologyRequest(storageId = storageId, token = token)
                val result = apiService.getTopologies(request)
                _topologies.value = result
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки топологий", e)
                _topologies.value = emptyList()
            }
        }
    }

    /**
     * Обновление материала с передачей всех изменяемых полей
     */
    fun updateWmsItem(
        item: WmsItemDto,
        newPosition: String,
        newPositionId: Int?,
        newMin: Int,
        newMax: Int,
        newMaterial: String? = null,    // Опционально: новый артикул (только для non-SAP)
        newQty: Int? = null,            // Опционально: новое количество (только для non-SAP)
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                val request = UpdateWmsRequest(
                    id = item.id,
                    // Если передано новое значение — используем его, иначе берём из item
                    material = newMaterial?.ifBlank { item.material } ?: item.material,
                    max = newMax,
                    min = newMin,
                    positionId = newPositionId,
                    position = newPosition,
                    storageId = item.storageId?.toString() ?: "",
                    storage = item.storage,
                    price = item.price.toString(),
                    // Количество: новое или из item
                    qty = newQty ?: item.qty.toInt(),
                    sapA = item.sapA,
                    name = item.name,
                    token = token
                )
                val responseBody = apiService.updateWmsItem(request)
                val responseString = responseBody.toString().trim()

                if (responseString.contains("success", ignoreCase = true) ||
                    responseString.contains("ok", ignoreCase = true)
                ) {
                    onSuccess()
                } else {
                    onError(responseString.ifEmpty { "Ошибка сервера" })
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка обновления материала", e)
                onError(e.message ?: "Ошибка сети")
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
                json.optString("login", "login")
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
    fun cancelWmsRequest(
        requestId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = WmsRequestAction(token = token, idPop = requestId, type = "cancel")
                val response = apiService.cancelWmsRequest(request)

                if (response.status == "success" || response.status == "ok") {
                    _uiState.value =
                        UiState.WmsRequestCancelled(response.message ?: "Запрос отменён")
                    onComplete(true, response.message ?: "Запрос отменён")
                } else {
                    _uiState.value = UiState.Error(response.message ?: "Ошибка отмены запроса")
                    onComplete(false, response.message ?: "Ошибка отмены запроса")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка отмены запроса: $e")
                val msg = e.message ?: "Ошибка сети"
                _uiState.value = UiState.Error(msg)
                onComplete(false, msg)
            }
        }
    }

    /**
     * Принимает входящий складской запрос
     */
    fun acceptWmsRequest(
        requestId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val token = getTokenOrThrow()
                val request = WmsRequestAction(token = token, idPop = requestId, type = "accept")
                val response = apiService.acceptWmsRequest(request)

                if (response.status == "success" || response.status == "ok") {
                    _uiState.value = UiState.WmsRequestAccepted(response.message ?: "Запрос принят")
                    onComplete(true, response.message ?: "Запрос отменён")
                } else {
                    _uiState.value = UiState.Error(response.message ?: "Ошибка принятия запроса")
                    onComplete(false, response.message ?: "Ошибка принятия запроса")

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
            _uiState.value is UiState.WmsRequestAccepted
        ) {
            _uiState.value = UiState.Idle
        }
    }

    fun loadWmsData(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch {
            val cachedWms = localStorage.getCachedWms()

            // Если это первая загрузка и есть кэш, показываем его сразу
            if (page == 1 && !append && cachedWms.isNotEmpty()) {
                _uiState.value = UiState.WmsLoaded(items = cachedWms)
            }

            val isOnline = networkMonitor.isCurrentlyConnected()
            if (!isOnline) {
                if (page == 1 && cachedWms.isEmpty()) {
                    _uiState.value =
                        UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            // Если сеть есть, делаем запрос
            if (page == 1 && !append) {
                _uiState.value = UiState.Loading // Показываем загрузку только если кэша не было
            }
            if (append) {
                _isLoadingMore.value = true
            }

            try {
                val token = getTokenOrThrow()
                val request = GetWmsRequest(
                    token = token,
                    numSap = currentWmsSearchQuery,
                    nameSap = "",
                    stloPop = currentStorageFilterId ?: "",
                    isHideStock = if (currentHideZeroQty) 1 else 0,
                    page = page,
                    limit = 50
                )
                val response: WmsResponseDto = apiService.getWmsData(request)

                wmsCurrentPage = response.page
                wmsTotalPages = response.totalPages
                totalMaterialsCount = response.totalCount
                _hasMoreWms.value = response.page < response.totalPages

                val currentState = _uiState.value
                val oldItems = if (append && currentState is UiState.WmsLoaded) {
                    currentState.items
                } else {
                    emptyList()
                }

                val updatedList = oldItems + response.data

                // 3. Успех: сохраняем ВЕСЬ обновленный список в кэш
                localStorage.saveWmsCache(updatedList)
                _uiState.value = UiState.WmsLoaded(items = updatedList)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки WMS (используем кэш)", e)
                if (page == 1 && !append && cachedWms.isEmpty()) {
                    _uiState.value = UiState.Error(e.message ?: "Неизвестная ошибка")
                }
                // Если ошибка при append (подгрузке), просто игнорируем, старые данные остаются
            } finally {
                if (append) {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    fun loadMoreWmsData() {
        Log.d(
            "WMS_PAGINATION",
            "loadMoreWmsData called: isLoadingMore=${_isLoadingMore.value}, " +
                    "hasMore=${_hasMoreWms.value}, currentPage=$wmsCurrentPage, totalPages=$wmsTotalPages"
        )
        if (_isLoadingMore.value) {
            Log.d("WMS_PAGINATION", "Ignored: already loading")
            return
        }
        if (!_hasMoreWms.value) {
            Log.d("WMS_PAGINATION", "Ignored: no more pages")
            return
        }
        if (wmsCurrentPage >= wmsTotalPages) {
            Log.d("WMS_PAGINATION", "Ignored: currentPage >= totalPages")
            return
        }
        loadWmsData(page = wmsCurrentPage + 1, append = true)
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

    fun loadOrders() {
        loadOrdersGeneric(
            type = "status",
            isArchive = false,
            cacheGetter = { localStorage.getCachedOrders() },
            cacheSaver = { localStorage.saveOrdersCache(it) })
    }

    fun loadArchive() {
        // Для архива можно использовать тот же кэш заказов или создать отдельный,
        // но для простоты пока используем общую логику.
        loadOrdersGeneric(
            type = "archive",
            isArchive = true,
            cacheGetter = { localStorage.getCachedOrders() },
            cacheSaver = { localStorage.saveOrdersCache(it) })
    }

    private fun loadOrdersGeneric(
        type: String,
        isArchive: Boolean,
        cacheGetter: () -> List<OrderDto>,
        cacheSaver: (List<OrderDto>) -> Unit
    ) {
        viewModelScope.launch {
            val cachedData = cacheGetter()

            // МГНОВЕННО показываем кэш, если он есть (экран не будет пустым!)
            if (cachedData.isNotEmpty()) {
                _uiState.value =
                    if (isArchive) UiState.ArchiveLoaded(cachedData) else UiState.OrdersLoaded(
                        cachedData
                    )
            }

            // Проверяем сеть. Если сети нет, мы уже показали кэш на шаге 1, просто выходим.
            val isOnline = networkMonitor.isCurrentlyConnected()
            if (!isOnline) {
                if (cachedData.isEmpty()) {
                    _uiState.value =
                        UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            // Если сеть есть, грузим свежие данные в фоне
            // Мы НЕ ставим UiState.Loading здесь, чтобы не стирать кэш с экрана!
            try {
                val response = apiService.getOrders(GetOrdersRequest(getTokenOrThrow(), type, null))

                // Успех: сохраняем в кэш и обновляем UI свежими данными
                cacheSaver(response)
                _uiState.value =
                    if (isArchive) UiState.ArchiveLoaded(response) else UiState.OrdersLoaded(
                        response
                    )

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки заказов (используем кэш)", e)
                // Мы ничего не делаем с _uiState, потому что там уже лежит кэш из шага 1!
                // Пользователь даже не заметит ошибку, он просто увидит чуть устаревшие данные.
            }
        }
    }

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

                // Проверяем на простые текстовые успехи
                if (responseString.equals("ok", ignoreCase = true) ||
                    responseString.equals("success", ignoreCase = true)
                ) {
                    _uiState.value = UiState.WmsWriteOffSuccess("Списание успешно завершено")
                    return@launch
                }

                // Пытаемся распарсить как JSON
                if (responseString.startsWith("{")) {
                    try {
                        val json = JSONObject(responseString)
                        val status = json.optString("status", "")
                        val message = json.optString("message", json.optString("msg", ""))

                        if (status.equals("success", ignoreCase = true) || status.equals(
                                "ok",
                                ignoreCase = true
                            )
                        ) {
                            _uiState.value =
                                UiState.WmsWriteOffSuccess(message.ifEmpty { "Списание успешно завершено" })
                        } else {
                            _uiState.value = UiState.Error(message.ifEmpty { "Ошибка сервера" })
                        }
                    } catch (e: JSONException) {
                        _uiState.value = UiState.Error("Ошибка парсинга ответа сервера")
                    }
                } else {
                    // Это plain text. Проверяем, не ошибка ли это
                    val errorMsg = if (responseString.contains("SQLSTATE", ignoreCase = true) ||
                        responseString.contains("error", ignoreCase = true) ||
                        responseString.contains("exception", ignoreCase = true)
                    ) {
                        "Ошибка сервера: ${responseString.take(150)}"
                    } else {
                        _uiState.value =
                            UiState.WmsWriteOffSuccess(responseString.ifEmpty { "Списание успешно завершено" })
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

                val responseBody = apiService.updateOrderMaterial(request = request)

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

                val responseBody = apiService.updateOrderMaterial(request = request)

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
                    id_mat = "0"   // или "" — зависит от поведения API
                )

                // Вызываем API с request в теле
                val responseBody = apiService.updateOrderMaterial(request = request)

                val responseString = responseBody.string().trim()
                Log.d("MainViewModel", "[addNewMaterialToOrder] status response: $responseString")

                // Обрабатываем ответ через общий хелпер
                handleOrderMatResponse(responseString, order, onSuccess, onError)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка addNewMaterialToOrder", e)
                onError(e.message ?: "Ошибка сети")
            }
        }
    }

    suspend fun getNameMaterial(material: String): String {
        return try {
            val token = getTokenOrThrow()
            val request = GetNameMaterialRequest(token = token, material = material)
            val response = apiService.getNameMaterial(request)
            Log.d("MainViewModel", "[getNameMaterial] material name: ${response.name}")
            response.name ?: ""
        } catch (e: Exception) {
            Log.e("MainViewModel", "Ошибка получения имени материала", e)
            ""
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


    fun loadMaterials(orderNumber: String) {
        viewModelScope.launch {
            val cachedMaterials = localStorage.getCachedOrderMaterials(orderNumber)
            if (cachedMaterials.isNotEmpty()) {
                _uiState.value = UiState.MaterialsLoaded(cachedMaterials)
            }

            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedMaterials.isEmpty()) {
                    _uiState.value =
                        UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            try {
                val response = apiService.getOrderMaterials(
                    GetOrderMaterialsRequest(getTokenOrThrow(), orderNumber, "receive")
                )
                val materials = response.firstOrNull()?.materials ?: emptyList()
                localStorage.saveOrderMaterialsCache(orderNumber, materials)
                _uiState.value = UiState.MaterialsLoaded(materials)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки материалов (используем кэш)", e)
                if (cachedMaterials.isEmpty()) {
                    _uiState.value = UiState.Error(e.message ?: "Ошибка загрузки материалов")
                }
            }
        }
    }

    /**
     * Вызывается при входе на экран упаковки.
     * Сбрасывает все предыдущие состояния (включая ошибки с других экранов)
     * и переводит ViewModel в состояние PackToWarehouseIdle.
     */
    fun enterPackToWarehouseScreen() {
        _uiState.value = UiState.PackToWarehouseIdle()
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

    // ====================== Материалы на складе ======================
    fun loadWarehouseMaterials(startDate: String, endDate: String, sapNum: String = "") {
        viewModelScope.launch {
            val cachedMaterials = localStorage.getCachedWarehouseMaterials()
            if (cachedMaterials.isNotEmpty()) {
                _uiState.value = UiState.WarehouseMaterialsLoaded(cachedMaterials)
            }

            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedMaterials.isEmpty()) {
                    _uiState.value =
                        UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            try {
                val response = apiService.getWarehouseMaterials(
                    GetWarehouseMaterialsRequest(getTokenOrThrow(), sapNum, "", startDate, endDate)
                )
                localStorage.saveWarehouseMaterialsCache(response)
                _uiState.value = UiState.WarehouseMaterialsLoaded(response)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки склада (используем кэш)", e)
                if (cachedMaterials.isEmpty()) {
                    _uiState.value = UiState.Error(e.message ?: "Ошибка загрузки склада")
                }
            }
        }
    }

    /**
     * Отправляет данные о принятых материалах на складе на сервер
     */
    fun receiveWmsMaterials(materials: List<WmsReceiveItem>) {
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

                // Проверяем на простые текстовые успехи (сервер может возвращать "ok", "success")
                if (responseString.equals("ok", ignoreCase = true) ||
                    responseString.equals("success", ignoreCase = true)
                ) {
                    _uiState.value = UiState.WmsReceiveSuccess("Приемка успешно завершена")
                    return@launch
                }

                // Пытаемся распарсить как JSON
                if (responseString.startsWith("{")) {
                    try {
                        val json = JSONObject(responseString)
                        val status = json.optString("status", "")
                        val message = json.optString("message", json.optString("msg", ""))

                        if (status.equals("success", ignoreCase = true) || status.equals(
                                "ok",
                                ignoreCase = true
                            )
                        ) {
                            _uiState.value =
                                UiState.WmsReceiveSuccess(message.ifEmpty { "Приемка успешно завершена" })
                        } else {
                            _uiState.value = UiState.Error(message.ifEmpty { "Ошибка сервера" })
                        }
                    } catch (e: JSONException) {
                        _uiState.value = UiState.Error("Ошибка парсинга ответа сервера")
                    }
                } else {
                    // Это plain text. Проверяем, не ошибка ли это
                    val errorMsg = if (responseString.contains("SQLSTATE", ignoreCase = true) ||
                        responseString.contains("error", ignoreCase = true) ||
                        responseString.contains("exception", ignoreCase = true)
                    ) {
                        "Ошибка сервера: ${responseString.take(150)}"
                    } else {
                        // Неизвестный текстовый ответ — считаем успехом, если он короткий и не похож на ошибку
                        // Но лучше всё же показывать предупреждение для отладки
                        _uiState.value =
                            UiState.WmsReceiveSuccess(responseString.ifEmpty { "Приемка успешно завершена" })
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

    // ====================== Инвентаризация ======================
    fun loadInventoryOrders() {
        viewModelScope.launch {
            val cachedOrders = localStorage.getCachedInventoryOrders()
            if (cachedOrders.isNotEmpty()) {
                _uiState.value = UiState.InventoryOrdersLoaded(cachedOrders)
            }

            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedOrders.isEmpty()) {
                    _uiState.value = UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            try {
                val response = apiService.getInventoryOrders(
                    GetInventoryOrdersRequest(getTokenOrThrow(), "status")
                )
                localStorage.saveInventoryOrdersCache(response)
                _uiState.value = UiState.InventoryOrdersLoaded(response)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки заказов инвентаризации (используем кэш)", e)
                if (cachedOrders.isEmpty()) {
                    _uiState.value = UiState.Error(e.message ?: "Не удалось загрузить заказы инвентаризации")
                }
            }
        }
    }

    fun checkInventoryMaterial(material: String, order: String, qty: Int) {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                // Отправляем запрос на сверку
                val responseList = apiService.checkInventoryMaterial(
                    CheckInventoryMaterialRequest(
                        token,
                        material,
                        order,
                        qty
                    )
                )

                // Если ответ успешный, перезагружаем список материалов, чтобы получить актуальный count_fact с сервера
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
                    loadInventoryMaterials(orderNumber)
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
        viewModelScope.launch {
            val cachedMaterials = localStorage.getCachedInventoryMaterials(orderNumber)
            if (cachedMaterials.isNotEmpty()) {
                _uiState.value = UiState.InventoryMaterialsLoaded(cachedMaterials, orderNumber)
            }

            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedMaterials.isEmpty()) {
                    _uiState.value = UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            try {
                val response = apiService.getInventoryMaterials(
                    GetInventoryMaterialsRequest(getTokenOrThrow(), orderNumber)
                )
                localStorage.saveInventoryMaterialsCache(orderNumber, response)
                _uiState.value = UiState.InventoryMaterialsLoaded(response, orderNumber)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Ошибка загрузки материалов инвентаризации (используем кэш)", e)
                if (cachedMaterials.isEmpty()) {
                    _uiState.value = UiState.Error(e.message ?: "Ошибка загрузки материалов инвентаризации")
                }
            }
        }
    }

    // Метод для установки контекста инвентаризации
    fun setInventoryContext(orderNumber: String, warehouse: String, isActive: Boolean) {
        currentInventoryOrder = orderNumber
        currentInventoryWarehouse = warehouse
        isInventoryActive = isActive
    }
    // ====================== Инвентаризация ======================


    // --- Helpers ---
    suspend fun getTokenOrThrow(): String {
        if (currentToken.isNullOrEmpty() && localStorage.getToken().isNullOrEmpty()) {
            logout()
            throw Exception("Пользователь не авторизован. Автовыход.")
        }
        return currentToken ?: localStorage.getToken()
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


    // ====================== ЧЕРНОВИКИ ЭКРАНОВ (Кэш при навигации) ======================
    // Упаковка (Packaging)
    private val _packagingMaterials = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val packagingMaterials: StateFlow<List<Pair<String, Int>>> = _packagingMaterials.asStateFlow()

    fun addPackagingMaterial(material: String, qty: Int) {
        val current = _packagingMaterials.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.first == material }
        if (existingIndex != -1) {
            current[existingIndex] = Pair(material, current[existingIndex].second + qty)
        } else {
            current.add(Pair(material, qty))
        }
        _packagingMaterials.value = current
    }

    fun removePackagingMaterial(index: Int) {
        val current = _packagingMaterials.value.toMutableList()
        if (index in current.indices) current.removeAt(index)
        _packagingMaterials.value = current
    }

    fun updatePackagingMaterial(index: Int, material: String, qty: Int) {
        val current = _packagingMaterials.value.toMutableList()
        if (index in current.indices) {
            current[index] = Pair(material, qty)
        }
        _packagingMaterials.value = current
    }

    fun clearPackagingMaterials() { _packagingMaterials.value = emptyList() }


    // Упаковка на склад (PackToWarehouse)
    data class PackToWarehouseDraft(val material: String = "", val qty: String = "", val code: String = "")
    private val _packToWarehouseDraft = MutableStateFlow(PackToWarehouseDraft())
    val packToWarehouseDraft: StateFlow<PackToWarehouseDraft> = _packToWarehouseDraft.asStateFlow()

    fun updatePackToWarehouseDraft(material: String? = null, qty: String? = null, code: String? = null) {
        val current = _packToWarehouseDraft.value
        _packToWarehouseDraft.value = current.copy(
            material = material ?: current.material,
            qty = qty ?: current.qty,
            code = code ?: current.code
        )
    }
    fun clearPackToWarehouseDraft() { _packToWarehouseDraft.value = PackToWarehouseDraft() }


    // Списание (WmsWriteOff)
    private val _writeOffItems = MutableStateFlow<List<WmsWriteOffItem>>(emptyList())
    val writeOffItems: StateFlow<List<WmsWriteOffItem>> = _writeOffItems.asStateFlow()

    fun addWriteOffItem(item: WmsWriteOffItem) {
        val current = _writeOffItems.value.toMutableList()
        // Можно добавить логику объединения, если нужно, пока просто добавляем
        current.add(item)
        _writeOffItems.value = current
    }

    fun updateWriteOffItem(index: Int, item: WmsWriteOffItem) {
        val current = _writeOffItems.value.toMutableList()
        if (index in current.indices) current[index] = item
        _writeOffItems.value = current
    }

    fun removeWriteOffItem(index: Int) {
        val current = _writeOffItems.value.toMutableList()
        if (index in current.indices) current.removeAt(index)
        _writeOffItems.value = current
    }

    fun clearWriteOffItems() { _writeOffItems.value = emptyList() }

    // Приемка (WmsReceive)
    private val _receiveItems = MutableStateFlow<List<WmsReceiveItem>>(emptyList())
    val receiveItems: StateFlow<List<WmsReceiveItem>> = _receiveItems.asStateFlow()

    // Для блокировки заказа при сканировании
    private val _activeReceiveOrder = MutableStateFlow("")
    val activeReceiveOrder: StateFlow<String> = _activeReceiveOrder.asStateFlow()

    fun setReceiveItems(items: List<WmsReceiveItem>) { _receiveItems.value = items }

    fun updateReceiveItem(index: Int, item: WmsReceiveItem) {
        val current = _receiveItems.value.toMutableList()
        if (index in current.indices) current[index] = item
        _receiveItems.value = current
    }

    fun removeReceiveItem(index: Int) {
        val current = _receiveItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            if (current.isEmpty()) _activeReceiveOrder.value = "" // Сброс при очистке
        }
        _receiveItems.value = current
    }

    fun setActiveReceiveOrder(order: String) { _activeReceiveOrder.value = order }
    fun clearReceiveItems() {
        _receiveItems.value = emptyList()
        _activeReceiveOrder.value = ""
    }
    // =====================================================================================
}