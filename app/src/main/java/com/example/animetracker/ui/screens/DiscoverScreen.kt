package com.example.animetracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animetracker.data.network.ANILIST_GENRES
import com.example.animetracker.ui.components.AnimePosterCard
import com.example.animetracker.ui.model.toHomeCardItem
import com.example.animetracker.viewmodel.AnimeViewModel
import java.time.LocalDate

private val SEASONS = listOf("WINTER", "SPRING", "SUMMER", "FALL")

private fun seasonLabel(value: String?): String = when (value) {
    "WINTER" -> "Winter"
    "SPRING" -> "Spring"
    "SUMMER" -> "Summer"
    "FALL" -> "Fall"
    else -> "Any Season"
}

/** Browse the catalog by genre, season, and year — separate from Home's curated rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(viewModel: AnimeViewModel, onAnimeClick: (Int) -> Unit) {
    val genre by viewModel.discoverGenre.collectAsState()
    val season by viewModel.discoverSeason.collectAsState()
    val year by viewModel.discoverYear.collectAsState()
    val results by viewModel.discoverResults.collectAsState()
    val isLoading by viewModel.isDiscoverLoading.collectAsState()
    val error by viewModel.discoverError.collectAsState()
    val localByAniListId by viewModel.localByAniListId.collectAsState()

    val items = remember(results, localByAniListId) {
        results.map { it.toHomeCardItem(localByAniListId[it.id]) }
    }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    val currentYear = remember { LocalDate.now().year }
    val yearOptions = remember(currentYear) { (currentYear downTo currentYear - 14).toList() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Discover") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Season + year filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = season == null,
                        onClick = { viewModel.setDiscoverSeason(null) },
                        label = { Text("Any Season") }
                    )
                    SEASONS.forEach { s ->
                        FilterChip(
                            selected = season == s,
                            onClick = { viewModel.setDiscoverSeason(if (season == s) null else s) },
                            label = { Text(seasonLabel(s)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = yearMenuExpanded,
                    onExpandedChange = { yearMenuExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = year?.toString() ?: "Any Year",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = yearMenuExpanded,
                        onDismissRequest = { yearMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Any Year") },
                            onClick = {
                                viewModel.setDiscoverYear(null)
                                yearMenuExpanded = false
                            }
                        )
                        yearOptions.forEach { y ->
                            DropdownMenuItem(
                                text = { Text(y.toString()) },
                                onClick = {
                                    viewModel.setDiscoverYear(y)
                                    yearMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Genre filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = genre == null,
                    onClick = { viewModel.setDiscoverGenre(null) },
                    label = { Text("All Genres") }
                )
                ANILIST_GENRES.forEach { g ->
                    FilterChip(
                        selected = genre == g,
                        onClick = { viewModel.setDiscoverGenre(if (genre == g) null else g) },
                        label = { Text(g) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && items.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    error != null && items.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { viewModel.loadDiscover() }, modifier = Modifier.padding(top = 12.dp)) {
                                Text("Retry")
                            }
                        }
                    }
                    items.isEmpty() -> {
                        Text(
                            text = "No anime match these filters",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(128.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(items, key = { it.key }) { item ->
                                AnimePosterCard(
                                    item = item,
                                    onClick = { item.aniListId?.let(onAnimeClick) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
