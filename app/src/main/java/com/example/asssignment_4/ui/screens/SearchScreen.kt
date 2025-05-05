package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.asssignment_4.R
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(searchResults) { artist ->
            SearchResultCard(
                artist = artist,
                onClick = {
                    navController.navigate(
                        Screen.ArtistDetail.createRoute(artist.id)
                    )
                }
            )
        }
    }
}

@Composable
fun SearchResultCard(
    artist: Artist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
//    Log.d("SearchResultCard", "Artist: ${artist.name}, ImageUrl: ${artist.imageUrl}")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp

    ) {
        // we'll fix the height to something banner‑ish
        Box(modifier = Modifier.height(185.dp)) {
            // 1) full‑size background image
            if (artist.imageUrl == "/assets/shared/missing_image.png") {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.artsy_logo)
                        .crossfade(true)
                        .error(R.drawable.artsy_logo)
                        .fallback(R.drawable.artsy_logo)
                        .build(),
                    contentDescription = "${artist.name} artwork",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artist.imageUrl)
                        .crossfade(true)
                        .error(R.drawable.artsy_logo)
                        .fallback(R.drawable.artsy_logo)
                        .build(),
                    contentDescription = "${artist.name} artwork",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }


            // 2) bottom overlay with the name and arrow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    .padding(start = 8.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go to details",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            
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
