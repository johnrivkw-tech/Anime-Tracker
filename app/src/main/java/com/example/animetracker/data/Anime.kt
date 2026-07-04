package com.example.animetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single anime entry in the user's watchlist.
 *
 * [totalEpisodes] of 0 means "unknown / not set". [rating] of 0 means
 * "not rated yet" (valid ratings are 1-10).
 */
@Entity(tableName = "anime_table")
data class Anime(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val episodesWatched: Int = 0,
    val totalEpisodes: Int = 0,
    val status: AnimeStatus = AnimeStatus.PLAN_TO_WATCH,
    val rating: Int = 0
)
