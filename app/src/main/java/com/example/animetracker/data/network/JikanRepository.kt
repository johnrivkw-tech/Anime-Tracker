package com.example.animetracker.data.network

/**
 * Wraps calls to the Jikan API and turns network exceptions into a
 * [Result], so the ViewModel doesn't need to know about Retrofit or
 * HTTP exceptions.
 */
class JikanRepository {
    suspend fun searchAnime(query: String): Result<List<JikanAnimeResult>> {
        return try {
            val response = JikanApi.service.searchAnime(query = query)
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
