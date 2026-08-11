package com.gps.warehouse.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gps.warehouse.ui.components.ErrorStateView
import com.gps.warehouse.ui.MainViewModel
import com.gps.warehouse.R
import com.gps.warehouse.ui.components.AppIconDisplay

// Реальный экран
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: MainViewModel
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is MainViewModel.UiState.LoggedIn) {
            onLoginSuccess()
        }
    }

    LoginScreenContent(
        username = username,
        onUsernameChange = { username = it },
        password = password,
        onPasswordChange = { password = it },
        uiState = uiState,
        onLoginClick = {
            if (username.isNotBlank() && password.isNotBlank()) {
                viewModel.login(username, password)
            }
        }
    )
}

// Чистый UI компонент. Принимает только данные и коллбэки
@Composable
fun LoginScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    uiState: MainViewModel.UiState,
    onLoginClick: () -> Unit
) {
    // Состояние для переключения видимости пароля
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Авторизация GPS RS", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        AppIconDisplay()

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Логин") },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is MainViewModel.UiState.Loading
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            // Если пароль виден - показываем текст, иначе скрываем
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // Иконка глаза справа
            trailingIcon = {
                val image = if (isPasswordVisible)
                    Icons.Filled.Visibility
                else
                    Icons.Filled.VisibilityOff

                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = if (isPasswordVisible) "Скрыть пароль" else "Показать пароль")
                }
            },
            enabled = uiState !is MainViewModel.UiState.Loading
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLoginClick,
            enabled = uiState !is MainViewModel.UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is MainViewModel.UiState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Войти")
            }
        }

        if (uiState is MainViewModel.UiState.Error) {
            ErrorStateView(
                message = uiState.message
            )
        }
    }
}

@Preview(showBackground = true, name = "Login - Default")
@Composable
fun LoginScreenPreviewDefault() {
    LoginScreenContent(
        username = "",
        onUsernameChange = {},
        password = "",
        onPasswordChange = {},
        uiState = MainViewModel.UiState.Idle,
        onLoginClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewError() {
    LoginScreenContent(
        username = "",
        onUsernameChange = {},
        password = "",
        onPasswordChange = {},
        uiState = MainViewModel.UiState.Error("Error"),
        onLoginClick = {}
    )
}