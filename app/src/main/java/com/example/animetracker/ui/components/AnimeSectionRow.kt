package com.example.animetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.animetracker.ui.model.HomeCardItem
import com.example.animetracker.ui.theme.Blaze
import com.example.animetracker.ui.theme.Bone
import com.example.animetracker.ui.theme.Smoke

@Composable
fun AnimeSectionRow(
    title: String,
    items: List<HomeCardItem>,
    isLoading: Boolean,
    onItemClick: (HomeCardItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Bone,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            isLoading -> {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CircularProgressIndicator(color = Blaze, modifier = Modifier.size(24.dp))
                }
            }
            items.isEmpty() -> {
                Text(
                    text = "Nothing to show yet",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Smoke,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.key }) { item ->
                        AnimePosterCard(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}
