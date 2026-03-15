package com.mg.costeoapp.core.database

import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        seedTiendas(db, now)
    }

    private fun seedTiendas(db: SupportSQLiteDatabase, now: Long) {
        val tiendas = listOf("Super Selectos", "PriceSmart", "Walmart")
        tiendas.forEach { nombre ->
            db.execSQL(
                "INSERT INTO tiendas (nombre, activo, created_at, updated_at) VALUES (?, 1, ?, ?)",
                arrayOf<Any>(nombre, now, now)
            )
        }
    }
}
