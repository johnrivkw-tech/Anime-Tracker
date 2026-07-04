package com.example.animetracker.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.ui.components.AnimeCard
import com.example.animetracker.viewmodel.AnimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AnimeViewModel) {
    val animeList by viewModel.filteredAnime.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    val onlineResults by viewModel.searchResults.collectAsState()
    val isSearchingApi by viewModel.isSearchingApi.collectAsState()
    val searchApiError by viewModel.searchApiError.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var animeBeingEdited by remember { mutableStateOf<Anime?>(null) }

    var showSearchDialog by remember { mutableStateOf(false) }
    var onlineQuery by remember { mutableStateOf("") }

    fun closeSearchDialog() {
        showSearchDialog = false
        onlineQuery = ""
        viewModel.clearSearchResults()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AnimeTracker") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSearchDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add anime")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search your watchlist") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.onStatusFilterChange(null) },
                    label = { Text("All") }
                )
                AnimeStatus.entries.forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(status.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val isFiltering = searchQuery.isNotEmpty() || statusFilter != null

            if (animeList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFiltering) "No matches" else "Your watchlist is empty",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFiltering) {
                                "Try a different search or filter"
                            } else {
                                "Tap + to search for your first anime"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(animeList, key = { it.id }) { anime ->
                        AnimeCard(
                            anime = anime,
                            onIncrement = { viewModel.incrementEpisode(anime) },
                            onEdit = {
                                animeBeingEdited = anime
                                showDialog = true
                            },
                            onDelete = { viewModel.deleteAnime(anime) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        val editing = animeBeingEdited
        AddEditAnimeDialog(
            anime = editing,
            onDismiss = { showDialog = false },
            onConfirm = { name, watched, total, status, rating ->
                if (editing != null) {
                    viewModel.updateAnime(
                        editing.copy(
                            name = name,
                            episodesWatched = watched,
                            totalEpisodes = total,
                            status = status,
                            rating = rating
                        )
                    )
                } else {
                    viewModel.addAnime(name, watched, total, status, rating)
                }
                showDialog = false
            }
        )
    }

    if (showSearchDialog) {
        SearchAnimeDialog(
            query = onlineQuery,
            onQueryChange = {
                onlineQuery = it
                viewModel.searchOnline(it)
            },
            results = onlineResults,
            isLoading = isSearchingApi,
            error = searchApiError,
            onDismiss = { closeSearchDialog() },
            onSelect = { result ->
                viewModel.addAnimeFromSearchResult(result)
                closeSearchDialog()
            },
            onAddManually = {
                closeSearchDialog()
                animeBeingEdited = null
                showDialog = true
            }
        )
    }
}
