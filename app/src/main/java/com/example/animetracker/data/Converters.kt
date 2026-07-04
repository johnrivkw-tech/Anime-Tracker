package com.example.animetracker.data

import androidx.room.TypeConverter

/**
 * Tells Room how to store an [AnimeStatus] enum in a plain TEXT column
 * (as its name) and how to read it back.
 */
class Converters {
    @TypeConverter
    fun fromAnimeStatus(status: AnimeStatus): String = status.name

    @TypeConverter
    fun toAnimeStatus(value: String): AnimeStatus =
        AnimeStatus.entries.firstOrNull { it.name == value } ?: AnimeStatus.PLAN_TO_WATCH
}
