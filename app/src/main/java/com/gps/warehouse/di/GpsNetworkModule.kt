package com.gps.warehouse.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gps.warehouse.R
import com.gps.warehouse.data.remote.GPSApiService
import com.gps.warehouse.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.InputStream
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.math.log

/**
 * Модуль Hilt для предоставления зависимостей, связанных с сетью.
 * Все предоставленные здесь объекты имеют область видимости Singleton,
 * то есть создаются один раз при запуске приложения и переиспользуются.
 */
@Module
@InstallIn(SingletonComponent::class)
object GpsNetworkModule {

    /**
     * Предоставляет экземпляр OkHttpClient.
     * OkHttpClient отвечает за выполнение HTTP-запросов.
     * Здесь мы настраиваем:
     * 1. Логирование запросов и ответов (для отладки).
     * 2. Тайм-ауты соединения и чтения.
     * 3. CookieJar для автоматического управления куки.
     */
    @Provides
    @Singleton
    @Named("gps")
    fun provideOkHttpClient(@ApplicationContext context: Context, authInterceptor: AuthInterceptor): OkHttpClient {
        // 1. Загружаем сертификат из res/raw/gps_https.crt
        val certificateFactory = CertificateFactory.getInstance("X.509")
//        val certificateInputStream: InputStream = context.resources.openRawResource(R.raw.gps_https)
        val certificateInputStream: InputStream = context.resources.openRawResource(R.raw.hmmr_ru)
        val certificate: Certificate = certificateFactory.generateCertificate(certificateInputStream)
        certificateInputStream.close()

        // 2. Создаем KeyStore и добавляем туда наш сертификат
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }

        // 3. Создаем TrustManager, который доверяет нашему KeyStore
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }
        val trustManagers = trustManagerFactory.trustManagers
        require(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${trustManagers.contentToString()}"
        }
        val trustManager = trustManagers[0] as X509TrustManager

        // 4. Создаем SSLContext с нашим TrustManager
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }


        // Настраиваем интерцептор для логирования всего тела запроса и ответа.
        // Уровень BODY полезен при разработке, но в продакшене лучше использовать NONE или HEADERS.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Создаем кастомный CookieJar для сохранения сессионных данных, т.к. API их использует.
        val cookieJar = PersistentCookieJar()

        return OkHttpClient.Builder()
//            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .addInterceptor(authInterceptor)                // Перехватывает 401 ошибки
            .addInterceptor(loggingInterceptor)             // Добавляем логгер
            .connectTimeout(300, TimeUnit.SECONDS)   // Тайм-аут на установление соединения
            .readTimeout(300, TimeUnit.SECONDS)      // Тайм-аут на чтение данных
            .cookieJar(cookieJar)                           // Устанавливаем менеджер куки
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
    @Named("gps")
    fun provideRetrofit(@Named("gps") client: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL_API) // Базовый URL API, взятый из Constants
            .client(client)                  // Используем настроенный OkHttpClient
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
    fun provideApiService(@Named("gps") retrofit: Retrofit): GPSApiService {
        return retrofit.create(GPSApiService::class.java)
    }
}