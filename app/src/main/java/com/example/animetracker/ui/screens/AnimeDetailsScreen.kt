package com.example.animetracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.data.network.AniListCharacterEdge
import com.example.animetracker.data.network.AniListMedia
import com.example.animetracker.ui.theme.Blaze
import com.example.animetracker.ui.theme.Bone
import com.example.animetracker.ui.theme.Charcoal
import com.example.animetracker.ui.theme.CharcoalHigh
import com.example.animetracker.ui.theme.ErrorRed
import com.example.animetracker.ui.theme.Pulse
import com.example.animetracker.ui.theme.Smoke
import com.example.animetracker.ui.theme.Void
import com.example.animetracker.viewmodel.AnimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsScreen(
    viewModel: AnimeViewModel,
    aniListId: Int,
    onBack: () -> Unit
) {
    val details by viewModel.animeDetails.collectAsState()
    val isLoading by viewModel.isDetailsLoading.collectAsState()
    val error by viewModel.detailsError.collectAsState()
    val characters by viewModel.characters.collectAsState()
    val allLocal by viewModel.allLocalAnime.collectAsState()
    val context = LocalContext.current

    val localEntry = remember(allLocal, aniListId) { allLocal.firstOrNull { it.aniListId == aniListId } }

    LaunchedEffect(aniListId) {
        viewModel.loadAnimeDetails(aniListId)
        viewModel.loadAnimeCharacters(aniListId)
    }

    Scaffold(
        containerColor = Void,
        topBar = {
            TopAppBar(
                title = { Text(details?.displayTitle ?: "", color = Bone) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Bone)
                    }
                },
                actions = {
                    if (localEntry != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(localEntry) }) {
                            Icon(
                                imageVector = if (localEntry.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (localEntry.isFavorite) Pulse else Smoke
                            )
                        }
                    }
                    IconButton(onClick = {
                        val title = details?.displayTitle ?: "this anime"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out $title on AniList: https://anilist.co/anime/$aniListId"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = Bone)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Void,
                    titleContentColor = Bone
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading && details == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blaze)
                }
            }
            error != null && details == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = error ?: "", color = ErrorRed, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.loadAnimeDetails(aniListId) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blaze, contentColor = Bone)
                    ) {
                        Text("Retry")
                    }
                }
            }
            details != null -> {
                DetailsContent(
                    details = details!!,
                    localEntry = localEntry,
                    characters = characters,
                    paddingValues = paddingValues,
                    onSetStatus = { status -> viewModel.setAnimeStatus(details!!, localEntry, status) },
                    onRemove = { entry -> viewModel.deleteAnime(entry) },
                    onRate = { entry, rating -> viewModel.rateAnime(entry, rating) },
                    onMarkEpisodeWatched = { entry -> viewModel.incrementEpisode(entry) },
                    onWatchTrailer = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(
    details: AniListMedia,
    localEntry: Anime?,
    characters: List<AniListCharacterEdge>,
    paddingValues: PaddingValues,
    onSetStatus: (AnimeStatus) -> Unit,
    onRemove: (Anime) -> Unit,
    onRate: (Anime, Int) -> Unit,
    onMarkEpisodeWatched: (Anime) -> Unit,
    onWatchTrailer: (String) -> Unit
) {
    val scroll = rememberScrollState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            HeroSection(details = details)
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = details.displayTitle, style = MaterialTheme.typography.headlineSmall, color = Bone)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (details.score != null) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Pulse,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.2f", details.score),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Bone
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = details.status ?: "Unknown status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Smoke
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                val seasonYear = listOfNotNull(
                    details.season?.lowercase()?.replaceFirstChar { it.uppercase() },
                    details.seasonYear?.toString()
                ).joinToString(" ")
                val episodeText = if (details.episodes != null) "${details.episodes} episodes" else "Episodes unknown"
                Text(
                    text = listOfNotNull(seasonYear.ifBlank { null }, episodeText).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Smoke
                )

                if (details.studioNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Studio: ${details.studioNames.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Smoke
                    )
                }

                if (details.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        details.genres.forEach { genreName ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CharcoalHigh
                            ) {
                                Text(
                                    text = genreName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Bone,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                val trailerUrl = details.trailer?.videoUrl
                if (trailerUrl != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onWatchTrailer(trailerUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blaze, contentColor = Bone)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Trailer", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Synopsis", style = MaterialTheme.typography.titleMedium, color = Bone)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = details.synopsis ?: "No synopsis available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Smoke
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Your List", style = MaterialTheme.typography.titleMedium, color = Bone)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        item {
            StatusActionSection(
                localEntry = localEntry,
                onSetStatus = onSetStatus,
                onRemove = onRemove
            )
        }
        if (localEntry != null) {
            item {
                TrackingSection(
                    entry = localEntry,
                    onRate = onRate,
                    onMarkEpisodeWatched = onMarkEpisodeWatched
                )
            }
        }
        if (characters.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Characters",
                    style = MaterialTheme.typography.titleMedium,
                    color = Bone,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(characters.take(15), key = { it.node.id }) { entry ->
                        CharacterCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(details: AniListMedia) {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        AsyncImage(
            model = details.bannerImage ?: details.posterUrl,
            contentDescription = details.displayTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Void)
                    )
                )
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp)
                .offset(y = 40.dp)
                .size(width = 110.dp, height = 156.dp),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 0.dp
        ) {
            AsyncImage(
                model = details.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    Spacer(modifier = Modifier.height(44.dp))
}

@Composable
private fun StatusActionSection(
    localEntry: Anime?,
    onSetStatus: (AnimeStatus) -> Unit,
    onRemove: (Anime) -> Unit
) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimeStatus.entries.forEach { status ->
                FilterChip(
                    selected = localEntry?.status == status,
                    onClick = { onSetStatus(status) },
                    label = { Text(status.label) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = CharcoalHigh,
                        labelColor = Smoke,
                        selectedContainerColor = Blaze,
                        selectedLabelColor = Bone
                    )
                )
            }
        }
        if (localEntry != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onRemove(localEntry) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove from List")
            }
        }
    }
}

@Composable
private fun TrackingSection(
    entry: Anime,
    onRate: (Anime, Int) -> Unit,
    onMarkEpisodeWatched: (Anime) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(text = "Your Rating", style = MaterialTheme.typography.titleSmall, color = Bone)
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            for (i in 1..10) {
                Icon(
                    imageVector = if (i <= entry.rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Rate $i",
                    tint = Pulse,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onRate(entry, i) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        val progressText = if (entry.totalEpisodes > 0) {
            "Episode ${entry.episodesWatched} / ${entry.totalEpisodes}"
        } else {
            "Episode ${entry.episodesWatched}"
        }
        Text(text = "Progress: $progressText", style = MaterialTheme.typography.titleSmall, color = Bone)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onMarkEpisodeWatched(entry) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blaze, contentColor = Bone)
        ) {
            Text("Mark Episode Watched", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
private fun CharacterCard(entry: AniListCharacterEdge) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = entry.imageUrl,
            contentDescription = entry.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(72.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Bone,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
