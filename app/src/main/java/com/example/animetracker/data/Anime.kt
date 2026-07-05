package com.example.animetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single anime entry in the user's watchlist.
 *
 * [totalEpisodes] of 0 means "unknown / not set". [rating] of 0 means
 * "not rated yet" (valid ratings are 1-10). [imageUrl], [aniListId], and
 * [episodeDurationMinutes] are filled in automatically when added via
 * online search, and stay null for manually-added entries.
 * [episodeDurationMinutes] is AniList's per-episode runtime; it's what the
 * Profile screen uses to turn episodes watched into a days/hours watch-time
 * stat. Entries without it (manual adds, or titles AniList didn't report a
 * runtime for) fall back to a flat 24 minutes/episode.
 */
@Entity(tableName = "anime_table")
data class Anime(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val episodesWatched: Int = 0,
    val totalEpisodes: Int = 0,
    val status: AnimeStatus = AnimeStatus.PLAN_TO_WATCH,
    val rating: Int = 0,
    val imageUrl: String? = null,
    val aniListId: Int? = null,
    val isFavorite: Boolean = false,
    val episodeDurationMinutes: Int? = null
)
