package com.example.animetracker.data.network

import java.time.LocalDate

/** AniList's fixed genre vocabulary, used to populate the Discover filter chips. */
val ANILIST_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror",
    "Mahou Shoujo", "Mecha", "Music", "Mystery", "Psychological", "Romance",
    "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller"
)

// GraphQL field selection shared by every query below, aliasing `status` to
// `rawStatus` so AniListMedia can expose its own human-readable `status`.
private const val MEDIA_FIELDS = """
    id
    idMal
    title { romaji english native }
    episodes
    duration
    averageScore
    rawStatus: status
    season
    seasonYear
    description
    coverImage { extraLarge large }
    bannerImage
    genres
    studios(isMain: true) { nodes { id name } }
    trailer { id site }
"""

private val SEARCH_QUERY = """
    query(${'$'}search: String, ${'$'}sort: [MediaSort], ${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}perPage: Int) {
      Page(perPage: ${'$'}perPage) {
        media(
          search: ${'$'}search
          sort: ${'$'}sort
          season: ${'$'}season
          seasonYear: ${'$'}seasonYear
          type: ANIME
          isAdult: false
        ) {
          $MEDIA_FIELDS
        }
      }
    }
""".trimIndent()

private val DETAILS_QUERY = """
    query(${'$'}id: Int) {
      Media(id: ${'$'}id, type: ANIME) {
        $MEDIA_FIELDS
      }
    }
""".trimIndent()

private val DISCOVER_QUERY = """
    query(${'$'}genre: String, ${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}sort: [MediaSort], ${'$'}perPage: Int, ${'$'}page: Int) {
      Page(page: ${'$'}page, perPage: ${'$'}perPage) {
        media(
          genre: ${'$'}genre
          season: ${'$'}season
          seasonYear: ${'$'}seasonYear
          sort: ${'$'}sort
          type: ANIME
          isAdult: false
        ) {
          $MEDIA_FIELDS
        }
      }
    }
""".trimIndent()

private val CHARACTERS_QUERY = """
    query(${'$'}id: Int) {
      Media(id: ${'$'}id, type: ANIME) {
        characters(sort: [ROLE], perPage: 15) {
          edges {
            role
            node {
              id
              name { full }
              image { large }
            }
          }
        }
      }
    }
""".trimIndent()

/**
 * Wraps calls to the AniList GraphQL API and turns network/GraphQL errors
 * into a [Result], so the ViewModel doesn't need to know about Retrofit,
 * GraphQL, or HTTP exceptions.
 *
 * AniList has no key-less "current season" endpoint the way Jikan's
 * `/seasons/now` did, so [currentSeason] works out today's quarter
 * (Winter = Jan-Mar, Spring = Apr-Jun, Summer = Jul-Sep, Fall = Oct-Dec)
 * client-side and passes it as the season/seasonYear filter.
 *
 * A couple of the "personalized" home sections are simplified stand-ins for
 * true recommendation logic, since real recs need a signed-in AniList
 * account this app doesn't have:
 *  - "Recommended For You" uses AniList's most-favorited list.
 *  - "New Releases" uses titles sorted by most recent start date.
 *
 * AniList needs no API key for any of this, but it does rate-limit by IP
 * (docs list anywhere from 30 to 90 requests/minute depending on current
 * load), so avoid hammering it in a tight loop.
 */
class AniListRepository {

    suspend fun searchAnime(query: String): Result<List<AniListMedia>> = safeCall {
        val response = AniListApi.service.searchMedia(
            AniListRequest(
                query = SEARCH_QUERY,
                variables = mapOf("search" to query, "perPage" to 10)
            )
        )
        checkErrors(response.errors)
        response.data?.Page?.media ?: emptyList()
    }

    suspend fun getTrending(): Result<List<AniListMedia>> = safeCall {
        fetchList(sort = "TRENDING_DESC")
    }

    suspend fun getPopularThisSeason(): Result<List<AniListMedia>> = safeCall {
        val (season, year) = currentSeason()
        fetchList(sort = "POPULARITY_DESC", season = season, seasonYear = year)
    }

    suspend fun getTopRated(): Result<List<AniListMedia>> = safeCall {
        fetchList(sort = "SCORE_DESC")
    }

    suspend fun getNewReleases(): Result<List<AniListMedia>> = safeCall {
        fetchList(sort = "START_DATE_DESC")
    }

    suspend fun getRecommended(): Result<List<AniListMedia>> = safeCall {
        fetchList(sort = "FAVOURITES_DESC")
    }

    /** Browse the catalog by genre/season/year for the Discover tab, sorted by popularity. */
    suspend fun discoverAnime(
        genre: String?,
        season: String?,
        seasonYear: Int?,
        page: Int = 1
    ): Result<List<AniListMedia>> = safeCall {
        val response = AniListApi.service.searchMedia(
            AniListRequest(
                query = DISCOVER_QUERY,
                variables = mapOf(
                    "genre" to genre,
                    "season" to season,
                    "seasonYear" to seasonYear,
                    "sort" to listOf("POPULARITY_DESC"),
                    "perPage" to 30,
                    "page" to page
                )
            )
        )
        checkErrors(response.errors)
        response.data?.Page?.media ?: emptyList()
    }

    suspend fun getAnimeDetails(aniListId: Int): Result<AniListMedia> = safeCall {
        val response = AniListApi.service.getMedia(
            AniListRequest(query = DETAILS_QUERY, variables = mapOf("id" to aniListId))
        )
        checkErrors(response.errors)
        response.data?.Media ?: throw IllegalStateException("Anime $aniListId not found")
    }

    suspend fun getAnimeCharacters(aniListId: Int): Result<List<AniListCharacterEdge>> = safeCall {
        val response = AniListApi.service.getCharacters(
            AniListRequest(query = CHARACTERS_QUERY, variables = mapOf("id" to aniListId))
        )
        checkErrors(response.errors)
        response.data?.Media?.characters?.edges ?: emptyList()
    }

    private suspend fun fetchList(
        sort: String,
        season: String? = null,
        seasonYear: Int? = null
    ): List<AniListMedia> {
        val response = AniListApi.service.searchMedia(
            AniListRequest(
                query = SEARCH_QUERY,
                variables = mapOf(
                    "sort" to listOf(sort),
                    "season" to season,
                    "seasonYear" to seasonYear,
                    "perPage" to 10
                )
            )
        )
        checkErrors(response.errors)
        return response.data?.Page?.media ?: emptyList()
    }

    /** The current quarterly anime season, e.g. ("SUMMER", 2026). */
    private fun currentSeason(): Pair<String, Int> {
        val today = LocalDate.now()
        val season = when (today.monthValue) {
            in 1..3 -> "WINTER"
            in 4..6 -> "SPRING"
            in 7..9 -> "SUMMER"
            else -> "FALL"
        }
        return season to today.year
    }

    private fun checkErrors(errors: List<AniListError>?) {
        if (!errors.isNullOrEmpty()) {
            throw IllegalStateException(errors.first().message ?: "AniList API error")
        }
    }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
