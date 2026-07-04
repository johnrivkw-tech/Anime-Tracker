package com.example.animetracker.ui.model

/**
 * Unified shape used to render poster cards on the Home feed, regardless of
 * whether the data came from the online Jikan catalog or the local
 * watchlist (used for "Continue Tracking").
 */
data class HomeCardItem(
    val key: String,
    val malId: Int?,
    val title: String,
    val imageUrl: String?,
    val score: Double?,
    val statusLabel: String?,
    val progressText: String?
)
