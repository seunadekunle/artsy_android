package com.example.asssignment_4.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.ui.components.TimeAgoText

@Composable
fun ArtistRow(
    artist: Artist,
    onClick: (Artist) -> Unit,
    onFavoriteToggle: ((String, Boolean) -> Unit)? = null,
    timestamp: String? = null // Add timestamp parameter with default value null
) {

    val artistInfo = formatFavoriteArtistInfo(artist = artist)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(artist) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (artistInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artistInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        Row(verticalAlignment = Alignment.CenterVertically) {
            // Use TimeAgoText if timestamp is available, otherwise show a placeholder
            if (timestamp != null) {
                TimeAgoText(dateTime = timestamp, delayMillis = 1000)
            } else {
                Text(
                    text = "Recently",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatFavoriteArtistInfo(artist: Artist?): String {
    val details = mutableListOf<String?>()
    val nationality = artist?.nationality
    val birth = artist?.birthday

    details.add(nationality ?: "")
    details.add(birth ?: "")


    return details.filterNotNull().joinToString(", ")
}


@Preview(showBackground = true)
@Composable
fun ArtistRowPreview() {
    val artist = Artist(
        id = "favorite.artistId",
        name = "favorite.artistName",
        nationality = "Spanish",
        birthday = "1881",
        deathday = null,
        imageUrl = "favorite.artistImage",
        biography = null,
        isFavorite = true
    )
    ArtistRow(
        artist = artist,
        onClick = {},
        onFavoriteToggle = { _, _ -> },
        timestamp = "2025-05-07T12:00:00Z" // Add sample timestamp for preview
    )
}