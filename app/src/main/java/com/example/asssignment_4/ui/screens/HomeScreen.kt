package com.example.asssignment_4.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.ui.navigation.Screen
import com.example.asssignment_4.ui.theme.artsyBlue
import com.example.asssignment_4.ui.theme.artsyDarkBlue
import com.example.asssignment_4.ui.theme.artsyLightBlue
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val searchTerm by homeViewModel.searchTerm.collectAsState()
    val searchResults by homeViewModel.searchResults.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val error by homeViewModel.error.collectAsState()
    val favourites by homeViewModel.favourites.collectAsState()
    val favouriteIds by homeViewModel.favouriteIds.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val authError by authViewModel.authError.collectAsState()

    val isLoggedIn = currentUser != null
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Date
        Text(
            text = "31 March 2025",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F6))
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // Login button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = artsyDarkBlue
                    )
                ) {
                    Text(
                        "Log in to see favorites",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            // Powered by Artsy attribution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Powered by Artsy",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://artsy.org"))
                            navController.context.startActivity(intent)
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

        }
    }
}

fun formatArtistInfo(artist: Artist): String {
    val details = mutableListOf<String?>()
    details.add(artist.nationality)
    val birth = artist.birthday
    val death = artist.deathday
    if (birth != null || death != null) {
        details.add("(${birth ?: "?"} - ${death ?: "?"})")
    }
    return details.filterNotNull().filter { it.isNotEmpty() }.joinToString(", ")
}

@Preview(showBackground = true)
@Composable
fun ArtistCard(
    artist: Artist = Artist(
        id = "123",
        name = "Pablo Picasso",
        nationality = "Spanish",
        birthday = "1881",
        deathday = "1973",
        imageUrl = "",
        biography = "Pablo Picasso was a Spanish painter, sculptor, printmaker, ceramicist and theatre designer."
    ),
    artistInfo: String = "Spanish, (1881 - 1973)",
    isFav: Boolean = false,
    onFavToggle: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(artist.name, style = MaterialTheme.typography.bodyLarge)
                Text(artistInfo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onFavToggle() }) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) Color(0xFFFFC107) else Color.Gray
                )
            }
        }
    }
}

// Create a separate preview version of HomeScreen that doesn't depend on ViewModels
@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenPreview() {
    // Create a simplified version of the HomeScreen UI for preview
    val navController = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Date
        Text(
            text = "31 March 2025",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F6))
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // Login button
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = artsyDarkBlue
                    )
                ) {
                    Text(
                        "Log in to see favorites",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            // Powered by Artsy attribution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Powered by Artsy",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://artsy.org"))
                            navController.context.startActivity(intent)
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

        }

    }


}


@Preview(showBackground = true)
@Composable
fun SearchField(
    value: String = "",
    onValueChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search for artists...") },
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorMessage(message: String = "An error occurred") {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}
