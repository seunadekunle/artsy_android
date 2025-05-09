package com.example.asssignment_4.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.asssignment_4.ui.screens.*
import com.example.asssignment_4.viewmodel.LoginViewModel

// Define the navigation routes
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: String): String {
            return "artist_detail/$artistId"
        }
    }
    object Login : Screen("login")
    object Register : Screen("register")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    snackbarHostState: SnackbarHostState
) {
    val slideSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
    val fadeSpec = tween<Float>(durationMillis = 300)
    val detailFadeSpec = tween<Float>(durationMillis = 400, easing = LinearOutSlowInEasing)

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // HOME SCREEN
        composable(
            route = Screen.Home.route,
            enterTransition = {
                when (initialState.destination.route) {
                    // Back from Search: Home enters from left
                    Screen.Search.route -> slideInHorizontally(animationSpec = slideSpec) { -it }
                    Screen.ArtistDetail.route -> fadeIn(animationSpec = detailFadeSpec)
                    else -> fadeIn(animationSpec = fadeSpec)
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    // Forward to Search: Home exits to left
                    Screen.Search.route -> slideOutHorizontally(animationSpec = slideSpec) { -it }
                    Screen.Login.route -> fadeOut(animationSpec = fadeSpec)
                    Screen.ArtistDetail.route -> fadeOut(animationSpec = detailFadeSpec)
                    else -> fadeOut(animationSpec = fadeSpec)
                }
            },
            popEnterTransition = {
                when (initialState.destination.route) {
                    Screen.Search.route -> slideInHorizontally(animationSpec = slideSpec) { -it }
                    else -> fadeIn(animationSpec = fadeSpec)
                }
            }
        ) {
            HomeScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }

        // SEARCH SCREEN
        composable(
            route = Screen.Search.route,
            enterTransition = {
                when (initialState.destination.route) {
                    // Forward from Home: Search enters from right
                    Screen.Home.route -> slideInHorizontally(animationSpec = slideSpec) { it }
                    else -> fadeIn(animationSpec = fadeSpec)
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    // Back to Home: Search exits to right
                    Screen.Home.route -> slideOutHorizontally(animationSpec = slideSpec) { it }
                    Screen.ArtistDetail.route -> fadeOut(animationSpec = fadeSpec)
                    else -> fadeOut(animationSpec = fadeSpec)
                }
            },
            popEnterTransition = {
                when (initialState.destination.route) {
                    Screen.ArtistDetail.route -> fadeIn(animationSpec = fadeSpec)
                    else -> slideInHorizontally(animationSpec = slideSpec) { it }
                }
            },
            popExitTransition = {
                when (targetState.destination.route) {
                    Screen.ArtistDetail.route -> fadeOut(animationSpec = fadeSpec)
                    else -> slideOutHorizontally(animationSpec = slideSpec) { it }
                }
            }
        ) {
            SearchScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }

        // ARTIST DETAIL SCREEN
        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = detailFadeSpec) },
            exitTransition = { fadeOut(animationSpec = detailFadeSpec) },
            popEnterTransition = { fadeIn(animationSpec = detailFadeSpec) },
            popExitTransition = { fadeOut(animationSpec = detailFadeSpec) }
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(
                navController = navController,
                artistId = artistId,
                paddingValues = paddingValues,
                snackbarHostState = snackbarHostState
            )
        }

        // LOGIN SCREEN
        composable(
            route = Screen.Login.route,
            enterTransition = { fadeIn(animationSpec = fadeSpec) },
            exitTransition = { fadeOut(animationSpec = fadeSpec) },
            popEnterTransition = { fadeIn(animationSpec = fadeSpec) },
            popExitTransition = { fadeOut(animationSpec = fadeSpec) }
        ) {
            LoginScreen(
                navController = navController,
                snackbarHostState = snackbarHostState,
                onRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // REGISTER SCREEN
        composable(
            route = Screen.Register.route,
            enterTransition = { fadeIn(animationSpec = fadeSpec) },
            exitTransition = { fadeOut(animationSpec = fadeSpec) },
            popEnterTransition = { fadeIn(animationSpec = fadeSpec) },
            popExitTransition = { fadeOut(animationSpec = fadeSpec) }
        ) {
            RegisterScreen(
                navController = navController,
                snackbarHostState = snackbarHostState,
                onLogin = { navController.navigate(Screen.Login.route) },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

