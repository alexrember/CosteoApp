package com.mg.costeoapp.feature.inventario.data.remote

import com.mg.costeoapp.feature.inventario.data.dto.BloomreachResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BloomreachApi {

    companion object {
        const val BASE_URL = "https://core.dxpapi.com/"
    }

    // Public keys from PriceSmart website JS. May rotate — consider Remote Config.
    @GET("api/v1/core/")
    suspend fun search(
        @Query("account_id") accountId: String = "6534",
        @Query("auth_key") authKey: String = "b1zxwk4lc88e8a8v",
        @Query("domain_key") domainKey: String = "pricesmart_com_sv",
        @Query("request_type") requestType: String = "search",
        @Query("search_type") searchType: String = "keyword",
        @Query("q") query: String,
        @Query("rows") rows: Int = 10,
        @Query("start") start: Int = 0,
        @Query("fl") fields: String = "pid,title,brand,price,sale_price,thumb_image,url"
    ): Response<BloomreachResponse>
}
