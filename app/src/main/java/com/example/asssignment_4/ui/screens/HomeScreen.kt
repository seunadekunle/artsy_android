package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.asssignment_4.ui.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.asssignment_4.model.Artist
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

    var showMenu by remember { mutableStateOf(false) }
    val isLoggedIn = currentUser != null
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist Search") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Avatar")
                        }
                        if (isLoggedIn) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("Log out") }, onClick = {
                                    authViewModel.logoutUser()
                                    showMenu = false
                                })
                                DropdownMenuItem(text = { Text("Delete account") }, onClick = { showMenu = false })
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Date
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search input field
            SearchField(
                value = searchTerm,
                onValueChange = { homeViewModel.setSearchTerm(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LoadingIndicator()
            } else if (error != null) {
                ErrorMessage(message = error ?: "Unknown error")
            }

            authError?.let {
                Text(
                    text = "Auth Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (searchTerm.length < 3 && !isLoading) {
                if (isLoggedIn) {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Card(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                items(favourites.size) { index ->
                                    val favArtist = favourites[index]
                                    Text(favArtist.name ?: "Unknown Favourite", modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                } else {
                    Text("Login to see your favourites", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            } else if (!isLoading) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(searchResults.size) { index ->
                        val artist = searchResults[index]
                        ArtistCard(
                            artist = artist,
                            artistInfo = formatArtistInfo(artist),
                            isFav = favouriteIds.contains(artist.id),
                            onFavToggle = { homeViewModel.toggleFavourite(artist) },
                            onClick = {
                                focusManager.clearFocus()
                                navController.navigate(Screen.ArtistDetail.createRoute(artist.id ?: "unknown"))
                            }
                        )
                    }
                }
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
    artist: Artist = Artist(id = "123", name = "Pablo Picasso", nationality = "Spanish", birthday = "1881", deathday = "1973", imageUrl = "", biography = "Pablo Picasso was a Spanish painter, sculptor, printmaker, ceramicist and theatre designer."),
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
                Text(artist.name ?: "Unknown Artist", style = MaterialTheme.typography.bodyLarge)
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist Search") },
                actions = {
                    Box {
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Avatar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Date
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
            )
            
            // Search field
            SearchField(value = "", onValueChange = {})
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sample artist cards
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(3) { index ->
                    val artist = Artist(
                        id = "artist$index",
                        name = "Sample Artist $index",
                        nationality = "Country",
                        birthday = "1980",
                        deathday = null,
                        imageUrl = "",
                        biography = "Sample biography"
                    )
                    ArtistCard(
                        artist = artist,
                        artistInfo = formatArtistInfo(artist),
                        isFav = index % 2 == 0,
                        onFavToggle = { },
                        onClick = { }
                    )
                }
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
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorMessage(message: String = "An error occurred") {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        textAlign = TextAlign.Center
    )
}
