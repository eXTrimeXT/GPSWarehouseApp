package com.gps.warehouse.ui.assets_screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.TokenStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.AssetPositionDto
import com.gps.warehouse.data.remote.assets_dto.WorkshopDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Исправленный интерфейс состояния (добавлено поле positions)
sealed interface AssetMapUiState {
    object Loading : AssetMapUiState
    data class Success(
        val workshops: List<WorkshopDto>,
        val positions: List<AssetPositionDto>
    ) : AssetMapUiState
    data class Error(val message: String) : AssetMapUiState
}

@HiltViewModel
class AssetMapViewModel @Inject constructor(
    private val apiService: AssetApiService,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssetMapUiState>(AssetMapUiState.Loading)
    val uiState: StateFlow<AssetMapUiState> = _uiState.asStateFlow()

    fun loadMapData() {
        viewModelScope.launch {
            _uiState.value = AssetMapUiState.Loading
            try {
                val token = tokenStorage.getToken() ?: throw Exception("Нет токена")

                // Запускаем оба запроса параллельно
                val workshops = apiService.getWorkshops(token)

                // 2. Загружаем позиции (убедитесь, что этот метод есть в AssetApiService)
                val positions = apiService.getAssetPositions(token)

                // 3. Передаем positions в состояние
                _uiState.value = AssetMapUiState.Success(workshops, positions)
            } catch (e: Exception) {
                _uiState.value = AssetMapUiState.Error(e.message ?: "Ошибка загрузки карты")
            }
        }
    }
}