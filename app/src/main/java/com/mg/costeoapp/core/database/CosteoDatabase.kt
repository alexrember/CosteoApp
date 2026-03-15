package com.mg.costeoapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.database.entity.Tienda

@Database(
    entities = [
        Tienda::class,
        Producto::class,
        ProductoTienda::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CosteoDatabase : RoomDatabase() {
    abstract fun tiendaDao(): TiendaDao
    abstract fun productoDao(): ProductoDao
    abstract fun productoTiendaDao(): ProductoTiendaDao
}
