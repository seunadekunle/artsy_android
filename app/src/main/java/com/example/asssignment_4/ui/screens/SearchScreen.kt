package com.example.asssignment_4.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.asssignment_4.model.Artist
import com.example.asssignment_4.ui.navigation.Screen
import com.example.asssignment_4.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val searchTerm by homeViewModel.searchTerm.collectAsState()
    val searchResults by homeViewModel.searchResults.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val error by homeViewModel.error.collectAsState()
    val favourites by homeViewModel.favourites.collectAsState()
    val favouriteIds by homeViewModel.favouriteIds.collectAsState()
    
    var isSearchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun SearchResultCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artist image
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = "${artist.name}'s artwork",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Artist info
        Column {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium
            )
            artist.nationality?.let { nationality ->
                Text(
                    text = formatArtistInfo(nationality, artist.birthday),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatArtistInfo(nationality: String, birthday: String?): String {
    val info = mutableListOf<String>()
    info.add(nationality)
    birthday?.let { info.add("Born $it") }
    return info.joinToString(" • ")
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(navController = rememberNavController())
}
