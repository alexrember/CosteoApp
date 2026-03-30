package com.mg.costeoapp.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.DatabaseSeeder
import com.mg.costeoapp.core.database.dao.CarritoTemporalDao
import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.SyncMetadataDao
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

    // TODO Fase 7: Encrypt database with SQLCipher
    // 1. Add dependency: implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    // 2. Add: implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    // 3. Replace .build() with:
    //    val passphrase = getOrCreatePassphrase() // from EncryptedSharedPreferences
    //    val factory = SupportFactory(passphrase)
    //    .openHelperFactory(factory)
    //    .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CosteoDatabase =
        Room.databaseBuilder(
            context,
            CosteoDatabase::class.java,
            "costeo_database"
        )
            .addMigrations(CosteoDatabase.MIGRATION_1_2, CosteoDatabase.MIGRATION_2_3, CosteoDatabase.MIGRATION_3_4, CosteoDatabase.MIGRATION_4_5, CosteoDatabase.MIGRATION_5_6, CosteoDatabase.MIGRATION_6_7, CosteoDatabase.MIGRATION_7_8, CosteoDatabase.MIGRATION_8_9, CosteoDatabase.MIGRATION_9_10, CosteoDatabase.MIGRATION_10_11)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    DatabaseSeeder.seed(db)
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            })
            .build()

    @Provides
    fun provideTiendaDao(database: CosteoDatabase): TiendaDao =
        database.tiendaDao()

    @Provides
    fun provideProductoDao(database: CosteoDatabase): ProductoDao =
        database.productoDao()

    @Provides
    fun provideProductoTiendaDao(database: CosteoDatabase): ProductoTiendaDao =
        database.productoTiendaDao()

    @Provides
    fun provideInventarioDao(database: CosteoDatabase): InventarioDao =
        database.inventarioDao()

    @Provides
    fun provideCarritoTemporalDao(database: CosteoDatabase): CarritoTemporalDao =
        database.carritoTemporalDao()

    @Provides
    fun providePrefabricadoDao(database: CosteoDatabase): PrefabricadoDao =
        database.prefabricadoDao()

    @Provides
    fun providePrefabricadoIngredienteDao(database: CosteoDatabase): PrefabricadoIngredienteDao =
        database.prefabricadoIngredienteDao()

    @Provides
    fun providePlatoDao(database: CosteoDatabase): PlatoDao =
        database.platoDao()

    @Provides
    fun providePlatoComponenteDao(database: CosteoDatabase): PlatoComponenteDao =
        database.platoComponenteDao()

    @Provides
    fun provideSyncMetadataDao(database: CosteoDatabase): SyncMetadataDao =
        database.syncMetadataDao()
}
