package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carrito_temporal")
data class CarritoTemporal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "producto_id")
    val productoId: Long,

    @ColumnInfo(name = "tienda_id")
    val tiendaId: Long,

    val cantidad: Double,

    @ColumnInfo(name = "precio_unitario")
    val precioUnitario: Long
)
