package com.example.animetracker.ui.model

import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.data.network.JikanAnimeResult

fun JikanAnimeResult.toHomeCardItem(localStatus: AnimeStatus?): HomeCardItem = HomeCardItem(
    key = "mal_$mal_id",
    malId = mal_id,
    title = title,
    imageUrl = images.jpg.large_image_url ?: images.jpg.image_url,
    score = score,
    statusLabel = localStatus?.label,
    progressText = null
)

fun Anime.toHomeCardItem(): HomeCardItem = HomeCardItem(
    key = "local_$id",
    malId = malId,
    title = name,
    imageUrl = imageUrl,
    score = null,
    statusLabel = status.label,
    progressText = if (totalEpisodes > 0) "Ep $episodesWatched / $totalEpisodes" else "Ep $episodesWatched"
)
