package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.asssignment_4.ui.components.SearchResultCard
import com.example.asssignment_4.ui.navigation.Screen
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val isLoggedIn = currentUser != null

    val searchResults by homeViewModel.searchResults.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val error by homeViewModel.error.collectAsState()
    val favourites by homeViewModel.favourites.collectAsState()
    val favouriteIds by homeViewModel.favouriteIds.collectAsState()
    
    var isSearchVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    var searchTerm by rememberSaveable { mutableStateOf("") }


    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            focusRequester.requestFocus()
        }
    }

    // Trigger search when text changes
    LaunchedEffect(searchTerm) {
        homeViewModel.setSearchTerm(searchTerm)
    }

    Scaffold(
        topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = LocalContentColor.current.copy(alpha = 0.75f))
                                Spacer(modifier = Modifier.width(6.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = searchTerm,
                                        onValueChange = {
                                            searchTerm = it
                                            if (it.isNotEmpty()) { homeViewModel.setSearchTerm(it) }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.W500, textDecoration = TextDecoration.Underline, color = LocalContentColor.current),
                                        cursorBrush = SolidColor(LocalContentColor.current)
                                    )
                                    if (searchTerm.isEmpty()) {
                                        Text("Search artists…", fontSize = 22.sp, color = LocalContentColor.current.copy(alpha = 0.6f), modifier = Modifier.padding(start = 6.dp))
                                    }
                                }

                                IconButton(onClick = { homeViewModel.setSearchTerm(""); searchTerm = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = LocalContentColor.current.copy(alpha = 1f))
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )

        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(searchResults) { artist ->
                val isArtistFavorite = favouriteIds.contains(artist.id)
                SearchResultCard(
                    artist = artist,
                    isLoggedIn = isLoggedIn,
                    isFavorite = isArtistFavorite,
                    onFavoriteClick = {
                        if (isLoggedIn) {
                            if (isArtistFavorite) {
                                homeViewModel.removeFavorite(artist.id)
                            } else {
                                homeViewModel.addFavorite(artist.id)
                            }
                        }
                    },
                    onClick = {
                        navController.navigate(
                            Screen.ArtistDetail.createRoute(artist.id)
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    SearchScreen(navController = rememberNavController())
}
