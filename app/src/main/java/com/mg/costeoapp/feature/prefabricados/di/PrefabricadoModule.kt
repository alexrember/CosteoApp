package com.mg.costeoapp.feature.prefabricados.di

import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrefabricadoModule {

    @Binds
    @Singleton
    abstract fun bindPrefabricadoRepository(impl: PrefabricadoRepositoryImpl): PrefabricadoRepository
}
