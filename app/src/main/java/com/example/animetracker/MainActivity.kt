package com.example.animetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import com.example.animetracker.ui.screens.AnimeDetailsScreen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.animetracker.ui.navigation.BottomNavBar
import com.example.animetracker.ui.navigation.Destination
import com.example.animetracker.ui.screens.HomeFeedScreen
import com.example.animetracker.ui.screens.HomeScreen
import com.example.animetracker.ui.screens.PlaceholderScreen
import com.example.animetracker.ui.theme.AnimeTrackerTheme
import com.example.animetracker.viewmodel.AnimeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VizoraApp()
                }
            }
        }
    }
}

@Composable
private fun VizoraApp() {
    val navController = rememberNavController()
    val viewModel: AnimeViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Destination.HOME.route) {
                HomeFeedScreen(
                    viewModel = viewModel,
                    onAnimeClick = { malId -> navController.navigate("details/$malId") }
                )
            }
            composable(Destination.MY_LIST.route) {
                HomeScreen(viewModel = viewModel)
            }
            composable(Destination.DISCOVER.route) {
                PlaceholderScreen(
                    title = "Discover",
                    subtitle = "Browse anime by genre, season, and studio",
                    icon = Icons.Filled.Explore
                )
            }
            composable(Destination.SEARCH.route) {
                PlaceholderScreen(
                    title = "Search",
                    subtitle = "Find any anime across the whole catalog",
                    icon = Icons.Filled.Search
                )
            }
            composable(Destination.PROFILE.route) {
                PlaceholderScreen(
                    title = "Profile",
                    subtitle = "Your stats and settings will live here",
                    icon = Icons.Filled.Person
                )
            }
            composable(
                route = "details/{malId}",
                arguments = listOf(navArgument("malId") { type = NavType.IntType })
            ) { backStackEntry ->
                val malId = backStackEntry.arguments?.getInt("malId") ?: 0
                AnimeDetailsScreen(
                    viewModel = viewModel,
                    malId = malId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
