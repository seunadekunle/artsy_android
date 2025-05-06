package com.example.asssignment_4.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.util.Log
import com.example.asssignment_4.ui.navigation.AppNavGraph
import com.example.asssignment_4.ui.navigation.Screen
import com.example.asssignment_4.ui.screens.SearchResultCard
import com.example.asssignment_4.ui.theme.YourAppTheme
import com.example.asssignment_4.ui.theme.artsyBlue
import com.example.asssignment_4.viewmodel.AuthViewModel
import com.example.asssignment_4.viewmodel.HomeViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainScaffold(
    navController: NavHostController
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val isLoggedIn = currentUser != null

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isSearchScreen = currentRoute == Screen.Search.route
    val isLoginScreen = currentRoute == Screen.Login.route
    val isSignupScreen = currentRoute == Screen.Register.route
    val homeViewModel: HomeViewModel = hiltViewModel()
    val artistDetail by homeViewModel.artistDetail.collectAsState() // Collect artist details for title

    var searchTerm by remember { mutableStateOf("") }

    // Observe search results
    val searchResults by homeViewModel.searchResults.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val error by homeViewModel.error.collectAsState()

    // Trigger search when text changes
    LaunchedEffect(searchTerm) {
        homeViewModel.setSearchTerm(searchTerm)
    }

    // Animation for screen transitions
    val enterTransition = remember {
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    }
    val exitTransition = remember {
        slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    }

    // --- DEBUG LOG --- 
    Log.d("MainScaffoldRouteCheck", "Current Route = '$currentRoute'")
    // -----------------

    // Determine which TopAppBar composable to show based on the current route
    val topBarToShow: (@Composable () -> Unit)? = when {
        currentRoute?.startsWith(Screen.ArtistDetail.route.substringBefore('{')) == true -> {
            null // Explicitly show NO TopAppBar from MainScaffold on ArtistDetailScreen
        }
        currentRoute?.startsWith(Screen.Login.route.substringBefore('{')) == true -> {
            null
        }
        isSearchScreen -> {
            @Composable { // Lambda for Search TopAppBar
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
        }
        currentRoute == Screen.Home.route -> {
             @Composable { // Lambda for Home TopAppBar
                TopAppBar(
                    title = { Text("Artist Search", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)) },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.navigate(Screen.Search.route) }) { 
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimary) 
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = if (isLoggedIn) Icons.Filled.Person else Icons.Outlined.Person, 
                                    contentDescription = "Profile", 
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (isLoggedIn) {
                                    DropdownMenuItem(text = { Text("Favorites") }, onClick = { navController.navigate(Screen.Favourites.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Logout") }, onClick = { authViewModel.logoutUser(); showMenu = false })
                                } else {
                                    DropdownMenuItem(text = { Text("Login") }, onClick = { navController.navigate(Screen.Login.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Register") }, onClick = { navController.navigate(Screen.Register.route); showMenu = false })
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
        // Add other specific routes here if they need a TopAppBar from MainScaffold
        else -> {
             @Composable { // Lambda for Default TopAppBar (Login, Register, etc.)
                TopAppBar(
                    title = { Text("Artsy App") }, // Default Title
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(imageVector = if (isLoggedIn) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (isLoggedIn) {
                                    DropdownMenuItem(text = { Text("Favorites") }, onClick = { navController.navigate(Screen.Favourites.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Logout") }, onClick = { authViewModel.logoutUser(); showMenu = false })
                                } else {
                                    DropdownMenuItem(text = { Text("Login") }, onClick = { navController.navigate(Screen.Login.route); showMenu = false })
                                    DropdownMenuItem(text = { Text("Register") }, onClick = { navController.navigate(Screen.Register.route); showMenu = false })
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = artsyBlue)
                )
            }
        }
    }

    Scaffold(
        topBar = { topBarToShow?.invoke() },
        containerColor = MaterialTheme.colorScheme.background,
        content = { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                if (isSearchScreen) {
                    Column {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            }
                        }
                        else if (searchResults.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
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
                        } else if (searchTerm.isNotBlank()) {
                            Text(
                                text = "",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    AppNavGraph(navController = navController, paddingValues = innerPadding)
                }
            }
        }
    )
}

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldPreview() {
    YourAppTheme(darkTheme = false) {
        MainScaffold(
            navController = rememberNavController()
        )
    }
}
