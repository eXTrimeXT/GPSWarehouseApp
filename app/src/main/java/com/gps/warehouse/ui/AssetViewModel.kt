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
        object Idle : AssetUiState()
        object Loading : AssetUiState()
        data class AssetsLoaded(val assets: List<AssetShortDto>) : AssetUiState()
        data class AssetDetailsLoaded(val asset: AssetDto) : AssetUiState()
        data class AssetTypesLoaded(val types: List<AssetTypeDto>) : AssetUiState()
        data class AssetClassesLoaded(val classes: List<AssetClassDto>) : AssetUiState()
        data class AssetModelsLoaded(val models: List<AssetModelDto>) : AssetUiState()
        data class CatalogLoaded(val catalog: List<AssetCatalogDto>) : AssetUiState()
        data class UsersLoaded(val users: List<UserShortDto>) : AssetUiState()
        data class VendorsLoaded(val vendors: List<VendorShortDto>) : AssetUiState()
        data class WarehousesLoaded(val warehouses: List<WarehouseShortDto>) : AssetUiState()
        data class Error(val message: String) : AssetUiState()
    }

    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    private val _selectedAssetType = MutableStateFlow<Int?>(null)
    val selectedAssetType: StateFlow<Int?> = _selectedAssetType.asStateFlow()

    private val _selectedAssetClass = MutableStateFlow<Int?>(null)
    val selectedAssetClass: StateFlow<Int?> = _selectedAssetClass.asStateFlow()

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

    fun loadAssetTypes() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val token = getTokenOrThrow()
                val types = assetApiService.getAssetTypes("Bearer $token")
                _uiState.value = AssetUiState.AssetTypesLoaded(types)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки типов")
            }
        }
    }

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

    fun filterByAssetType(typeId: Int?) {
        _selectedAssetType.value = typeId
        loadAssets()
    }

    fun filterByAssetClass(classId: Int?) {
        _selectedAssetClass.value = classId
        loadAssets()
    }

    fun resetState() {
        _uiState.value = AssetUiState.Idle
    }

    private suspend fun getTokenOrThrow(): String {
        return tokenStorage.getToken() ?: throw Exception("Пользователь не авторизован")
    }
}