package com.mg.costeoapp.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MigrationTest {

    @Test
    fun `MIGRATION_8_9 existe y tiene versiones correctas`() {
        val migration = CosteoDatabase.MIGRATION_8_9
        assertNotNull(migration)
        assertEquals(8, migration.startVersion)
        assertEquals(9, migration.endVersion)
    }

    @Test
    fun `MIGRATION_9_10 existe y tiene versiones correctas`() {
        val migration = CosteoDatabase.MIGRATION_9_10
        assertNotNull(migration)
        assertEquals(9, migration.startVersion)
        assertEquals(10, migration.endVersion)
    }

    @Test
    fun `cadena de migraciones 8 a 10 es continua`() {
        val m89 = CosteoDatabase.MIGRATION_8_9
        val m910 = CosteoDatabase.MIGRATION_9_10
        assertEquals(m89.endVersion, m910.startVersion)
    }

    @Test
    fun `todas las migraciones de CosteoDatabase cubren de 1 a 10`() {
        val migrations = listOf(
            CosteoDatabase.MIGRATION_1_2,
            CosteoDatabase.MIGRATION_2_3,
            CosteoDatabase.MIGRATION_3_4,
            CosteoDatabase.MIGRATION_4_5,
            CosteoDatabase.MIGRATION_5_6,
            CosteoDatabase.MIGRATION_6_7,
            CosteoDatabase.MIGRATION_7_8,
            CosteoDatabase.MIGRATION_8_9,
            CosteoDatabase.MIGRATION_9_10
        )
        val sorted = migrations.sortedBy { it.startVersion }
        assertEquals(1, sorted.first().startVersion)
        assertEquals(10, sorted.last().endVersion)
        for (i in 0 until sorted.size - 1) {
            assertEquals(
                "Hueco entre migracion ${sorted[i].startVersion}->${sorted[i].endVersion} y ${sorted[i + 1].startVersion}->${sorted[i + 1].endVersion}",
                sorted[i].endVersion,
                sorted[i + 1].startVersion
            )
        }
    }
}
