package com.gps.warehouse.di

import android.util.Log
import com.gps.warehouse.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Если сервер вернул 401 Unauthorized, сессия точно истекла
        if (response.code == 401) {
            Log.w("AuthInterceptor", "Получен HTTP 401 Unauthorized. Инициируем автовыход.")
            sessionManager.notifySessionExpired()
        }

        return response
    }
}