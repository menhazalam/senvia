package com.menhaz.senvia.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface TelegramApi {
    @GET("sendMessage")
    suspend fun sendMessage(
        @Query("chat_id") chatId: String,
        @Query("text") text: String
    ): Response<TelegramResponse>

    companion object {
        fun create(botToken: String): TelegramApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            return Retrofit.Builder()
                .baseUrl("https://api.telegram.org/bot$botToken/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TelegramApi::class.java)
        }
    }
}

data class TelegramResponse(
    val ok: Boolean,
    val result: Any? = null,
    val error_code: Int? = null,
    val description: String? = null
)