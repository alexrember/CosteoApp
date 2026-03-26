package com.mg.costeoapp.feature.inventario.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mg.costeoapp.BuildConfig
import com.mg.costeoapp.feature.inventario.data.remote.BloomreachSearchApi
import com.mg.costeoapp.feature.inventario.data.remote.OpenFoodFactsApi
import com.mg.costeoapp.feature.inventario.data.remote.WalmartVtexApi
import com.mg.costeoapp.feature.inventario.data.repository.NutritionRepository
import com.mg.costeoapp.feature.inventario.data.repository.PriceSmartStoreRepository
import com.mg.costeoapp.feature.inventario.data.repository.CosteoBackendRepository
import com.mg.costeoapp.feature.inventario.data.repository.StoreSearchOrchestrator
import com.mg.costeoapp.feature.inventario.data.repository.SuperSelectosRepository
import com.mg.costeoapp.feature.inventario.data.repository.WalmartStoreRepository
import com.mg.costeoapp.feature.settings.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import javax.inject.Named
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

    // TODO: Add certificate pinning for Walmart VTEX before production release
    // Get pins with: openssl s_client -connect www.walmart.com.sv:443 | openssl x509 -pubkey | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
    // Then add: .certificatePinner(CertificatePinner.Builder().add("www.walmart.com.sv", "sha256/REAL_HASH_HERE").build())
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

    // --- PriceSmart Bloomreach Search API ---

    @Provides
    @Singleton
    fun provideBloomreachSearchApi(client: OkHttpClient, json: Json): BloomreachSearchApi {
        return Retrofit.Builder()
            .baseUrl(BloomreachSearchApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BloomreachSearchApi::class.java)
    }

    @Provides
    @Singleton
    fun providePriceSmartStoreRepository(api: BloomreachSearchApi): PriceSmartStoreRepository {
        return PriceSmartStoreRepository(api)
    }

    // --- Super Selectos (HTML scraping) ---

    @Provides
    @Singleton
    fun provideSuperSelectosRepository(client: OkHttpClient): SuperSelectosRepository {
        return SuperSelectosRepository(client)
    }

    // --- Backend centralizado (Fase 8) ---

    @Provides
    @Singleton
    fun provideCosteoBackendRepository(client: OkHttpClient, json: Json, supabaseClient: SupabaseClient): CosteoBackendRepository {
        return CosteoBackendRepository(client, json, supabaseClient)
    }

    // --- Orquestador de busqueda paralela ---

    @Provides
    @Singleton
    fun provideStoreSearchOrchestrator(
        walmartRepository: WalmartStoreRepository,
        priceSmartRepository: PriceSmartStoreRepository,
        superSelectosRepository: SuperSelectosRepository,
        backendRepository: CosteoBackendRepository,
        settingsRepository: SettingsRepository
    ): StoreSearchOrchestrator {
        return StoreSearchOrchestrator(
            walmartRepository,
            priceSmartRepository,
            superSelectosRepository,
            backendRepository,
            settingsRepository
        )
    }

    // --- Open Food Facts ---

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(client: OkHttpClient, json: Json): OpenFoodFactsApi {
        return Retrofit.Builder()
            .baseUrl(OpenFoodFactsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNutritionRepository(api: OpenFoodFactsApi): NutritionRepository {
        return NutritionRepository(api)
    }
}
