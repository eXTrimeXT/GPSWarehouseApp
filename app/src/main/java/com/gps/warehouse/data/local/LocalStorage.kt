package com.gps.warehouse.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.gps.warehouse.data.local.LocalStorage.PreferencesKeys.KEY_CAMERA_SCAN
import com.gps.warehouse.data.local.LocalStorage.PreferencesKeys.LOGIN_TIMESTAMP
import com.gps.warehouse.data.local.LocalStorage.PreferencesKeys.THEME_MODE_KEY
import com.gps.warehouse.data.local.LocalStorage.PreferencesKeys.TOKEN
import com.gps.warehouse.data.remote.assets_dto.AssetResponseDto
import com.gps.warehouse.data.remote.assets_dto.AssetTypeDto
import com.gps.warehouse.data.remote.assets_dto.DeviceResponse
import com.gps.warehouse.data.remote.assets_dto.InventorizationItemDto
import com.gps.warehouse.data.remote.assets_dto.InventorizationSessionDto
import com.gps.warehouse.data.remote.assets_dto.MyPcDto
import com.gps.warehouse.data.remote.assets_dto.NotificationDto
import com.gps.warehouse.data.remote.gps_dto.BmListDto
import com.gps.warehouse.data.remote.gps_dto.GpsPermissionDto
import com.gps.warehouse.data.remote.gps_dto.InventoryMaterialDto
import com.gps.warehouse.data.remote.gps_dto.InventoryOrderDto
import com.gps.warehouse.data.remote.gps_dto.MaterialDto
import com.gps.warehouse.data.remote.gps_dto.OrderDto
import com.gps.warehouse.data.remote.gps_dto.WarehouseMaterialDto
import com.gps.warehouse.data.remote.gps_dto.WmsItemDto
import com.gps.warehouse.utils.AppThemeMode
import com.gps.warehouse.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

private const val TOKEN_PREFS_NAME = "gps_token_prefs"
private const val TOKEN_KEY = "jwt_token"
private const val CACHE_PREFS_NAME = "gps_profile_cache_prefs"

private val Context.dataStore by preferencesDataStore(name = TOKEN_PREFS_NAME)

@Singleton
class LocalStorage @Inject constructor(private val context: Context) {
    // Инициализируем SharedPreferences для кэша и Gson один раз
    private val cachePrefs by lazy {
        context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    object PreferencesKeys {
        // Работа с ТОКЕНОМ
        val TOKEN = stringPreferencesKey(TOKEN_KEY)
        val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")

        // Для Темы
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

        // Для камеры
        val KEY_CAMERA_SCAN = booleanPreferencesKey("camera_scan_enabled")
    }

    // ======================== ТОКЕН ========================
    // Поток для получения токена
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit {
            it[TOKEN] = token
            it[LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }

    }

    suspend fun clearToken() {
        context.dataStore.edit {
            it.remove(TOKEN)
            it.remove(LOGIN_TIMESTAMP)
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[TOKEN] }.firstOrNull()
    }

    suspend fun getLoginTimestamp(): Long? {
        return context.dataStore.data.map { it[LOGIN_TIMESTAMP] }.firstOrNull()
    }
    // ======================== ТОКЕН ========================


    // ======================== Тема ========================
    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { preferences ->
            val value = preferences[THEME_MODE_KEY] ?: AppThemeMode.SYSTEM.value
            AppThemeMode.entries.firstOrNull { it.value == value } ?: AppThemeMode.SYSTEM
        }
    // ======================== Тема ========================


    // ======================== Камера ========================
    val cameraScanEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_CAMERA_SCAN] ?: false }

    suspend fun setCameraScanEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_CAMERA_SCAN] = enabled }
    }
    // ======================== Камера ========================


    // ============================ КЭШ ============================
    // ======================== КЭШ ПРОФИЛЯ ========================
    fun saveProfileCache(
        bmList: List<BmListDto>,
        permissions: List<GpsPermissionDto>,
        isAssetsAdmin: Boolean
    ) {
        cachePrefs.edit()
            .putString("cache_bm_list", gson.toJson(bmList))
            .putString("cache_permissions", gson.toJson(permissions))
            .putBoolean("cache_is_assets_admin", isAssetsAdmin)
            .apply()
    }

    fun getCachedBmList(): List<BmListDto> {
        val json = cachePrefs.getString("cache_bm_list", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<BmListDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    fun getCachedPermissions(): List<GpsPermissionDto> {
        val json = cachePrefs.getString("cache_permissions", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<GpsPermissionDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    fun getCachedIsAssetsAdmin(): Boolean {
        return cachePrefs.getBoolean("cache_is_assets_admin", false)
    }

    // ======================== КЭШ МОБИЛЬНЫХ УСТРОЙСТВ ========================
    fun saveMobileDevicesCache(devices: List<DeviceResponse>) {
        cachePrefs.edit().putString("cache_mobile_devices", gson.toJson(devices)).apply()
    }

    fun getCachedMobileDevices(): List<DeviceResponse> {
        val json = cachePrefs.getString("cache_mobile_devices", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<DeviceResponse>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    fun saveMobileDeviceDetailCache(serialNumber: String, device: DeviceResponse) {
        cachePrefs.edit().putString("cache_mobile_detail_$serialNumber", gson.toJson(device)).apply()
    }

    fun getCachedMobileDeviceDetail(serialNumber: String): DeviceResponse? {
        val json = cachePrefs.getString("cache_mobile_detail_$serialNumber", null) ?: return null
        return try { gson.fromJson(json, DeviceResponse::class.java) } catch (e: Exception) { null }
    }

    // ======================== КЭШ СПИСКОВ ========================
    // --- Заказы ---
    fun saveOrdersCache(orders: List<OrderDto>) {
        cachePrefs.edit()
            .putString("cache_orders", gson.toJson(orders))
            .apply()
    }

    fun getCachedOrders(): List<OrderDto> {
        val json = cachePrefs.getString("cache_orders", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<OrderDto>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Материалы WMS ---
    fun saveWmsCache(items: List<WmsItemDto>) {
        cachePrefs.edit()
            .putString("cache_wms", gson.toJson(items))
            .apply()
    }

    fun getCachedWms(): List<WmsItemDto> {
        val json = cachePrefs.getString("cache_wms", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WmsItemDto>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    // =============================================================



    // ======================== КЭШ АКТИВОВ ========================
    // --- Список активов (ключ зависит от типа и флага "мои") ---
    fun saveAssetsCache(
        assets: List<AssetResponseDto>,
        assetTypeId: Int? = null,
        onlyMy: Boolean = false
    ) {
        // Формируем уникальный ключ, например: "cache_assets_type_5_my" или "cache_assets_type_all_all"
        val typeKey = assetTypeId?.toString() ?: "all"
        val myKey = if (onlyMy) "my" else "all"
        val cacheKey = "cache_assets_${typeKey}_${myKey}"

        cachePrefs.edit().putString(cacheKey, gson.toJson(assets)).apply()
        Log.d("LocalStorage", "Saved assets cache for key: $cacheKey, size: ${assets.size}")
    }

    fun getCachedAssets(assetTypeId: Int? = null, onlyMy: Boolean = false): List<AssetResponseDto> {
        val typeKey = assetTypeId?.toString() ?: "all"
        val myKey = if (onlyMy) "my" else "all"
        val cacheKey = "cache_assets_${typeKey}_${myKey}"

        val json = cachePrefs.getString(cacheKey, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<AssetResponseDto>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Детали актива (ключ зависит от конкретного ID актива или материала) ---
    fun saveAssetDetailCache(assetId: Int?, materialId: String?, asset: AssetResponseDto) {
        // Формируем уникальный ключ, например: "cache_detail_123" или "cache_detail_mat_ABC-999"
        val detailKey = "cache_detail_${assetId ?: "mat_$materialId"}"
        cachePrefs.edit().putString(detailKey, gson.toJson(asset)).apply()
    }

    fun getCachedAssetDetail(assetId: Int?, materialId: String?): AssetResponseDto? {
        val detailKey = "cache_detail_${assetId ?: "mat_$materialId"}"
        val json = cachePrefs.getString(detailKey, null) ?: return null
        return try {
            gson.fromJson(json, AssetResponseDto::class.java)
        } catch (e: Exception) {
            null
        }
    }
    // ============================================================================

    // --- Типы активов ---
    fun saveAssetTypesCache(types: List<AssetTypeDto>) {
        cachePrefs.edit().putString("cache_asset_types", gson.toJson(types)).apply()
    }
    fun getCachedAssetTypes(): List<AssetTypeDto> {
        val json = cachePrefs.getString("cache_asset_types", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<AssetTypeDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Сессии инвентаризации ---
    fun saveInventorySessionsCache(sessions: List<InventorizationSessionDto>) {
        cachePrefs.edit().putString("cache_inv_sessions", gson.toJson(sessions)).apply()
    }
    fun getCachedInventorySessions(): List<InventorizationSessionDto> {
        val json = cachePrefs.getString("cache_inv_sessions", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<InventorizationSessionDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Мои ПК ---
    fun saveMyPcsCache(pcs: List<MyPcDto>) {
        cachePrefs.edit().putString("cache_my_pcs", gson.toJson(pcs)).apply()
    }
    fun getCachedMyPcs(): List<MyPcDto> {
        val json = cachePrefs.getString("cache_my_pcs", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<MyPcDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }
    // =============================================================

    // --- Элементы инвентаризации (по ID сессии) ---
    fun saveInventoryItemsCache(sessionId: Int, items: List<InventorizationItemDto>) {
        cachePrefs.edit().putString("cache_inv_items_$sessionId", gson.toJson(items)).apply()
    }
    fun getCachedInventoryItems(sessionId: Int): List<InventorizationItemDto> {
        val json = cachePrefs.getString("cache_inv_items_$sessionId", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<InventorizationItemDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Уведомления ---
    fun saveNotificationsCache(notifications: List<NotificationDto>) {
        cachePrefs.edit().putString("cache_notifications", gson.toJson(notifications)).apply()
    }
    fun getCachedNotifications(): List<NotificationDto> {
        val json = cachePrefs.getString("cache_notifications", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<NotificationDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Материалы заказа (по номеру заказа) ---
    fun saveOrderMaterialsCache(orderNumber: String, materials: List<MaterialDto>) {
        cachePrefs.edit().putString("cache_order_mats_$orderNumber", gson.toJson(materials)).apply()
    }
    fun getCachedOrderMaterials(orderNumber: String): List<MaterialDto> {
        val json = cachePrefs.getString("cache_order_mats_$orderNumber", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<MaterialDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Материалы на складе ---
    fun saveWarehouseMaterialsCache(materials: List<WarehouseMaterialDto>) {
        cachePrefs.edit().putString("cache_warehouse_mats", gson.toJson(materials)).apply()
    }
    fun getCachedWarehouseMaterials(): List<WarehouseMaterialDto> {
        val json = cachePrefs.getString("cache_warehouse_mats", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<WarehouseMaterialDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Заказы инвентаризации ---
    fun saveInventoryOrdersCache(orders: List<InventoryOrderDto>) {
        cachePrefs.edit().putString("cache_inv_orders", gson.toJson(orders)).apply()
    }
    fun getCachedInventoryOrders(): List<InventoryOrderDto> {
        val json = cachePrefs.getString("cache_inv_orders", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<InventoryOrderDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    // --- Материалы инвентаризации (по номеру заказа) ---
    fun saveInventoryMaterialsCache(orderNumber: String, materials: List<InventoryMaterialDto>) {
        cachePrefs.edit().putString("cache_inv_mats_$orderNumber", gson.toJson(materials)).apply()
    }
    fun getCachedInventoryMaterials(orderNumber: String): List<InventoryMaterialDto> {
        val json = cachePrefs.getString("cache_inv_mats_$orderNumber", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<InventoryMaterialDto>>() {}.type) } catch (e: Exception) { emptyList() }
    }
    // ====================================================================
}