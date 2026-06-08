package com.gps.warehouse.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gps.warehouse.data.remote.GPSApiService
import com.gps.warehouse.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Модуль Hilt для предоставления зависимостей, связанных с сетью.
 * Все предоставленные здесь объекты имеют область видимости Singleton,
 * то есть создаются один раз при запуске приложения и переиспользуются.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Предоставляет экземпляр OkHttpClient.
     * OkHttpClient отвечает за выполнение HTTP-запросов.
     * Здесь мы настраиваем:
     * 1. Логирование запросов и ответов (для отладки).
     * 2. Таймауты соединения и чтения.
     * 3. CookieJar для автоматического управления куки.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Настраиваем интерцептор для логирования всего тела запроса и ответа.
        // Уровень BODY полезен при разработке, но в продакшене лучше использовать NONE или HEADERS.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Создаем кастомный CookieJar для сохранения сессионных данных, т.к. API их использует.
        val cookieJar = PersistentCookieJar()

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)            // Добавляем логгер
            .connectTimeout(30, TimeUnit.SECONDS)   // Таймаут на установление соединения
            .readTimeout(30, TimeUnit.SECONDS)      // Таймаут на чтение данных
            .cookieJar(cookieJar)                          // Устанавливаем менеджер куки
            .build()
    }

    /**
     * Предоставляет экземпляр Gson для парсинга JSON.
     * Мы используем setLenient(), чтобы Gson мог прощать некоторые ошибки в JSON,
     * например, комментарии или лишние запятые, что иногда встречается в ответах сервера.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient() // Разрешает нестрогий синтаксис JSON
            .create()
    }

    /**
     * Предоставляет экземпляр Retrofit.
     * Retrofit — это типобезопасный HTTP-клиент для Android.
     * Здесь мы связываем baseUrl, OkHttpClient и конвертеры данных.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)    // Базовый URL API, взятый из Constants
            .client(client)                 // Используем настроенный OkHttpClient
            // ScalarsConverterFactory нужен для обработки простых типов (String, Int и т.д.).
            // Он используется в методе getPublicKey(), который возвращает plain text.
            .addConverterFactory(ScalarsConverterFactory.create())
            // GsonConverterFactory преобразует JSON-ответы в Kotlin-объекты (Data Classes).
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Предоставляет экземпляр ApiService.
     * ApiService — это интерфейс, описывающий все эндпоинты нашего API.
     * Retrofit создает реализацию этого интерфейса автоматически.
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): GPSApiService {
        return retrofit.create(GPSApiService::class.java)
    }
}