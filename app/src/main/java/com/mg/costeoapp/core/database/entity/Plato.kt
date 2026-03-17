package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "platos",
    indices = [
        Index("nombre"),
        Index("activo")
    ]
)
data class Plato(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nombre: String,

    val descripcion: String? = null,

    @ColumnInfo(name = "margen_porcentaje")
    val margenPorcentaje: Double? = null,

    @ColumnInfo(name = "precio_venta_manual")
    val precioVentaManual: Long? = null,

    @ColumnInfo(defaultValue = "1")
    val activo: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
