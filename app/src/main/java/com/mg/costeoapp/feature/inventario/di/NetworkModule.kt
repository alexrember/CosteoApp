package com.mg.costeoapp.feature.inventario.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mg.costeoapp.BuildConfig
import com.mg.costeoapp.feature.inventario.data.remote.WalmartVtexApi
import com.mg.costeoapp.feature.inventario.data.repository.WalmartStoreRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CosteoApp/1.0 Android")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideWalmartVtexApi(client: OkHttpClient, json: Json): WalmartVtexApi {
        return Retrofit.Builder()
            .baseUrl(WalmartVtexApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WalmartVtexApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWalmartStoreRepository(api: WalmartVtexApi): WalmartStoreRepository {
        return WalmartStoreRepository(api)
    }
}
