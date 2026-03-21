package com.mg.costeoapp.feature.inventario.data.remote

import com.mg.costeoapp.BuildConfig
import com.mg.costeoapp.feature.inventario.data.dto.BloomreachSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BloomreachSearchApi {

    companion object {
        const val BASE_URL = "https://core.dxpapi.com/"
    }

    @GET("api/v1/core/")
    suspend fun search(
        @Query("account_id") accountId: String = BuildConfig.BLOOMREACH_ACCOUNT_ID,
        @Query("auth_key") authKey: String = BuildConfig.BLOOMREACH_AUTH_KEY,
        @Query("domain_key") domainKey: String = "pricesmart_bloomreach_io_es",
        @Query("view_id") viewId: String = "SV",
        @Query("request_type") requestType: String = "search",
        @Query("search_type") searchType: String = "keyword",
        @Query("q") query: String,
        @Query("rows") rows: Int = 10,
        @Query("start") start: Int = 0,
        @Query("fl") fields: String = "pid,title,brand,price_SV,thumb_image,availability_SV",
        @Query("_br_uid_2") brUid: String = "costeoapp",
        @Query("url") url: String = "https://www.pricesmart.com/es-sv/busqueda",
        @Query("ref_url") refUrl: String = "https://www.pricesmart.com/es-sv"
    ): Response<BloomreachSearchResponse>
}
