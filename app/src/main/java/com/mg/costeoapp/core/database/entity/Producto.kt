package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["codigo_barras"], unique = true),
        Index(value = ["global_product_id"])
    ]
)
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "codigo_barras")
    val codigoBarras: String? = null,

    @ColumnInfo(name = "unidad_medida")
    val unidadMedida: String,

    @ColumnInfo(name = "cantidad_por_empaque")
    val cantidadPorEmpaque: Double,

    @ColumnInfo(name = "unidades_por_empaque", defaultValue = "1")
    val unidadesPorEmpaque: Int = 1,

    @ColumnInfo(name = "es_servicio")
    val esServicio: Boolean = false,

    @ColumnInfo(name = "notas")
    val notas: String? = null,

    @ColumnInfo(name = "activo")
    val activo: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "nutricion_porcion_g")
    val nutricionPorcionG: Double? = null,

    @ColumnInfo(name = "nutricion_calorias")
    val nutricionCalorias: Double? = null,

    @ColumnInfo(name = "nutricion_proteinas_g")
    val nutricionProteinasG: Double? = null,

    @ColumnInfo(name = "nutricion_carbohidratos_g")
    val nutricionCarbohidratosG: Double? = null,

    @ColumnInfo(name = "nutricion_grasas_g")
    val nutricionGrasasG: Double? = null,

    @ColumnInfo(name = "nutricion_fibra_g")
    val nutricionFibraG: Double? = null,

    @ColumnInfo(name = "nutricion_sodio_mg")
    val nutricionSodioMg: Double? = null,

    @ColumnInfo(name = "nutricion_fuente")
    val nutricionFuente: String? = null,

    @ColumnInfo(name = "factor_merma")
    val factorMerma: Int = 0,

    @ColumnInfo(name = "version", defaultValue = "1")
    val version: Int = 1,

    @ColumnInfo(name = "global_product_id")
    val globalProductId: String? = null,

    @ColumnInfo(name = "alias")
    val alias: String? = null
)
