package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class OrchestratedSearchResult(
    val results: List<StoreSearchResult>,
    val storesSearched: List<String>,
    val storesTimedOut: List<String>,
    val storesFailed: List<String>,
    val totalTimeMs: Long
)

class StoreSearchOrchestrator @Inject constructor(
    private val walmartRepository: WalmartStoreRepository,
    private val priceSmartRepository: PriceSmartStoreRepository,
    private val superSelectosRepository: SuperSelectosRepository
) {
    companion object {
        private const val TIMEOUT_MS = 5000L
    }

    suspend fun searchByBarcode(barcode: String): OrchestratedSearchResult {
        return executeParallelSearch(
            walmartSearch = { walmartRepository.searchByBarcode(barcode) },
            priceSmartSearch = { priceSmartRepository.searchByBarcode(barcode) },
            superSelectosSearch = { superSelectosRepository.searchByBarcode(barcode) }
        )
    }

    suspend fun searchPriceSmartByName(query: String): List<StoreSearchResult> {
        return withTimeoutOrNull(TIMEOUT_MS) {
            priceSmartRepository.searchByName(query).getOrNull()
        } ?: emptyList()
    }

    suspend fun searchByName(query: String): OrchestratedSearchResult {
        return executeParallelSearch(
            walmartSearch = { walmartRepository.searchByName(query) },
            priceSmartSearch = { priceSmartRepository.searchByName(query) },
            superSelectosSearch = { superSelectosRepository.searchByName(query) }
        )
    }

    private suspend fun executeParallelSearch(
        walmartSearch: suspend () -> Result<List<StoreSearchResult>>,
        priceSmartSearch: suspend () -> Result<List<StoreSearchResult>>,
        superSelectosSearch: suspend () -> Result<List<StoreSearchResult>>
    ): OrchestratedSearchResult {
        val start = System.currentTimeMillis()
        val storesSearched = mutableListOf<String>()
        val storesTimedOut = mutableListOf<String>()
        val storesFailed = mutableListOf<String>()
        val allResults = mutableListOf<StoreSearchResult>()

        coroutineScope {
            val jobs = listOf(
                "Walmart SV" to async {
                    withTimeoutOrNull(TIMEOUT_MS) { walmartSearch() }
                },
                "PriceSmart" to async {
                    withTimeoutOrNull(TIMEOUT_MS) { priceSmartSearch() }
                },
                "Super Selectos" to async {
                    withTimeoutOrNull(TIMEOUT_MS) { superSelectosSearch() }
                }
            )

            for ((name, deferred) in jobs) {
                storesSearched.add(name)
                val result = deferred.await()
                when {
                    result == null -> storesTimedOut.add(name)
                    result.isFailure -> storesFailed.add(name)
                    else -> allResults.addAll(result.getOrDefault(emptyList()))
                }
            }
        }

        return OrchestratedSearchResult(
            results = allResults,
            storesSearched = storesSearched,
            storesTimedOut = storesTimedOut,
            storesFailed = storesFailed,
            totalTimeMs = System.currentTimeMillis() - start
        )
    }
}
