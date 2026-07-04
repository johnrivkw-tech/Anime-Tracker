package com.example.animetracker.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApiService {
    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("sfw") sfw: Boolean = true
    ): JikanSearchResponse
}

/**
 * Single Retrofit instance for the app. Jikan is public and keyless,
 * so there's no auth setup needed here.
 */
object JikanApi {
    private const val BASE_URL = "https://api.jikan.moe/v4/"

    val service: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JikanApiService::class.java)
    }
}
