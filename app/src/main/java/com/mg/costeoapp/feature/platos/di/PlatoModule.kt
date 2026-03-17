package com.mg.costeoapp.feature.platos.di

import com.mg.costeoapp.feature.platos.data.PlatoRepository
import com.mg.costeoapp.feature.platos.data.PlatoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatoModule {

    @Binds
    @Singleton
    abstract fun bindPlatoRepository(impl: PlatoRepositoryImpl): PlatoRepository
}
