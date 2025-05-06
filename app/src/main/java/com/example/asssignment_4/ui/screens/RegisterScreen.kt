package com.example.asssignment_4.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.asssignment_4.ui.theme.artsyBlue
import com.example.asssignment_4.ui.theme.artsyDarkBlue
import com.example.asssignment_4.ui.theme.lightArtsyBlue
import com.example.asssignment_4.ui.theme.lightArtsyDarkBlue
import com.example.asssignment_4.viewmodel.AuthEvent
import com.example.asssignment_4.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogin: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var nameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val authErrorMessage by authViewModel.authError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, authViewModel) {
        authViewModel.authEvent.collect { event ->
            when (event) {
                is com.example.asssignment_4.viewmodel.AuthEvent.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                    onRegisterSuccess()
                }

                is com.example.asssignment_4.viewmodel.AuthEvent.Failure -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    fun validate(field: String? = null) {
        when (field) {
            "name", null -> {
                nameError = if (name.isBlank()) "Full name cannot be empty" else null
            }

            "email", null -> {
                emailError = when {
                    email.isBlank() -> "Email cannot be empty"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                        .matches() -> "Invalid email format"

                    else -> null
                }
            }

            "password", null -> {
                passwordError = if (password.isBlank()) "Password cannot be empty" else null
            }
        }

    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Register") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameTouched) validate("name")
                },
                label = { Text("Enter full name") },
                isError = nameError != null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused && !nameTouched) {
                            nameTouched = true
                            validate("name")
                        }
                    }
            )
            if (nameError != null) {
                Text(
                    text = nameError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailTouched) validate("email")
                },
                label = { Text("Enter email") },
                isError = emailError != null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused && !emailTouched) {
                            emailTouched = true
                            validate("email")
                        }
                    }
            )
            if (emailError != null) {
                Text(
                    text = emailError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordTouched) validate("password")
                },
                label = { Text("Password") },
                isError = passwordError != null,
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused && !passwordTouched) {
                            passwordTouched = true
                            validate("password")
                        }
                    }
            )
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    nameTouched = true
                    emailTouched = true
                    passwordTouched = true
                    validate()
                    if (nameError == null && emailError == null && passwordError == null) {
                        authViewModel.registerUser(name, email, password)
                    }

                },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(37.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) Color.Gray else lightArtsyDarkBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Register", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))

            if (authErrorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = authErrorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have an account yet? ",
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Login",
                    color = lightArtsyDarkBlue,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        navController = rememberNavController(),
        onLogin = {}
    )
}
