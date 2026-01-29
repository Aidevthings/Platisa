package com.platisa.app.core.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {
    @GET("latest")
    suspend fun getLatestRate(
        @Query("from") from: String = "EUR",
        @Query("to") to: String = "RSD"
    ): CurrencyResponse
}

data class CurrencyResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
