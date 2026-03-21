package com.mg.costeoapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mg.costeoapp.core.database.dao.CarritoTemporalDao
import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.CarritoTemporal
import com.mg.costeoapp.core.database.entity.CostoIndirecto
import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.database.entity.Tienda

@Database(
    entities = [
        Tienda::class,
        Producto::class,
        ProductoTienda::class,
        Inventario::class,
        CarritoTemporal::class,
        Prefabricado::class,
        PrefabricadoIngrediente::class,
        CostoIndirecto::class,
        Plato::class,
        PlatoComponente::class
    ],
    version = 8,
    exportSchema = true
)
abstract class CosteoDatabase : RoomDatabase() {
    abstract fun tiendaDao(): TiendaDao
    abstract fun productoDao(): ProductoDao
    abstract fun productoTiendaDao(): ProductoTiendaDao
    abstract fun inventarioDao(): InventarioDao
    abstract fun carritoTemporalDao(): CarritoTemporalDao
    abstract fun prefabricadoDao(): PrefabricadoDao
    abstract fun prefabricadoIngredienteDao(): PrefabricadoIngredienteDao
    abstract fun platoDao(): PlatoDao
    abstract fun platoComponenteDao(): PlatoComponenteDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS carrito_temporal (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        producto_id INTEGER NOT NULL,
                        tienda_id INTEGER NOT NULL,
                        cantidad REAL NOT NULL,
                        precio_unitario INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS prefabricados (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT,
                        duplicado_de INTEGER,
                        costo_fijo INTEGER NOT NULL DEFAULT 0,
                        rendimiento_porciones REAL NOT NULL,
                        activo INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (duplicado_de) REFERENCES prefabricados(id) ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prefabricados_duplicado_de ON prefabricados(duplicado_de)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prefabricados_activo ON prefabricados(activo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prefabricados_nombre ON prefabricados(nombre)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS prefabricado_ingrediente (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        prefabricado_id INTEGER NOT NULL,
                        producto_id INTEGER NOT NULL,
                        cantidad_usada REAL NOT NULL,
                        unidad_usada TEXT NOT NULL,
                        FOREIGN KEY (prefabricado_id) REFERENCES prefabricados(id) ON DELETE CASCADE,
                        FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE RESTRICT
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_prefabricado_ingrediente_prefabricado_id_producto_id ON prefabricado_ingrediente(prefabricado_id, producto_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_prefabricado_ingrediente_producto_id ON prefabricado_ingrediente(producto_id)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS costos_indirectos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        prefabricado_id INTEGER NOT NULL,
                        nombre TEXT NOT NULL,
                        monto INTEGER NOT NULL,
                        activo INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY (prefabricado_id) REFERENCES prefabricados(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_costos_indirectos_prefabricado_id ON costos_indirectos(prefabricado_id)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS platos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT,
                        margen_porcentaje REAL,
                        precio_venta_manual INTEGER,
                        activo INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_platos_nombre ON platos(nombre)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_platos_activo ON platos(activo)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plato_componente (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plato_id INTEGER NOT NULL,
                        prefabricado_id INTEGER,
                        producto_id INTEGER,
                        cantidad REAL NOT NULL,
                        notas TEXT,
                        FOREIGN KEY (plato_id) REFERENCES platos(id) ON DELETE CASCADE,
                        FOREIGN KEY (prefabricado_id) REFERENCES prefabricados(id) ON DELETE RESTRICT,
                        FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE RESTRICT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plato_componente_plato_id ON plato_componente(plato_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plato_componente_prefabricado_id ON plato_componente(prefabricado_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plato_componente_producto_id ON plato_componente(producto_id)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_productos_codigo_barras")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productos_codigo_barras ON productos(codigo_barras)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventario_producto_id_agotado_activo ON inventario(producto_id, agotado, activo)")
            }
        }
    }
}
