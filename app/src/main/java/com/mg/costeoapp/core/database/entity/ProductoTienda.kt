package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "producto_tienda",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Tienda::class,
            parentColumns = ["id"],
            childColumns = ["tienda_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["producto_id", "tienda_id", "fecha_registro"], unique = true),
        Index(value = ["producto_id"]),
        Index(value = ["tienda_id"])
    ]
)
data class ProductoTienda(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "producto_id")
    val productoId: Long,

    @ColumnInfo(name = "tienda_id")
    val tiendaId: Long,

    @ColumnInfo(name = "precio")
    val precio: Long,

    @ColumnInfo(name = "fecha_registro")
    val fechaRegistro: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "activo")
    val activo: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "version", defaultValue = "1")
    val version: Int = 1
)
