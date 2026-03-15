package com.mg.costeoapp.feature.productos.di

import com.mg.costeoapp.feature.productos.data.ProductoRepository
import com.mg.costeoapp.feature.productos.data.ProductoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductoModule {

    @Binds
    @Singleton
    abstract fun bindProductoRepository(impl: ProductoRepositoryImpl): ProductoRepository
}
