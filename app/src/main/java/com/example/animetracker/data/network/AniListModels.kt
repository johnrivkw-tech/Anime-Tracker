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
    val idMal: Int?,
    val title: AniListTitle,
    val episodes: Int?,
    val duration: Int?,
    val averageScore: Int?,
    val rawStatus: String?,
    val season: String?,
    val seasonYear: Int?,
    val description: String?,
    val coverImage: AniListCoverImage?,
    val bannerImage: String?,
    val genres: List<String> = emptyList(),
    val studios: AniListStudioConnection? = null,
    val trailer: AniListTrailer? = null
) {
    /** Prefers the English localized title, falling back through romaji to native script. */
    val displayTitle: String
        get() = title.english ?: title.romaji ?: title.native ?: "Untitled"

    /** Highest-resolution cover art AniList has for this title. */
    val posterUrl: String?
        get() = coverImage?.extraLarge ?: coverImage?.large

    /** Converted from AniList's 0-100 scale to the 0-10 scale the rest of the app displays. */
    val score: Double?
        get() = averageScore?.let { it / 10.0 }

    /** Human-readable status label, in the same style Jikan used to send. */
    val status: String?
        get() = when (rawStatus) {
            "FINISHED" -> "Finished Airing"
            "RELEASING" -> "Currently Airing"
            "NOT_YET_RELEASED" -> "Not Yet Aired"
            "CANCELLED" -> "Cancelled"
            "HIATUS" -> "On Hiatus"
            else -> null
        }

    /** Plain-text synopsis; AniList's raw description often has stray HTML in it. */
    val synopsis: String?
        get() = description?.cleanAniListDescription()

    /** Animation studio(s) only — the query excludes production/licensing companies. */
    val studioNames: List<String>
        get() = studios?.nodes.orEmpty().map { it.name }
}

data class AniListTitle(
    val romaji: String?,
    val english: String?,
    val native: String?
)

data class AniListCoverImage(
    val extraLarge: String?,
    val large: String?
)

data class AniListStudioConnection(
    val nodes: List<AniListStudio> = emptyList()
)

data class AniListStudio(
    val id: Int,
    val name: String
)

data class AniListTrailer(
    val id: String?,
    val site: String?
) {
    /** AniList only ever hosts trailers on YouTube or Dailymotion. */
    val videoUrl: String?
        get() = when (site?.lowercase()) {
            "youtube" -> id?.let { "https://www.youtube.com/watch?v=$it" }
            "dailymotion" -> id?.let { "https://www.dailymotion.com/video/$it" }
            else -> null
        }
}

private fun String.cleanAniListDescription(): String = this
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("</?(i|b|em|strong)>", RegexOption.IGNORE_CASE), "")
    .replace(Regex("<[^>]*>"), "")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#039;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .trim()

// --- Characters -------------------------------------------------------------

data class AniListCharacterEdge(
    val role: String?,
    val node: AniListCharacterNode
) {
    val displayName: String get() = node.name.full ?: "Unknown"
    val imageUrl: String? get() = node.image?.large
}

data class AniListCharacterNode(
    val id: Int,
    val name: AniListCharacterName,
    val image: AniListCharacterImage?
)

data class AniListCharacterName(val full: String?)

data class AniListCharacterImage(val large: String?)
