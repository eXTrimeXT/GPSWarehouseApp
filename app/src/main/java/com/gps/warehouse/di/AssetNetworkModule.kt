package com.gps.warehouse.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gps.warehouse.data.remote.AssetApiService
import com.gps.warehouse.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AssetNetworkModule {

    // 2. Предоставляем GsonConverterFactory (ИМЕННО ЭТОГО НЕ ХВАТАЛО)
    @Provides
    @Singleton
    fun provideGsonConverterFactory(gson: Gson): GsonConverterFactory {
        return GsonConverterFactory.create(gson)
    }


    // 4. Базовый URL для API Активов
    @Provides
    @Singleton
    @Named("assetBaseUrl")
    fun provideAssetBaseUrl(): String {
        // Укажите здесь реальный базовый URL для API управления активами
        // Если он такой же, как у GPS, можно использовать Constants.BASE_URL
        return Constants.ASSET_URL
    }

    // 5. Создаем AssetApiService, используя предоставленные выше зависимости
    @Provides
    @Singleton
    fun provideAssetApiService(
        @Named("assetBaseUrl") baseUrl: String,
        okHttpClient: OkHttpClient,
        gsonConverterFactory: GsonConverterFactory
    ): AssetApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()
            .create(AssetApiService::class.java)
    }
}