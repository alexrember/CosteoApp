package com.mg.costeoapp.core.domain.engine

import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.domain.model.NutricionResumen
import com.mg.costeoapp.core.util.UnidadMedida
import javax.inject.Inject

interface NutricionEngine {
    suspend fun calculateNutricionPrefabricado(prefabricadoId: Long): NutricionResumen
    fun convertirAGramos(cantidad: Double, unidad: String): Double?
}

class NutricionEngineImpl @Inject constructor(
    private val prefabricadoDao: PrefabricadoDao,
    private val ingredienteDao: PrefabricadoIngredienteDao
) : NutricionEngine {

    override suspend fun calculateNutricionPrefabricado(prefabricadoId: Long): NutricionResumen {
        val prefabricado = prefabricadoDao.getById(prefabricadoId)
            ?: return NutricionResumen()

        val ingredientes = ingredienteDao.getByPrefabricadoOnce(prefabricadoId)
        val productosSinInfo = mutableListOf<String>()

        var totalCalorias = 0.0
        var totalProteinas = 0.0
        var totalCarbohidratos = 0.0
        var totalGrasas = 0.0
        var totalFibra = 0.0
        var totalSodio = 0.0
        var tieneAlgunDato = false

        for (item in ingredientes) {
            val producto = item.producto

            if (producto.esServicio) continue

            val porcionG = producto.nutricionPorcionG
            val calorias = producto.nutricionCalorias

            if (porcionG == null || porcionG <= 0 || calorias == null) {
                productosSinInfo.add(producto.nombre)
                continue
            }

            val cantidadEnGramos = convertirAGramos(
                item.ingrediente.cantidadUsada,
                item.ingrediente.unidadUsada
            )

            if (cantidadEnGramos == null) {
                productosSinInfo.add(producto.nombre)
                continue
            }

            val factor = cantidadEnGramos / porcionG
            tieneAlgunDato = true

            totalCalorias += (calorias) * factor
            producto.nutricionProteinasG?.let { totalProteinas += it * factor }
            producto.nutricionCarbohidratosG?.let { totalCarbohidratos += it * factor }
            producto.nutricionGrasasG?.let { totalGrasas += it * factor }
            producto.nutricionFibraG?.let { totalFibra += it * factor }
            producto.nutricionSodioMg?.let { totalSodio += it * factor }
        }

        if (!tieneAlgunDato) return NutricionResumen(productosSinInfo = productosSinInfo)

        val porciones = prefabricado.rendimientoPorciones
        if (porciones <= 0) return NutricionResumen(productosSinInfo = productosSinInfo)

        return NutricionResumen(
            calorias = totalCalorias / porciones,
            proteinas = totalProteinas / porciones,
            carbohidratos = totalCarbohidratos / porciones,
            grasas = totalGrasas / porciones,
            fibra = totalFibra / porciones,
            sodioMg = totalSodio / porciones,
            esCompleto = productosSinInfo.isEmpty(),
            productosSinInfo = productosSinInfo
        )
    }

    override fun convertirAGramos(cantidad: Double, unidad: String): Double? {
        val um = UnidadMedida.fromCodigo(unidad) ?: return null
        val factor = um.factorAGramos ?: return null
        return cantidad * factor
    }
}
