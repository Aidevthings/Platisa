package com.platisa.app.core.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class FeedbackRequest(
    val email: String,
    val message: String,
    val _subject: String, // Special Formspree field
    val device_info: String
)

interface FeedbackApi {
    @POST
    suspend fun sendFeedback(@Url url: String, @Body feedback: FeedbackRequest)
}
