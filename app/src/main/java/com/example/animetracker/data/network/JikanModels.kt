package com.example.animetracker.data.network

/**
 * Minimal data models mirroring the parts of the Jikan API (jikan.moe)
 * response this app uses. Jikan wraps the MyAnimeList database and needs
 * no API key.
 */
data class JikanSearchResponse(
    val data: List<JikanAnimeResult> = emptyList()
)

data class JikanAnimeResult(
    val mal_id: Int,
    val title: String,
    val episodes: Int?,
    val images: JikanImages
)

data class JikanImages(
    val jpg: JikanImageUrls
)

data class JikanImageUrls(
    val image_url: String?,
    val large_image_url: String?
)
