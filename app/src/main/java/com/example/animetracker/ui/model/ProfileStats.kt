package com.example.animetracker.ui.model

/** Aggregate stats shown on the Profile screen, derived from the local watchlist. */
data class ProfileStats(
    val totalAnime: Int = 0,
    val completed: Int = 0,
    val watching: Int = 0,
    val planToWatch: Int = 0,
    val favorites: Int = 0,
    val totalWatchMinutes: Long = 0
) {
    val watchDays: Long get() = totalWatchMinutes / (24 * 60)
    val watchHoursRemainder: Long get() = (totalWatchMinutes / 60) % 24
}
