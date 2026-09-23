package com.gps.warehouse.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gps.warehouse.data.local.LocalStorage
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import com.gps.warehouse.utils.NetworkMonitor
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
    private val localStorage: LocalStorage,
    private val apiService: AssetApiService,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _mobileUiState = MutableStateFlow<UiState>(UiState.Loading)
    val mobileUiState: StateFlow<UiState> = _mobileUiState

    private val _detailUiState = MutableStateFlow<UiState>(UiState.Loading)
    val detailUiState: StateFlow<UiState> = _detailUiState

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    private suspend fun getToken(): String {
        return localStorage.getToken() ?: throw Exception("Отсутствует GPS токен авторизации.")
    }

    fun loadDevices(serialNumber: String? = null) {
        viewModelScope.launch {
            val cachedDevices = localStorage.getCachedMobileDevices()

            // Мгновенно показываем кэш (только для полного списка, не для поиска)
            if (cachedDevices.isNotEmpty() && serialNumber == null) {
                _mobileUiState.value = UiState.Success(cachedDevices)
            }

            // Проверка сети
            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedDevices.isEmpty() || serialNumber != null) {
                    _mobileUiState.value = UiState.Error("Нет подключения к сети и нет сохраненных данных")
                }
                return@launch
            }

            // Загрузка из сети
            try {
                val response = apiService.getMobileDevices(
                    token = "Bearer ${getToken()}",
                    serialNumber = serialNumber,
                    skip = 0,
                    limit = 100
                )

                // Сохраняем в кэш только полный список (без фильтра по serialNumber)
                if (serialNumber == null) {
                    localStorage.saveMobileDevicesCache(response)
                }

                _mobileUiState.value = UiState.Success(response)
            } catch (e: Exception) {
                Log.e("MobileDevicesVM", "Ошибка загрузки устройств (используем кэш)", e)
                if (cachedDevices.isEmpty() || serialNumber != null) {
                    _mobileUiState.value = UiState.Error(e.message ?: "Неизвестная ошибка сети")
                }
            }
        }
    }

    fun loadDeviceDetails(serialNumber: String) {
        viewModelScope.launch {
            val cachedDetail = localStorage.getCachedMobileDeviceDetail(serialNumber)

            // Показываем кэш, если есть
            if (cachedDetail != null) {
                _detailUiState.value = UiState.Success(listOf(cachedDetail))
            } else {
                _detailUiState.value = UiState.Loading
            }

            // Проверка сети
            if (!networkMonitor.isCurrentlyConnected()) {
                if (cachedDetail == null) {
                    _detailUiState.value = UiState.Error("Нет сети и нет сохраненных данных")
                }
                return@launch
            }

            // Загрузка из сети
            try {
                val response = apiService.getMobileDevices(
                    token = "Bearer ${getToken()}",
                    serialNumber = serialNumber,
                    skip = 0,
                    limit = 1
                )
                if (response.isNotEmpty()) {
                    val device = response.first()
                    localStorage.saveMobileDeviceDetailCache(serialNumber, device) // Обновляем кэш
                    _detailUiState.value = UiState.Success(listOf(device))
                } else {
                    if (cachedDetail == null) _detailUiState.value = UiState.Error("Устройство не найдено")
                }
            } catch (e: Exception) {
                Log.e("MobileDevicesVM", "Ошибка загрузки деталей (используем кэш)", e)
                if (cachedDetail == null) {
                    _detailUiState.value = UiState.Error(e.message ?: "Ошибка сети")
                }
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
}