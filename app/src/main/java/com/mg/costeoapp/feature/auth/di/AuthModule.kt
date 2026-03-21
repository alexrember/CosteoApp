package com.mg.costeoapp.feature.auth.di

import com.mg.costeoapp.feature.auth.data.AuthRepository
import com.mg.costeoapp.feature.auth.data.SupabaseAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository
}
