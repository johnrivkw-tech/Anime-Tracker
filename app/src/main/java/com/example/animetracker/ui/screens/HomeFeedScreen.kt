package com.example.animetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.animetracker.ui.components.AnimeSectionRow
import com.example.animetracker.ui.components.FeaturedBanner
import com.example.animetracker.ui.model.toHomeCardItem
import com.example.animetracker.viewmodel.AnimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(viewModel: AnimeViewModel, onAnimeClick: (Int) -> Unit) {
    val trending by viewModel.trending.collectAsState()
    val popularSeason by viewModel.popularThisSeason.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()
    val recommended by viewModel.recommended.collectAsState()
    val continueTracking by viewModel.continueTracking.collectAsState()
    val localByMalId by viewModel.localByMalId.collectAsState()
    val isLoading by viewModel.isHomeFeedLoading.collectAsState()
    val error by viewModel.homeFeedError.collectAsState()

    val trendingItems = remember(trending, localByMalId) {
        trending.map { it.toHomeCardItem(localByMalId[it.mal_id]) }
    }
    val popularItems = remember(popularSeason, localByMalId) {
        popularSeason.map { it.toHomeCardItem(localByMalId[it.mal_id]) }
    }
    val topRatedItems = remember(topRated, localByMalId) {
        topRated.map { it.toHomeCardItem(localByMalId[it.mal_id]) }
    }
    val newReleaseItems = remember(newReleases, localByMalId) {
        newReleases.map { it.toHomeCardItem(localByMalId[it.mal_id]) }
    }
    val recommendedItems = remember(recommended, localByMalId) {
        recommended.map { it.toHomeCardItem(localByMalId[it.mal_id]) }
    }
    val continueItems = remember(continueTracking) {
        continueTracking.map { it.toHomeCardItem() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vizora") }) }
    ) { paddingValues ->
        if (error != null && trendingItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.loadHomeFeed() }) {
                    Text("Retry")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item {
                    FeaturedBanner(
                        item = trendingItems.firstOrNull(),
                        onClick = { trendingItems.firstOrNull()?.malId?.let(onAnimeClick) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                if (continueItems.isNotEmpty()) {
                    item {
                        AnimeSectionRow(
                            title = "Continue Tracking",
                            items = continueItems,
                            isLoading = false,
                            onItemClick = { it.malId?.let(onAnimeClick) }
                        )
                    }
                }
                item {
                    AnimeSectionRow(
                        title = "Trending Now",
                        items = trendingItems,
                        isLoading = isLoading && trendingItems.isEmpty(),
                        onItemClick = { it.malId?.let(onAnimeClick) }
                    )
                }
                item {
                    AnimeSectionRow(
                        title = "Popular This Season",
                        items = popularItems,
                        isLoading = isLoading && popularItems.isEmpty(),
                        onItemClick = { it.malId?.let(onAnimeClick) }
                    )
                }
                item {
                    AnimeSectionRow(
                        title = "Top Rated",
                        items = topRatedItems,
                        isLoading = isLoading && topRatedItems.isEmpty(),
                        onItemClick = { it.malId?.let(onAnimeClick) }
                    )
                }
                item {
                    AnimeSectionRow(
                        title = "New Releases",
                        items = newReleaseItems,
                        isLoading = isLoading && newReleaseItems.isEmpty(),
                        onItemClick = { it.malId?.let(onAnimeClick) }
                    )
                }
                item {
                    AnimeSectionRow(
                        title = "Recommended For You",
                        items = recommendedItems,
                        isLoading = isLoading && recommendedItems.isEmpty(),
                        onItemClick = { it.malId?.let(onAnimeClick) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
