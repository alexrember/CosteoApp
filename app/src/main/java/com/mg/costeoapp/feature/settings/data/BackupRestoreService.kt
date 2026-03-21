package com.mg.costeoapp.feature.settings.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mg.costeoapp.core.database.CosteoDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestoreService @Inject constructor(
    private val database: CosteoDatabase
) {

    companion object {
        private const val DB_NAME = "costeo_database"
        private const val SQLITE_MAGIC = "SQLite format 3"
    }

    fun exportDatabase(context: Context): Uri? {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) return null

        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }

        val exportDir = File(context.cacheDir, "backups")
        exportDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        val exportFile = File(exportDir, "costeo_backup_$timestamp.db")

        dbFile.copyTo(exportFile, overwrite = true)

        val walFile = File(dbFile.path + "-wal")
        if (walFile.exists()) {
            File(exportFile.path + "-wal").delete()
        }
        val shmFile = File(dbFile.path + "-shm")
        if (shmFile.exists()) {
            File(exportFile.path + "-shm").delete()
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
    }

    fun importDatabase(context: Context, sourceUri: Uri): Result<Unit> {
        return try {
            val tempFile = File(context.cacheDir, "import_temp.db")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("No se pudo leer el archivo seleccionado"))

            if (!isValidSqliteFile(tempFile)) {
                tempFile.delete()
                return Result.failure(Exception("El archivo no es una base de datos SQLite valida"))
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            val bakFile = File(dbFile.path + ".bak")

            if (dbFile.exists()) {
                dbFile.copyTo(bakFile, overwrite = true)
            }

            try {
                database.close()

                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                walFile.delete()
                shmFile.delete()

                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()

                Result.success(Unit)
            } catch (e: Exception) {
                if (bakFile.exists()) {
                    bakFile.copyTo(dbFile, overwrite = true)
                    bakFile.delete()
                }
                Result.failure(Exception("Error al importar: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al importar la base de datos: ${e.message}"))
        }
    }

    private fun isValidSqliteFile(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(16)
                val bytesRead = input.read(header)
                if (bytesRead < 16) return false
                val headerString = String(header, 0, 15, Charsets.US_ASCII)
                headerString == SQLITE_MAGIC
            }
        } catch (_: Exception) {
            false
        }
    }
}
