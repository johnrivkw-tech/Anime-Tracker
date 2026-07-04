package com.example.animetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** The five top-level destinations shown in the bottom navigation bar. */
enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    MY_LIST("my_list", "My List", Icons.Filled.List),
    DISCOVER("discover", "Discover", Icons.Filled.Explore),
    SEARCH("search", "Search", Icons.Filled.Search),
    PROFILE("profile", "Profile", Icons.Filled.Person)
}
