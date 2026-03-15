package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventario",
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
        Index(value = ["producto_id"]),
        Index(value = ["tienda_id"])
    ]
)
data class Inventario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "producto_id")
    val productoId: Long,

    @ColumnInfo(name = "tienda_id")
    val tiendaId: Long,

    val cantidad: Double,

    @ColumnInfo(name = "precio_compra")
    val precioCompra: Long,

    @ColumnInfo(name = "fecha_compra")
    val fechaCompra: Long,

    val agotado: Boolean = false,

    val activo: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
