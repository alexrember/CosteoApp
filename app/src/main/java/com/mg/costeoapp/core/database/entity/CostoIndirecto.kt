package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "costos_indirectos",
    foreignKeys = [
        ForeignKey(
            entity = Prefabricado::class,
            parentColumns = ["id"],
            childColumns = ["prefabricado_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("prefabricado_id")]
)
data class CostoIndirecto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "prefabricado_id")
    val prefabricadoId: Long,

    val nombre: String,

    val monto: Long,

    @ColumnInfo(defaultValue = "1")
    val activo: Boolean = true
)
