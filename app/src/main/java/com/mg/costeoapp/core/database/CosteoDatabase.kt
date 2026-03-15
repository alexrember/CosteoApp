package com.mg.costeoapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.database.entity.Tienda

@Database(
    entities = [
        Tienda::class,
        Producto::class,
        ProductoTienda::class,
        Inventario::class
    ],
    version = 3,
    exportSchema = true
)
abstract class CosteoDatabase : RoomDatabase() {
    abstract fun tiendaDao(): TiendaDao
    abstract fun productoDao(): ProductoDao
    abstract fun productoTiendaDao(): ProductoTiendaDao
    abstract fun inventarioDao(): InventarioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventario (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        producto_id INTEGER NOT NULL,
                        tienda_id INTEGER NOT NULL,
                        cantidad REAL NOT NULL,
                        precio_compra INTEGER NOT NULL,
                        fecha_compra INTEGER NOT NULL,
                        agotado INTEGER NOT NULL DEFAULT 0,
                        activo INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE RESTRICT,
                        FOREIGN KEY (tienda_id) REFERENCES tiendas(id) ON DELETE RESTRICT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventario_producto_id ON inventario(producto_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventario_tienda_id ON inventario(tienda_id)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE productos ADD COLUMN unidades_por_empaque INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
