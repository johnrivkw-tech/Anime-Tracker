package com.example.animetracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import com.example.animetracker.ui.components.AnimePosterCard
import com.example.animetracker.ui.model.toHomeCardItem
import com.example.animetracker.ui.theme.CharcoalHigh
import com.example.animetracker.viewmodel.AnimeViewModel

/**
 * The Search tab: browses the whole AniList catalog by title (unlike My
 * List's "add anime" dialog, this is for looking anything up and viewing
 * details — adding to your list happens from the details screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: AnimeViewModel, onAnimeClick: (Int) -> Unit) {
    val query by viewModel.catalogQuery.collectAsState()
    val results by viewModel.catalogResults.collectAsState()
    val isLoading by viewModel.isCatalogSearching.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val localByAniListId by viewModel.localByAniListId.collectAsState()

    val items = remember(results, localByAniListId) {
        results.map { it.toHomeCardItem(localByAniListId[it.id]) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalHigh)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onCatalogQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search the anime catalog...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onCatalogQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    error != null -> {
                        Text(
                            text = error ?: "",
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    query.isBlank() -> {
                        Text(
                            text = "Search for any anime in the catalog",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items.isEmpty() -> {
                        Text(
                            text = "No results found",
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
