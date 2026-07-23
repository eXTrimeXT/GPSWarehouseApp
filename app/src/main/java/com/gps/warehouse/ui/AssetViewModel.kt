package com.gps.warehouse.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.gps.warehouse.data.local.TokenStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.MyAssetDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import com.gps.warehouse.data.remote.assets_dto.ApiErrorResponseDto
import com.gps.warehouse.data.remote.assets_dto.CheckItemRequest
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionCreateRequest
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.data.remote.assets_dto.map.AssetPosition
import com.gps.warehouse.data.remote.assets_dto.map.Workshop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import retrofit2.HttpException
import javax.inject.Inject
import kotlin.jvm.java

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val assetApiService: AssetApiService
) : ViewModel() {

    sealed class AssetUiState {
        object Idle : AssetUiState()
        object Loading : AssetUiState()
        data class MyAssetsLoaded(val assets: List<MyAssetDto>) : AssetUiState()
        data class Error(val message: String) : AssetUiState()
        data class MyAssetDetailsLoaded(val asset: MyAssetDto) : AssetUiState()
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

        data class MapSuccess(
            val workshops: List<Workshop>,
            val assets: List<AssetPosition>
        ) : AssetUiState()

        data class InventorizationSessionsLoaded(val sessions: List<InventorizationSessionDto>) : AssetUiState()
        data class InventorizationItemsLoaded(
            val items: List<InventorizationItemDto>,
            val sessionId: Int
        ) : AssetUiState()
    }

    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    // StateFlow для хранения списка активов (для поиска по ID)
    private val _myAssetsList = MutableStateFlow<List<MyAssetDto>>(emptyList())
    val myAssetsList: StateFlow<List<MyAssetDto>> = _myAssetsList.asStateFlow()

    private val _myPcsList = MutableStateFlow<List<MyPcDto>>(emptyList())
    val myPcsList: StateFlow<List<MyPcDto>> = _myPcsList.asStateFlow()

    private val _assetTypes = MutableStateFlow<List<AssetTypeDto>>(emptyList())
    val assetTypes: StateFlow<List<AssetTypeDto>> = _assetTypes.asStateFlow()

    private val _mapData = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val mapData: StateFlow<AssetUiState> = _mapData.asStateFlow()

    private val _inventorizationSessions = MutableStateFlow<List<InventorizationSessionDto>>(emptyList())
    val inventorizationSessions: StateFlow<List<InventorizationSessionDto>> = _inventorizationSessions.asStateFlow()

    private val _inventorizationItems = MutableStateFlow<List<InventorizationItemDto>>(emptyList())
    val inventorizationItems: StateFlow<List<InventorizationItemDto>> = _inventorizationItems.asStateFlow()


    // ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ ДЛЯ ПАРСИНГА ОШИБОК
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

    suspend fun getToken(): String {
        return tokenStorage.getToken() ?: throw Exception("Отсутствует GPS токен авторизации. Выполните вход заново.")
    }

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

    // ================== Типы активов ==================
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

    fun loadMapData() {
        viewModelScope.launch {
            _mapData.value = AssetUiState.Loading
            try {
                val assets = assetApiService.getAssetPositions("Bearer ${getToken()}")
                val workshops = assetApiService.getWorkshops("Bearer ${getToken()}")
                _mapData.value = AssetUiState.MapSuccess(workshops, assets)
            } catch (e: Exception) {
                _mapData.value = AssetUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    // ================== Инвентаризация ==================
    fun loadInventorizationSessions() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val sessions = assetApiService.getInventorizationSessions("Bearer ${getToken()}")
                _inventorizationSessions.value = sessions
                _uiState.value = AssetUiState.InventorizationSessionsLoaded(sessions)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка загрузки сессий")
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
            _uiState.value = AssetUiState.Loading
            try {
                val session = assetApiService.startInventorizationSession(
                    "Bearer ${getToken()}",
                    InventorizationSessionCreateRequest(assetTypeId)
                )
                // Перезагружаем список сессий
                loadInventorizationSessions()
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(getErrorMessage(e) ?: "Ошибка создания сессии")
            }
        }
    }

    fun checkInventorizationItem(sessionId: Int, assetId: Int) {
        viewModelScope.launch {
            try {
                assetApiService.checkInventorizationItem(
                    "Bearer ${getToken()}",
                    sessionId,
                    CheckItemRequest(assetId)
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
}