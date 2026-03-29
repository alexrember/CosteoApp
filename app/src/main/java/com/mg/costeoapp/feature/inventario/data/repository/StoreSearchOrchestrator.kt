package com.mg.costeoapp.feature.inventario.data.repository

import android.util.Log
import com.mg.costeoapp.core.domain.model.StoreSearchResult
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
    private val backendRepository: CosteoBackendRepository
) {
    companion object {
        private const val TAG = "SearchOrchestrator"
        private const val TIMEOUT_MS = 8000L
    }

    suspend fun searchByBarcode(barcode: String): OrchestratedSearchResult {
        return executeBackendSearch { backendRepository.searchByBarcode(barcode) }
    }

    suspend fun searchByName(query: String): OrchestratedSearchResult {
        return executeBackendSearch { backendRepository.searchByName(query) }
    }

    private suspend fun executeBackendSearch(
        search: suspend () -> Result<List<StoreSearchResult>>
    ): OrchestratedSearchResult {
        val start = System.currentTimeMillis()
        return try {
            val result = withTimeoutOrNull(TIMEOUT_MS) { search() }
            when {
                result == null -> {
                    Log.w(TAG, "Backend timeout")
                    OrchestratedSearchResult(
                        results = emptyList(),
                        storesSearched = listOf("Backend Costeo"),
                        storesTimedOut = listOf("Backend Costeo"),
                        storesFailed = emptyList(),
                        totalTimeMs = System.currentTimeMillis() - start
                    )
                }
                result.isFailure -> {
                    Log.w(TAG, "Backend error: ${result.exceptionOrNull()?.message}")
                    OrchestratedSearchResult(
                        results = emptyList(),
                        storesSearched = listOf("Backend Costeo"),
                        storesTimedOut = emptyList(),
                        storesFailed = listOf("Backend Costeo"),
                        totalTimeMs = System.currentTimeMillis() - start
                    )
                }
                else -> OrchestratedSearchResult(
                    results = result.getOrDefault(emptyList()),
                    storesSearched = listOf("Backend Costeo"),
                    storesTimedOut = emptyList(),
                    storesFailed = emptyList(),
                    totalTimeMs = System.currentTimeMillis() - start
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend exception: ${e.message}", e)
            OrchestratedSearchResult(
                results = emptyList(),
                storesSearched = listOf("Backend Costeo"),
                storesTimedOut = emptyList(),
                storesFailed = listOf("Backend Costeo"),
                totalTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}
