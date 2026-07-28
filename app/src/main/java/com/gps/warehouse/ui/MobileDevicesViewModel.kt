package com.gps.warehouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.TokenStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState {
    object Loading : UiState()
    data class Success(val devices: List<DeviceResponse>) : UiState()
    data class Error(val message: String) : UiState()
}

@HiltViewModel
open class MobileDevicesViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val apiService: AssetApiService
) : ViewModel() {

    private val _mobileUiState = MutableStateFlow<UiState>(UiState.Loading)
    val mobileUiState: StateFlow<UiState> = _mobileUiState

    private val _detailUiState = MutableStateFlow<UiState>(UiState.Loading)
    val detailUiState: StateFlow<UiState> = _detailUiState

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    private suspend fun getToken(): String {
        return tokenStorage.getToken() ?: throw Exception("Отсутствует GPS токен авторизации.")
    }

    fun loadDevices(serialNumber: String? = null) {
        viewModelScope.launch {
            _mobileUiState.value = UiState.Loading
            try {
                val response = apiService.getMobileDevices(
                    token = "Bearer ${getToken()}",
                    serialNumber = serialNumber,
                    skip = 0,
                    limit = 100
                )
                _mobileUiState.value = UiState.Success(response)
            } catch (e: Exception) {
                _mobileUiState.value = UiState.Error(e.message ?: "Неизвестная ошибка сети")
            }
        }
    }

    fun loadDeviceDetails(serialNumber: String) {
        viewModelScope.launch {
            _detailUiState.value = UiState.Loading
            try {
                val response = apiService.getMobileDevices(
                    token = "Bearer ${getToken()}",
                    serialNumber = serialNumber,
                    skip = 0,
                    limit = 1 // Оптимизация: запрашиваем только 1 устройство
                )
                if (response.isNotEmpty()) {
                    _detailUiState.value = UiState.Success(response)
                } else {
                    _detailUiState.value = UiState.Error("Устройство не найдено")
                }
            } catch (e: Exception) {
                _detailUiState.value = UiState.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    fun playDeviceSound(serialNumber: String) {
        viewModelScope.launch {
            try {
                val response = apiService.playDeviceSound(
                    token = "Bearer ${getToken()}",
                    serialNumber = serialNumber
                )
                if (response.isSuccessful && response.body() != null) {
                    _actionMessage.value = response.body()!!.message
                } else {
                    _actionMessage.value = "Ошибка сервера: ${response.code()}"
                }
            } catch (e: Exception) {
                _actionMessage.value = "Ошибка сети: ${e.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}