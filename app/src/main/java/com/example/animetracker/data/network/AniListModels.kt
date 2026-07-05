package com.example.animetracker.data.network

/**
 * Data models for the AniList GraphQL API (https://anilist.co/graphiql).
 * AniList replaces Jikan (MyAnimeList) here mainly for image quality:
 * [AniListCoverImage.extraLarge] is noticeably sharper than what Jikan
 * served, and every title carries a proper widescreen
 * [AniListMedia.bannerImage] for the Home feed's Featured banner, which
 * Jikan has no equivalent of.
 *
 * A handful of computed properties below (displayTitle, posterUrl, score,
 * status, synopsis, studioNames) exist so the rest of the app can keep
 * reading a flat, friendly shape instead of AniList's raw GraphQL fields.
 */

// --- Request / response envelopes ---------------------------------------

data class AniListRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)

data class AniListError(val message: String? = null)

data class AniListPageResponse(
    val data: AniListPageData?,
    val errors: List<AniListError>? = null
)
data class AniListPageData(val Page: AniListPage?)
data class AniListPage(val media: List<AniListMedia> = emptyList())

data class AniListMediaResponse(
    val data: AniListMediaData?,
    val errors: List<AniListError>? = null
)
data class AniListMediaData(val Media: AniListMedia?)

data class AniListCharactersResponse(
    val data: AniListCharactersData?,
    val errors: List<AniListError>? = null
)
data class AniListCharactersData(val Media: AniListCharacterMedia?)
data class AniListCharacterMedia(val characters: AniListCharacterConnection?)
data class AniListCharacterConnection(val edges: List<AniListCharacterEdge> = emptyList())

// --- Core anime model -----------------------------------------------------

data class AniListMedia(
    val id: Int,
    val idMal:
