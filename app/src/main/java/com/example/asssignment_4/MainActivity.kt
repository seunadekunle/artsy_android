package com.example.asssignment_4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.asssignment_4.ui.MainScaffold
import com.example.asssignment_4.ui.theme.YourAppTheme // Replace with your theme package


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            YourAppTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScaffold(
                        navController = navController,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}