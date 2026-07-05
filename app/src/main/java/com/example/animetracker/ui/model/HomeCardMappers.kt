package com.example.animetracker.ui.model

import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.data.network.AniListMedia

fun AniListMedia.toHomeCardItem(localStatus: AnimeStatus?): HomeCardItem = HomeCardItem(
    key = "anilist_$id",
    aniListId = id,
    title = displayTitle,
    imageUrl = posterUrl,
    bannerUrl = bannerImage,
    score = score,
    statusLabel = localStatus?.label,
    progressText = null
)

fun Anime.toHomeCardItem(): HomeCardItem = HomeCardItem(
    key = "local_$id",
    aniListId = aniListId,
    title = name,
    imageUrl = imageUrl,
    bannerUrl = null,
    score = null,
    statusLabel = status.label,
    progressText = if (totalEpisodes > 0) "Ep $episodesWatched / $totalEpisodes" else "Ep $episodesWatched"
)
