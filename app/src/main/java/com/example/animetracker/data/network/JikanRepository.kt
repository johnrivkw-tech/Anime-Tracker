package com.example.animetracker.data.network

/**
 * Wraps calls to the Jikan API and turns network exceptions into a [Result],
 * so the ViewModel doesn't need to know about Retrofit or HTTP exceptions.
 *
 * A couple of the "personalized" home sections are simplified stand-ins for
 * true recommendation logic, since real recs need a signed-in MyAnimeList
 * account this app doesn't have:
 *  - "Recommended For You" uses Jikan's most-favorited list.
 *  - "New Releases" uses titles sorted by most recent start date.
 */
class JikanRepository {

    suspend fun searchAnime(query: String): Result<List<JikanAnimeResult>> = safeCall {
        JikanApi.service.searchAnime(query = query).data
    }

    suspend fun getTrending(): Result<List<JikanAnimeResult>> = safeCall {
        JikanApi.service.getTopAnime(filter = "bypopularity", limit = 10).data
    }

    suspend fun getPopularThisSeason(): Result<List<JikanAnimeResult>> = safeCall {
        JikanApi.service.getCurrentSeason(limit = 10).data
    }

    suspend fun getTopRated(): Result<List<JikanAnimeResult>> = safeCall {
        JikanApi.service.getTopAnime(limit = 10).data
    }

    suspend fun getNewReleases(): Result<List<JikanAnimeResult>> = safeCall {
        JikanApi.service.getAnimeList(orderBy = "start_date", sort = "desc", limit = 10).data
    }

    suspend fun getRecommended(): Result<List<JikanAnimeResult>> = safeCall {
        JikanApi.service.getTopAnime(filter = "favorite", limit = 10).data
    }

    suspend fun getAnimeDetails(malId: Int): Result<JikanAnimeResult> = safeCall {
        JikanApi.service.getAnimeFull(malId).data
    }

    suspend fun getAnimeCharacters(malId: Int): Result<List<JikanCharacterEntry>> = safeCall {
        JikanApi.service.getAnimeCharacters(malId).data
    }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
