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
import com.example.asssignment_4.ui.theme.lightArtsyBlue
import com.example.asssignment_4.ui.theme.lightArtsyDarkBlue
import com.example.asssignment_4.util.AuthManagerEvent
import com.example.asssignment_4.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val authErrorMessage by authViewModel.authError.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, authViewModel) {
        authViewModel.authEvent.collect { event ->
            when (event) {
                is AuthManagerEvent.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                    onLoginSuccess()
                }
                is AuthManagerEvent.Failure -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is AuthManagerEvent.SessionExpired -> {
                    // For login screen, we're already on the login screen, so just show a message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Your previous session has expired. Please log in again.",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    fun validate(field: String? = null) {
        when (field) {
            "email", null -> {
                emailError = when {
                    email.isBlank() -> "Email cannot be empty"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                        .matches() -> "Invalid email format"

                    else -> null
                }
            }

            "password", null -> {
                passwordError = when {
                    password.isBlank() -> "Password cannot be empty"
                    else -> null
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Login") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {},
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
                value = email,
                onValueChange = {
                    email = it
                    if (emailTouched) validate("email")
                },
                label = { Text("Email") },
                isError = emailError != null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) {
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
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
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
                        if (it.isFocused) {
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
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    validate()
                    if (emailError == null && passwordError == null) {
                        authViewModel.loginUser(email, password)
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) Color.Gray else lightArtsyDarkBlue,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = artsyBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Login", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            
            // Error message container
            authErrorMessage?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have an account yet? ",
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.05).sp,
                )
                Text(
                    "Register",
                    color = lightArtsyDarkBlue,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    letterSpacing = (-0.05).sp,
                    modifier = Modifier.clickable { onRegister() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        navController = rememberNavController(),
        snackbarHostState = remember { SnackbarHostState() },
        onRegister = {}
    )
}
