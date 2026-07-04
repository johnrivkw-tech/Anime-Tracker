package com.example.animetracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.animetracker.data.network.JikanAnimeResult
import com.example.animetracker.data.network.JikanCharacterEntry
import com.example.animetracker.viewmodel.AnimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsScreen(
    viewModel: AnimeViewModel,
    malId: Int,
    onBack: () -> Unit
) {
    val details by viewModel.animeDetails.collectAsState()
    val isLoading by viewModel.isDetailsLoading.collectAsState()
    val error by viewModel.detailsError.collectAsState()
    val characters by viewModel.characters.collectAsState()
    val allLocal by viewModel.allLocalAnime.collectAsState()
    val context = LocalContext.current

    val localEntry = remember(allLocal, malId) { allLocal.firstOrNull { it.malId == malId } }

    LaunchedEffect(malId) {
        viewModel.loadAnimeDetails(malId)
        viewModel.loadAnimeCharacters(malId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (localEntry != null) {
                        IconButton(onClick = { viewModel.toggleFavorite(localEntry) }) {
                            Icon(
                                imageVector = if (localEntry.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (localEntry.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = {
                        val title = details?.title ?: "this anime"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out $title on MyAnimeList: https://myanimelist.net/anime/$malId"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading && details == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && details == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadAnimeDetails(malId) }) {
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
    details: JikanAnimeResult,
    localEntry: Anime?,
    characters: List<JikanCharacterEntry>,
    paddingValues: PaddingValues,
    onSetStatus: (AnimeStatus) -> Unit,
    onRemove: (Anime) -> Unit,
    onRate: (Anime, Int) -> Unit,
    onMarkEpisodeWatched: (Anime) -> Unit,
    onWatchTrailer: (String) -> Unit
) {
    val scroll = rememberScrollState()

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            HeroSection(details = details)
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = details.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (details.score != null) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = String.format("%.2f", details.score), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = details.status ?: "Unknown status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                val seasonYear = listOfNotNull(
                    details.season?.replaceFirstChar { it.uppercase() },
                    details.year?.toString()
                ).joinToString(" ")
                val episodeText = if (details.episodes != null) "${details.episodes} episodes" else "Episodes unknown"
                Text(
                    text = listOfNotNull(seasonYear.ifBlank { null }, episodeText).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (details.studios.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Studio: ${details.studios.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (details.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        details.genres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = genre.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (!details.trailer?.embed_url.isNullOrBlank() || !details.trailer?.url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val url = details.trailer?.url ?: details.trailer?.embed_url
                            if (url != null) onWatchTrailer(url)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Trailer")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Synopsis", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = details.synopsis ?: "No synopsis available.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Your List", style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(characters.take(15), key = { it.character.mal_id }) { entry ->
                        CharacterCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(details: JikanAnimeResult) {
    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        AsyncImage(
            model = details.images.jpg.large_image_url ?: details.images.jpg.image_url,
            contentDescription = details.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
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
            shadowElevation = 8.dp
        ) {
            AsyncImage(
                model = details.images.jpg.image_url,
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
                    label = { Text(status.label) }
                )
            }
        }
        if (localEntry != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { onRemove(localEntry) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
        Text(text = "Your Rating", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            for (i in 1..10) {
                Icon(
                    imageVector = if (i <= entry.rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Rate $i",
                    tint = Color(0xFFFFC107),
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
        Text(text = "Progress: $progressText", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onMarkEpisodeWatched(entry) }) {
            Text("Mark Episode Watched")
        }
    }
}

@Composable
private fun CharacterCard(entry: JikanCharacterEntry) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = entry.character.images.jpg.image_url,
            contentDescription = entry.character.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(72.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.character.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
