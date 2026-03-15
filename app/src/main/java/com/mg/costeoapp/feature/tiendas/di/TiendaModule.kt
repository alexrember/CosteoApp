package com.mg.costeoapp.feature.tiendas.di

import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import com.mg.costeoapp.feature.tiendas.data.TiendaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TiendaModule {

    @Binds
    @Singleton
    abstract fun bindTiendaRepository(impl: TiendaRepositoryImpl): TiendaRepository
}
