package com.mg.costeoapp.feature.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.feature.platos.data.ComponenteDetalle
import com.mg.costeoapp.feature.platos.data.TipoComponente
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun exportPlatoDetail(
        plato: Plato,
        componentes: List<ComponenteDetalle>,
        costoTotal: Long,
        precioVenta: Long?
    ): Uri? {
        return try {
            context.cacheDir.listFiles()?.filter { it.name.startsWith("platos_") || it.name.startsWith("plato_") }?.forEach { it.delete() }
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
            val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
            val textPaint = Paint().apply { textSize = 12f }
            val smallPaint = Paint().apply { textSize = 10f; color = 0xFF666666.toInt() }

            var y = 40f
            val margin = 40f

            // Titulo
            canvas.drawText(plato.nombre, margin, y, titlePaint)
            y += 25f
            plato.descripcion?.let {
                canvas.drawText(it, margin, y, smallPaint)
                y += 20f
            }
            y += 15f

            // Costo y precio
            canvas.drawText("Costo total: ${CurrencyFormatter.fromCents(costoTotal)}", margin, y, headerPaint)
            y += 20f
            precioVenta?.let {
                canvas.drawText("Precio de venta: ${CurrencyFormatter.fromCents(it)}", margin, y, headerPaint)
                y += 20f
            }
            plato.margenPorcentaje?.let {
                canvas.drawText("Margen: ${it}%", margin, y, textPaint)
                y += 20f
            }
            y += 15f

            // Componentes
            canvas.drawText("Componentes", margin, y, headerPaint)
            y += 20f

            for ((index, comp) in componentes.withIndex()) {
                val tipo = if (comp.tipo == TipoComponente.PREFABRICADO) "Receta" else "Producto"
                canvas.drawText("${comp.nombre} ($tipo)", margin, y, textPaint)
                y += 15f
                val costoText = comp.costoTotal?.let { CurrencyFormatter.fromCents(it) } ?: "Sin precio"
                canvas.drawText("  Cantidad: ${comp.componente.cantidad} — Costo: $costoText", margin, y, smallPaint)
                y += 18f

                if (y > 780f) {
                    val remaining = componentes.size - index - 1
                    if (remaining > 0) {
                        canvas.drawText("... y $remaining componentes mas", margin, y, smallPaint)
                    }
                    break
                }
            }

            doc.finishPage(page)

            val file = File(context.cacheDir, "plato_${plato.id}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("Export", "Error exportando PDF", e)
            null
        }
    }
}
