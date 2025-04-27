package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import com.example.asssignment_4.ui.navigation.Screen

@Composable
fun SplashScreen(navController: NavHostController) {
    LaunchedEffect(true) {
        delay(1200)
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Artist Search",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
