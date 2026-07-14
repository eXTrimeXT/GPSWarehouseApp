package com.gps.warehouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.TokenStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.MyAssetDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
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
        data class MyAssetsLoaded(val assets: List<com.gps.warehouse.data.remote.assets_dto.MyAssetDto>) : AssetUiState()
        data class Error(val message: String) : AssetUiState()
        data class MyAssetDetailsLoaded(val asset: MyAssetDto) : AssetUiState()
        data class MyPcsLoaded(val pcs: List<MyPcDto>) : AssetUiState()
    }

    private val _uiState = MutableStateFlow<AssetUiState>(AssetUiState.Idle)
    val uiState: StateFlow<AssetUiState> = _uiState.asStateFlow()

    // Добавьте StateFlow для хранения списка активов (для поиска по ID)
    private val _myAssetsList = MutableStateFlow<List<MyAssetDto>>(emptyList())
    val myAssetsList: StateFlow<List<MyAssetDto>> = _myAssetsList.asStateFlow()

    private val _myPcsList = MutableStateFlow<List<MyPcDto>>(emptyList())
    val myPcsList: StateFlow<List<MyPcDto>> = _myPcsList.asStateFlow()

    // Обновите метод loadMyAssets:
    fun loadMyAssets() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val gpsToken = tokenStorage.getToken()
                    ?: throw Exception("Отсутствует GPS токен авторизации. Выполните вход заново.")
                val assets = assetApiService.getMyAssignedAssets("Bearer $gpsToken")
                _myAssetsList.value = assets  // Сохраняем список
                _uiState.value = AssetUiState.MyAssetsLoaded(assets)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки активов")
            }
        }
    }

    // Добавьте метод получения актива по ID:
    fun loadMyAssetDetails(assetId: Int) {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                val gpsToken = tokenStorage.getToken()
                    ?: throw Exception("Отсутствует GPS токен")
                val asset = assetApiService.getMyAssetById("Bearer $gpsToken", assetId)
                _uiState.value = AssetUiState.MyAssetDetailsLoaded(asset)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    // Метод получения ПК текущего пользователя
    fun loadMyPcs() {
        viewModelScope.launch {
            _uiState.value = AssetUiState.Loading
            try {
                // Используем GPS токен, как требуется для всех запросов к активам
                val gpsToken = tokenStorage.getToken()
                    ?: throw Exception("Отсутствует токен авторизации")

                val pcs = assetApiService.getMyPcs("Bearer $gpsToken")
                _myPcsList.value = pcs  // Сохраняем в отдельный поток
                _uiState.value = AssetUiState.MyPcsLoaded(pcs)
            } catch (e: Exception) {
                _uiState.value = AssetUiState.Error(e.message ?: "Ошибка загрузки ПК")
            }
        }
    }

    fun resetState() {
        _uiState.value = AssetUiState.Idle
    }
}