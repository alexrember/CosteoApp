package com.mg.costeoapp.feature.inventario.di

import com.mg.costeoapp.feature.inventario.data.InventarioRepository
import com.mg.costeoapp.feature.inventario.data.InventarioRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InventarioModule {

    @Binds
    @Singleton
    abstract fun bindInventarioRepository(impl: InventarioRepositoryImpl): InventarioRepository
}
