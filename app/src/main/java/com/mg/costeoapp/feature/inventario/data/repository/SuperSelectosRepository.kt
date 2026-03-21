package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Super Selectos — Deshabilitado temporalmente.
 *
 * Super Selectos usa Blazor Server + SignalR WebSocket para búsqueda y catálogo.
 * No tiene API REST pública ni búsqueda por URL. El scraping de categorías HTML
 * solo cubre ~100 productos de 3 categorías, insuficiente para búsqueda útil.
 *
 * TODO Fase 8: Implementar scraping server-side con Playwright desde backend,
 * o integrar cuando Super Selectos exponga API pública.
 */
class SuperSelectosRepository @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun searchByBarcode(barcode: String): Result<List<StoreSearchResult>> {
        return Result.success(emptyList())
    }

    suspend fun searchByName(query: String): Result<List<StoreSearchResult>> {
        return Result.success(emptyList())
    }
}
