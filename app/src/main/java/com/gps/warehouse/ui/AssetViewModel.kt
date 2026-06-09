package com.gps.warehouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.TokenStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val assetApiService: AssetApiService
) : ViewModel() {

    sealed class AssetUiState {
        // Базовые состояния
        object Idle : AssetUiState()
        object Loading : AssetUiState()
        data class Error(val message: String) : AssetUiState()

        // Активы
        data class AssetsLoaded(val assets: List<AssetShortDto>) : AssetUiState()
        data class AssetDetailsLoaded(val asset: AssetDto) : AssetUiState()
        // Типы активов
        data class AssetTypesLoaded(val types: List<AssetTypeDto>) : AssetUiState()
        data class AssetTypeDetailsLoaded(val assetType: AssetTypeDto) : AssetUiState()
        // Классы активов
        data class AssetClassesLoaded(val classes: List<AssetClassDto>) : AssetUiState()
        data class AssetClassDetailsLoaded(val assetClass: AssetClassDto) : AssetUiState()
        // Модель активов
        data class AssetModelsLoaded(val models: List<AssetModelDto>) : AssetUiState()
        data class AssetModelDetailsLoaded(val model: AssetModelDto) : AssetUiState()
        // Каталог активов
        data class CatalogLoaded(val catalog: List<AssetCatalogDto>) : AssetUiState()
        // Пользователи
        data class UsersLoaded(val users: List<UserShortDto>) : AssetUiState()
        // Поставщики и производители
        data class VendorsLoaded(val vendors: List<VendorShortDto>) : AssetUiState()
        // Склады
        data class WarehousesLoaded(val warehouses: List<WarehouseShortDto>) : AssetUiState()

        data class UserProfileLoaded(val profile: AssetsUserProfileDto) : AssetUiState()
    }

    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    private val _selectedAssetType = MutableStateFlow<Int?>(null)
    val selectedAssetType: StateFlow<Int?> = _selectedAssetType.asStateFlow()

    private val _selectedAssetClass = MutableStateFlow<Int?>(null)
    val selectedAssetClass: StateFlow<Int?> = _selectedAssetClass.asStateFlow()

    private val _userProfile = MutableStateFlow<AssetsUserProfileDto?>(null)
    val userProfile: StateFlow<AssetsUserProfileDto?> = _userProfile.asStateFlow()

    private val _assetTypes = MutableStateFlow<List<AssetTypeDto>>(emptyList())
    val assetTypes: StateFlow<List<AssetTypeDto>> = _assetTypes.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val token = getTokenOrThrow()
                val profile = assetApiService.getCurrentUser("Bearer $token")
                _userProfile.value = profile
                _uiState.value = AssetUiState.UserProfileLoaded(profile)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки профиля")
            }
        }
    }

    // ================== Активы ==================
    fun loadAssets() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val assets = assetApiService.getAssets("Bearer $token")
                _uiState.value = AssetUiState.AssetsLoaded(assets)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки активов")
            }
        }
    }

    fun loadAssetsByType(typeAsset: String?) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val assets = assetApiService.getAssets("Bearer $token", typeAsset?.toInt())
                _uiState.value = AssetUiState.AssetsLoaded(assets)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки активов")
            }
        }
    }

    fun loadAssetDetails(assetId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val asset = assetApiService.getAssetById("Bearer $token", assetId)
                _uiState.value = AssetUiState.AssetDetailsLoaded(asset)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки деталей")
            }
        }
    }
    // ================== Активы ==================

    // ================== Типы активов ==================
//    fun loadAssetTypes() {
//        viewModelScope.launch {
//            _uiState.value = AssetUiState.Loading
//            try {
//                val token = getTokenOrThrow()
//                val types = assetApiService.getAssetTypes("Bearer $token")
//                _uiState.value = AssetUiState.AssetTypesLoaded(types)
//            } catch (e: Exception) {
//                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки типов")
//            }
//        }
//    }

    fun loadAssetTypes() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val types = assetApiService.getAssetTypes("Bearer $token")
                _assetTypes.value = types
                _uiState.value = AssetUiState.AssetTypesLoaded(types)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки типов")
            }
        }
    }
    // ================== Типы активов ==================

    // ================== Классы ==================
    fun loadAssetClasses(typeId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val classes = assetApiService.getAssetClasses("Bearer $token", typeId)
                _uiState.value = AssetUiState.AssetClassesLoaded(classes)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки классов")
            }
        }
    }

    fun loadAssetClassDetails(classId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val assetClass = assetApiService.getAssetClassById("Bearer $token", classId)
                _uiState.value = AssetUiState.AssetClassDetailsLoaded(assetClass)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки класса актива")
            }
        }
    }
    // ================== Классы ==================

    // ================== Модели ==================
    fun loadAssetModels(classId: Int? = null) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val models = assetApiService.getAssetModels("Bearer $token", classId)
                _uiState.value = AssetUiState.AssetModelsLoaded(models)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки моделей")
            }
        }
    }

    fun loadAssetModelDetails(modelId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val model = assetApiService.getAssetModelById("Bearer $token", modelId)
                _uiState.value = AssetUiState.AssetModelDetailsLoaded(model)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки модели актива")
            }
        }
    }
    // ================== Модели ==================

    // ================== Каталог ==================
    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val catalog = assetApiService.getAssetCatalog("Bearer $token")
                _uiState.value = AssetUiState.CatalogLoaded(catalog)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки каталога")
            }
        }
    }
    // ================== Каталог ==================

    // ================== Пользователи ==================
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val users = assetApiService.getUsers("Bearer $token")
                _uiState.value = AssetUiState.UsersLoaded(users)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки пользователей")
            }
        }
    }
    // ================== Пользователи ==================

    // ================== Поставщики/Производители ==================
    fun loadVendors() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val vendors = assetApiService.getVendors("Bearer $token")
                _uiState.value = AssetUiState.VendorsLoaded(vendors)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки поставщиков")
            }
        }
    }
    // ================== Поставщики/Производители ==================

    // ================== Склады ==================
    fun loadWarehouses() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val warehouses = assetApiService.getWarehouses("Bearer $token")
                _uiState.value = AssetUiState.WarehousesLoaded(warehouses)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки складов")
            }
        }
    }
    // ================== Склады ==================

    // ================== Фильтры ==================
    fun filterByAssetType(typeId: Int?) {
        _selectedAssetType.value = typeId
        loadAssets()
    }

    fun filterByAssetClass(classId: Int?) {
        _selectedAssetClass.value = classId
        loadAssets()
    }
    // ================== Фильтры ==================

    fun resetState() {
        _uiState.value = AssetUiState.Idle
    }

    private suspend fun getTokenOrThrow(): String {
        return tokenStorage.getToken() ?: throw Exception("Пользователь не авторизован")
    }
}