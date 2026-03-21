package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import kotlin.math.roundToLong

class SuperSelectosRepository @Inject constructor(
    private val client: OkHttpClient
) {

    companion object {
        private const val BASE_URL = "https://www.superselectos.com"
        private const val BRANCH_OFFICE_COOKIE =
            "BranchOfficeIdselectos=eyJCcmFuY2hPZmZpY2VJZCI6IjMzIiwiSXNIb21lRGVsaXZlcnkiOnRydWUsIkV4cGlyYXRpb25EYXRlIjoiMjAzMC0wMS0wMVQwMDowMDowMC4wMDAwMDAwLTA2OjAwIiwiSXNFeHBpcmVkIjpmYWxzZX0="
        private const val CACHE_DURATION_MS = 10 * 60 * 1000L

        private val PRODUCT_CARD_REGEX = Regex(
            """<div\s+class="info-prod">.*?<strong\s+class="precio"[^>]*>\$([0-9]+\.[0-9]{2})</strong>""" +
                """(?:.*?<span\s+class="antes"[^>]*>\$([0-9]+\.[0-9]{2})</span>)?""" +
                """.*?<h5\s+class="prod-nombre"><a[^>]*href="[^"]*\?productId=(\d+)"[^>]*>([^<]+)</a></h5>""",
            RegexOption.DOT_MATCHES_ALL
        )

        private val IMAGE_REGEX = Regex(
            """<a[^>]*href="[^"]*\?productId=(\d+)"[^>]*class="clickeable"[^>]*>""" +
                """\s*<img\s+src="([^"]+)"[^>]*/?>""",
            RegexOption.DOT_MATCHES_ALL
        )

        private val CATEGORIES_TO_FETCH = listOf("01", "03", "05")
    }

    @Volatile
    private var cachedProducts: List<StoreSearchResult> = emptyList()

    @Volatile
    private var cacheTimestamp: Long = 0L

    suspend fun searchByBarcode(barcode: String): Result<List<StoreSearchResult>> {
        return searchByName(barcode)
    }

    suspend fun searchByName(query: String): Result<List<StoreSearchResult>> {
        return try {
            val results = withContext(Dispatchers.IO) {
                val products = getCachedOrFetchProducts()

                val queryLower = query.lowercase()
                val queryWords = queryLower.split(" ").filter { it.length > 2 }

                if (queryWords.isEmpty()) {
                    return@withContext products.take(10)
                }

                products
                    .filter { product ->
                        val nameLower = product.productName.lowercase()
                        queryWords.any { word -> nameLower.contains(word) }
                    }
                    .take(15)
            }
            Result.success(results)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Sin conexion a internet"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Tiempo de espera agotado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getCachedOrFetchProducts(): List<StoreSearchResult> {
        val now = System.currentTimeMillis()
        if (cachedProducts.isNotEmpty() && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            return cachedProducts
        }

        val products = fetchAllProducts()
        cachedProducts = products
        cacheTimestamp = now
        return products
    }

    private suspend fun fetchAllProducts(): List<StoreSearchResult> = coroutineScope {
        val urls = listOf("$BASE_URL/") + CATEGORIES_TO_FETCH.map { "$BASE_URL/products?category=$it" }

        val results = urls.map { url ->
            async(Dispatchers.IO) {
                try {
                    fetchAndParseProducts(url)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll()

        results.flatten().distinctBy { it.productName }
    }

    private fun fetchAndParseProducts(url: String): List<StoreSearchResult> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CosteoApp/1.0 Android")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "es-SV,es;q=0.9")
            .header("Cookie", BRANCH_OFFICE_COOKIE)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val html = response.body.string()

        val imageMap = mutableMapOf<String, String>()
        IMAGE_REGEX.findAll(html).forEach { match ->
            val productId = match.groupValues[1]
            val imageUrl = match.groupValues[2]
            if (imageUrl.isNotBlank()) {
                imageMap[productId] = imageUrl
            }
        }

        return PRODUCT_CARD_REGEX.findAll(html).map { match ->
            val price = match.groupValues[1]
            val listPrice = match.groupValues[2].ifBlank { null }
            val productId = match.groupValues[3]
            val name = decodeHtmlEntities(match.groupValues[4].trim())

            val priceInCents = (price.toDouble() * 100).roundToLong()
            val listPriceInCents = listPrice?.let { (it.toDouble() * 100).roundToLong() }

            StoreSearchResult(
                storeName = "Super Selectos",
                productName = name,
                brand = null,
                ean = null,
                price = priceInCents,
                listPrice = listPriceInCents ?: priceInCents,
                isAvailable = true,
                imageUrl = imageMap[productId],
                measurementUnit = extractMeasurementUnit(name),
                unitMultiplier = extractUnitMultiplier(name),
                source = "superselectos_web"
            )
        }.toList()
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&#xE1;", "a")
            .replace("&#xE9;", "e")
            .replace("&#xED;", "i")
            .replace("&#xF3;", "o")
            .replace("&#xFA;", "u")
            .replace("&#xF1;", "n")
            .replace("&#xC1;", "A")
            .replace("&#xC9;", "E")
            .replace("&#xCD;", "I")
            .replace("&#xD3;", "O")
            .replace("&#xDA;", "U")
            .replace("&#xD1;", "N")
            .replace("&#xFC;", "u")
            .replace("&#xDC;", "U")
            .replace("&#x2B;", "+")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
    }

    private fun extractMeasurementUnit(name: String): String? {
        val unitRegex = Regex("""(\d+(?:\.\d+)?)\s*(kg|g|ml|mL|L|lb|oz)\b""", RegexOption.IGNORE_CASE)
        return unitRegex.find(name)?.groupValues?.get(2)?.lowercase()
    }

    private fun extractUnitMultiplier(name: String): Double? {
        val unitRegex = Regex("""(\d+(?:\.\d+)?)\s*(kg|g|ml|mL|L|lb|oz)\b""", RegexOption.IGNORE_CASE)
        return unitRegex.find(name)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
