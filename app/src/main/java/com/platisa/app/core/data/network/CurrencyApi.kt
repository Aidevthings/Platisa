package com.platisa.app.core.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {
    @retrofit2.http.GET("latest/{base}")
    suspend fun getLatestRate(
        @retrofit2.http.Path("base") base: String
    ): CurrencyResponse
}

data class CurrencyResponse(
    @com.google.gson.annotations.SerializedName("base_code")
    val base: String,
    @com.google.gson.annotations.SerializedName("time_last_update_utc")
    val date: String,
    val rates: Map<String, Double>
)
