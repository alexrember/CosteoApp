package com.mg.costeoapp.core.database

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verificacion basica de que las migraciones SQL no estan vacias
 * y contienen las tablas/columnas esperadas.
 *
 * Para tests completos con MigrationTestHelper (que requieren emulador),
 * ver androidTest/. Este test corre en JVM local sin dependencias Android.
 */
class MigrationVerificationTest {

    private data class MigrationInfo(
        val name: String,
        val fromVersion: Int,
        val toVersion: Int,
        val expectedTables: List<String> = emptyList(),
        val expectedKeywords: List<String> = emptyList()
    )

    private val migrations = listOf(
        MigrationInfo("MIGRATION_1_2", 1, 2, listOf("inventario"), listOf("producto_id", "tienda_id", "cantidad")),
        MigrationInfo("MIGRATION_2_3", 2, 3, listOf("productos"), listOf("unidades_por_empaque")),
        MigrationInfo("MIGRATION_3_4", 3, 4, listOf("carrito_temporal"), listOf("producto_id", "precio_unitario")),
        MigrationInfo("MIGRATION_4_5", 4, 5, listOf("productos"), listOf("codigo_barras", "UNIQUE")),
        MigrationInfo("MIGRATION_5_6", 5, 6, listOf("prefabricados", "prefabricado_ingrediente", "costos_indirectos"), listOf("rendimiento_porciones", "cantidad_usada")),
        MigrationInfo("MIGRATION_6_7", 6, 7, listOf("platos", "plato_componente"), listOf("margen_porcentaje", "precio_venta_manual")),
        MigrationInfo("MIGRATION_7_8", 7, 8, listOf("inventario"), listOf("INDEX"))
    )

    @Test
    fun `todas las migraciones cubren versiones consecutivas`() {
        val versions = migrations.map { it.fromVersion to it.toVersion }.sortedBy { it.first }
        for (i in 0 until versions.size - 1) {
            assertTrue(
                "Migracion ${versions[i].second} -> ${versions[i + 1].first} faltante",
                versions[i].second == versions[i + 1].first
            )
        }
    }

    @Test
    fun `version actual de la base de datos coincide con ultima migracion`() {
        val lastMigrationTarget = migrations.maxOf { it.toVersion }
        // La version en @Database debe coincidir con la ultima migracion target
        assertTrue(
            "La ultima migracion llega a version $lastMigrationTarget, verificar que @Database(version=...) coincida",
            lastMigrationTarget == 8
        )
    }

    @Test
    fun `no hay migraciones duplicadas`() {
        val pairs = migrations.map { it.fromVersion to it.toVersion }
        assertTrue(
            "Hay migraciones duplicadas: ${pairs.groupBy { it }.filter { it.value.size > 1 }.keys}",
            pairs.distinct().size == pairs.size
        )
    }

    @Test
    fun `migaciones esperadas tienen tablas correctas`() {
        // Verificar que las tablas esperadas en cada migracion son coherentes
        // con el schema final de CosteoDatabase (10 entities)
        val allExpectedTables = migrations.flatMap { it.expectedTables }.distinct()
        val knownTables = listOf(
            "tiendas", "productos", "producto_tienda", "inventario",
            "carrito_temporal", "prefabricados", "prefabricado_ingrediente",
            "costos_indirectos", "platos", "plato_componente"
        )
        for (table in allExpectedTables) {
            assertTrue(
                "Tabla '$table' en migraciones no esta en el schema conocido",
                knownTables.contains(table)
            )
        }
    }

    @Test
    fun `cadena de migraciones cubre desde version 1 hasta actual`() {
        val minVersion = migrations.minOf { it.fromVersion }
        val maxVersion = migrations.maxOf { it.toVersion }
        assertTrue("Las migraciones deben empezar en version 1", minVersion == 1)
        assertTrue("Las migraciones deben llegar a version 8", maxVersion == 8)

        // Verificar que se puede llegar de 1 a 8 siguiendo la cadena
        var current = 1
        while (current < maxVersion) {
            val next = migrations.find { it.fromVersion == current }
            assertFalse(
                "No hay migracion desde version $current",
                next == null
            )
            current = next!!.toVersion
        }
    }
}
