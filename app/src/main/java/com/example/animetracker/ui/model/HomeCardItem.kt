package com.example.animetracker.ui.model

/**
 * Unified shape used to render poster cards on the Home feed, regardless of
 * whether the data came from the online AniList catalog or the local
 * watchlist (used for "Continue Tracking"). [bannerUrl] is only ever
 * populated from AniList (not every title has one), so the Featured banner
 * falls back to [imageUrl] when it's null.
 */
data class HomeCardItem(
    val key: String,
    val aniListId: Int?,
    val title: String,
    val imageUrl: String?,
    val bannerUrl: String?,
    val score: Double?,
    val statusLabel: String?,
    val progressText: String?
)
