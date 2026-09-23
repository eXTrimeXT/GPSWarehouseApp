package com.gps.warehouse.data.local

import android.content.Context
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
import com.gps.warehouse.data.remote.gps_dto.BmListDto
import com.gps.warehouse.data.remote.gps_dto.GpsPermissionDto
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
        return try {
            val type = object : TypeToken<List<BmListDto>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCachedPermissions(): List<GpsPermissionDto> {
        val json = cachePrefs.getString("cache_permissions", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<GpsPermissionDto>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCachedIsAssetsAdmin(): Boolean {
        return cachePrefs.getBoolean("cache_is_assets_admin", false)
    }
}