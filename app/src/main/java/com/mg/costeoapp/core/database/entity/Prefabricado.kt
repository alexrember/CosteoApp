package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prefabricados",
    foreignKeys = [
        ForeignKey(
            entity = Prefabricado::class,
            parentColumns = ["id"],
            childColumns = ["duplicado_de"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("duplicado_de"),
        Index("activo"),
        Index("nombre")
    ]
)
data class Prefabricado(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nombre: String,

    val descripcion: String? = null,

    @ColumnInfo(name = "duplicado_de")
    val duplicadoDe: Long? = null,

    @ColumnInfo(name = "costo_fijo", defaultValue = "0")
    val costoFijo: Long = 0,

    @ColumnInfo(name = "rendimiento_porciones")
    val rendimientoPorciones: Double,

    @ColumnInfo(defaultValue = "1")
    val activo: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
