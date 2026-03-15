package com.mg.costeoapp.core.di

import android.content.Context
import androidx.room.Room
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CosteoDatabase =
        Room.databaseBuilder(
            context,
            CosteoDatabase::class.java,
            "costeo_database"
        ).build()

    @Provides
    fun provideTiendaDao(database: CosteoDatabase): TiendaDao =
        database.tiendaDao()

    @Provides
    fun provideProductoDao(database: CosteoDatabase): ProductoDao =
        database.productoDao()

    @Provides
    fun provideProductoTiendaDao(database: CosteoDatabase): ProductoTiendaDao =
        database.productoTiendaDao()
}
