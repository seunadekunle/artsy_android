package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun RegisterScreen(
    navController: NavHostController,
    onLogin: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var valid = true
        nameError = null
        emailError = null
        passwordError = null
        if (name.isBlank()) {
            nameError = "Full name cannot be empty"
            valid = false
        }
        if (email.isBlank()) {
            emailError = "Email cannot be empty"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email format"
            valid = false
        }
        if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            valid = false
        }
        return valid
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Register",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                label = { Text("Enter full name") },
                isError = nameError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (nameError != null) {
                Text(
                    text = nameError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Enter email") },
                isError = emailError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (emailError != null) {
                Text(
                    text = emailError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = { Text("Password") },
                isError = passwordError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (validate()) {
                        isLoading = true
                        // Simulate register delay
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1200)
                            isLoading = false
                            if (email == "elon@gmail.com") {
                                errorMessage = "Email already exists"
                                showSnackbar = true
                            } else {
                                showSnackbar = true
                                errorMessage = null
                                onRegisterSuccess()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223C6A))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Register", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Text("Already have an account? ")
                Text(
                    "Login",
                    color = Color(0xFF223C6A),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLogin() }
                )
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
        if (showSnackbar) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                action = {
                    TextButton(onClick = { showSnackbar = false }) { Text("Dismiss") }
                }
            ) {
                Text(
                    if (errorMessage == null) "Registered successfully" else errorMessage!!,
                    color = if (errorMessage == null) Color.Green else Color.Red
                )
            }
        }
    }
}
