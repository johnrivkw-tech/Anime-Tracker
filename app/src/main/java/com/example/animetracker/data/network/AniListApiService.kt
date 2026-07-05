package com.example.animetracker.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * AniList exposes a single GraphQL endpoint rather than one REST route per
 * resource, so every call below is a POST of a query string + variables to
 * the same URL; [AniListRepository] decides which query text and variables
 * to send for a given screen.
 */
interface AniListApiService {

    @Headers("Accept: application/json")
    @POST(".")
    suspend fun searchMedia(@Body request: AniListRequest): AniListPageResponse

    @Headers("Accept: application/json")
    @POST(".")
    suspend fun getMedia(@Body request: AniListRequest): AniListMediaResponse

    @Headers("Accept: application/json")
    @POST(".")
    suspend fun getCharacters(@Body request: AniListRequest): AniListCharactersResponse
}

/**
 * Single Retrofit instance for the app. Like Jikan, AniList is public and
 * keyless for read-only queries like these — no auth setup needed here.
 */
object AniListApi {
    private const val BASE_URL = "https://graphql.anilist.co/"

    val service: AniListApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AniListApiService::class.java)
    }
}
