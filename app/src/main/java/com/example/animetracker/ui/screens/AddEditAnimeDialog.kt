package com.example.animetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.animetracker.data.Anime
import com.example.animetracker.data.AnimeStatus
import com.example.animetracker.ui.theme.Blaze
import com.example.animetracker.ui.theme.Bone
import com.example.animetracker.ui.theme.CharcoalHigh
import com.example.animetracker.ui.theme.Pulse

/**
 * Shared dialog for both adding a new anime and editing an existing one.
 * Pass [anime] = null to add; pass an existing entry to edit it.
 */
@Composable
fun AddEditAnimeDialog(
    anime: Anime?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        episodesWatched: Int,
        totalEpisodes: Int,
        status: AnimeStatus,
        rating: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf(anime?.name ?: "") }
    var episodesWatched by remember { mutableStateOf(anime?.episodesWatched?.toString() ?: "0") }
    var totalEpisodes by remember {
        mutableStateOf(anime?.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "")
    }
    var status by remember { mutableStateOf(anime?.status ?: AnimeStatus.PLAN_TO_WATCH) }
    var rating by remember { mutableStateOf(anime?.rating ?: 0) }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalHigh)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (anime == null) "Add anime" else "Edit anime",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Anime name") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text("Name can't be empty")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = episodesWatched,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) episodesWatched = input
                        },
                        label = { Text("Watched") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = totalEpisodes,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) totalEpisodes = input
                        },
                        label = { Text("Total (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Status", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimeStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blaze,
                                selectedLabelColor = Bone
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (rating > 0) "Rating: $rating / 10" else "Rating: not rated",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (star in 1..10) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Rate $star out of 10",
                            tint = if (star <= rating) {
                                Pulse
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    // Tapping the current top star clears one point,
                                    // so users can back a rating down to "not rated".
                                    rating = if (rating == star) star - 1 else star
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isBlank()) {
                            nameError = true
                        } else {
                            onConfirm(
                                name.trim(),
                                episodesWatched.toIntOrNull() ?: 0,
                                totalEpisodes.toIntOrNull() ?: 0,
                                status,
                                rating
                            )
                        }
                    }) {
                        Text(if (anime == null) "Add" else "Save")
                    }
                }
            }
        }
    }
}
