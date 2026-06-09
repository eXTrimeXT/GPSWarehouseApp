package com.gps.warehouse.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gps.warehouse.data.local.TokenStorage.PreferencesKeys.LOGIN_TIMESTAMP
import com.gps.warehouse.data.local.TokenStorage.PreferencesKeys.THEME_MODE_KEY
import com.gps.warehouse.data.local.TokenStorage.PreferencesKeys.TOKEN
import com.gps.warehouse.utils.AppThemeMode
import com.gps.warehouse.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = Constants.TOKEN_PREFS_NAME)

@Singleton
class TokenStorage @Inject constructor(private val context: Context) {
    object PreferencesKeys {
        // Работа с ТОКЕНОМ
        val TOKEN = stringPreferencesKey(Constants.TOKEN_KEY)
        val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")

        // Для Темы
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    // ======================== ТОКЕН ========================
    // Поток для получения токена
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN] }

    // Ключи для хранения в DataStore
    private val GPS_TOKEN_KEY = stringPreferencesKey("gps_token")
    private val ASSETS_TOKEN_KEY = stringPreferencesKey("assets_token")

    // ==================== GPS API TOKEN ====================

    /**
     * Сохраняет токен авторизации для GPS API
     */
    suspend fun saveGpsToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[GPS_TOKEN_KEY] = token
        }
    }

    /**
     * Получает токен авторизации для GPS API. Возвращает null, если токена нет.
     */
    suspend fun getGpsToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[GPS_TOKEN_KEY] }
            .first()
    }

    // ==================== ASSETS API TOKEN ====================

    /**
     * Сохраняет токен авторизации для Assets API
     */
    suspend fun saveAssetsToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ASSETS_TOKEN_KEY] = token
        }
    }

    /**
     * Получает токен авторизации для Assets API. Возвращает null, если токена нет.
     */
    suspend fun getAssetsToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[ASSETS_TOKEN_KEY] }
            .first()
    }

    // ==================== УТИЛИТЫ ====================

    /**
     * Очищает оба токена (используется при выходе из системы / logout)
     */
    suspend fun clearAllTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(GPS_TOKEN_KEY)
            preferences.remove(ASSETS_TOKEN_KEY)
        }
    }

    /**
     * Проверка, авторизован ли пользователь хотя бы в одной из систем
     */
    suspend fun isLoggedIn(): Boolean {
        return getGpsToken() != null || getAssetsToken() != null
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit {
            it[TOKEN] = token
            it[LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }

    }

    suspend fun clearToken() {
        context.dataStore.edit {
            it.remove(PreferencesKeys.TOKEN)
            it.remove(PreferencesKeys.LOGIN_TIMESTAMP)
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[PreferencesKeys.TOKEN] }.firstOrNull()
    }

    suspend fun getLoginTimestamp(): Long? {
        return context.dataStore.data.map { it[PreferencesKeys.LOGIN_TIMESTAMP] }.firstOrNull()
    }
    // ======================== ТОКЕН ========================


    suspend fun saveThemeMode(mode: AppThemeMode) {
        context.dataStore.edit {
            it[THEME_MODE_KEY] = mode.value
        }
    }

    suspend fun getThemeMode(): AppThemeMode {
        return try {
            val preferences = context.dataStore.data.first()
            val value = preferences[THEME_MODE_KEY] ?: AppThemeMode.SYSTEM.value
            AppThemeMode.entries.firstOrNull { it.value == value } ?: AppThemeMode.SYSTEM
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

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
}