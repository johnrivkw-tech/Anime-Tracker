package com.example.animetracker.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApiService {

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("sfw") sfw: Boolean = true
    ): JikanSearchResponse

    @GET("anime")
    suspend fun getAnimeList(
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("sfw") sfw: Boolean = true
    ): JikanSearchResponse

    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("filter") filter: String? = null,
        @Query("limit") limit: Int = 10
    ): JikanSearchResponse

    @GET("seasons/now")
    suspend fun getCurrentSeason(
        @Query("limit") limit: Int = 10
    ): JikanSearchResponse

    @GET("anime/{id}/full")
    suspend fun getAnimeFull(@Path("id") id: Int): JikanAnimeFullResponse

    @GET("anime/{id}/characters")
    suspend fun getAnimeCharacters(@Path("id") id: Int): JikanCharactersResponse
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
