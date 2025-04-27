package com.example.asssignment_4.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.asssignment_4.ui.navigation.AppNavGraph
import com.example.asssignment_4.ui.navigation.Screen

@Composable
fun MainScaffold(navController: NavHostController, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist Search") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        },
        content = { padding ->
            AppNavGraph(navController = navController)
        }
    )
}
