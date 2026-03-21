package com.mg.costeoapp.feature.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.DateFormatter
import com.mg.costeoapp.feature.platos.data.PlatoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

// Export XLSX diferido a backend (Fase 7+)
// Apache POI agrega ~5MB al APK. Mejor generar XLSX en servidor Supabase Edge Function.
// Por ahora CSV con BOM es compatible con Excel y es suficiente para exportar datos.
@Singleton
class CsvExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val platoRepository: PlatoRepository
) {

    suspend fun exportPlatos(platos: List<Plato>): Uri? {
        return try {
            context.cacheDir.listFiles()?.filter { it.name.startsWith("platos_") || it.name.startsWith("plato_") }?.forEach { it.delete() }
            val file = File(context.cacheDir, "platos_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                // BOM para Excel
                writer.write("\uFEFF")
                // Header
                writer.write("Nombre,Descripcion,Costo Total,Margen %,Precio Venta,Fecha Creacion\n")
                // Data
                for (plato in platos) {
                    val costeo = platoRepository.calculateCost(plato.id)
                    val precioVenta = platoRepository.calculatePrecioVenta(plato, costeo.costoTotal)
                    writer.write(
                        "${escapeCsv(plato.nombre)}," +
                        "${escapeCsv(plato.descripcion ?: "")}," +
                        "${CurrencyFormatter.fromCents(costeo.costoTotal)}," +
                        "${plato.margenPorcentaje ?: ""}," +
                        "${precioVenta?.let { CurrencyFormatter.fromCents(it) } ?: ""}," +
                        "${DateFormatter.formatDate(plato.createdAt)}\n"
                    )
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("Export", "Error exportando CSV", e)
            null
        }
    }

    fun shareFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir platos").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun escapeCsv(value: String): String {
        val sanitized = if (value.isNotEmpty() && value[0] in charArrayOf('=', '+', '-', '@')) {
            "'$value"
        } else value
        return if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else sanitized
    }
}
