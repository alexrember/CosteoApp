package com.mg.costeoapp.feature.auth.di

import com.mg.costeoapp.core.security.NativeSecrets
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = NativeSecrets.getSupabaseUrl(),
            supabaseKey = NativeSecrets.getSupabaseAnonKey()
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
