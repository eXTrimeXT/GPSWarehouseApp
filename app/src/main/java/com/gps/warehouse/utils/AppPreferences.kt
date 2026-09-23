package com.gps.warehouse.utils

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.gps.warehouse.data.remote.gps_dto.BmListDto
import com.gps.warehouse.data.remote.gps_dto.GpsPermissionDto

object AppPreferences {
    private const val PREFS_NAME = "app_ui_prefs"
    private const val KEY_DEFAULT_TAB = "default_tab_index"

    /**
     * Получает индекс вкладки по умолчанию (3 = Настройки)
     */
    fun getDefaultTab(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DEFAULT_TAB, 3)
    }

    /**
     * Сохраняет индекс выбранной вкладки
     */
    fun setDefaultTab(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_DEFAULT_TAB, index) }
    }
}

