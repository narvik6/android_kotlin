package com.bibo.android.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel

private object AuthRoutes {
    const val Login = "login"
    const val Register = "register"
}

@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AuthRoutes.Login,
    ) {
        composable(AuthRoutes.Login) {
            LoginScreen(
                onRegisterClick = { navController.navigate(AuthRoutes.Register) },
            )
        }
        composable(AuthRoutes.Register) {
            RegisterScreen(
                onLoginClick = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    AuthForm(
        title = "Вход",
        primaryActionText = "Войти",
        secondaryActionText = "Создать аккаунт",
        uiState = uiState,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onPrimaryAction = viewModel::login,
        onSecondaryAction = onRegisterClick,
    )
}

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    AuthForm(
        title = "Регистрация",
        primaryActionText = "Зарегистрироваться",
        secondaryActionText = "Уже есть аккаунт",
        uiState = uiState,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onPrimaryAction = viewModel::register,
        onSecondaryAction = onLoginClick,
    )
}

@Composable
private fun AuthForm(
    title: String,
    primaryActionText: String,
    secondaryActionText: String,
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            enabled = !uiState.loading,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Пароль") },
            singleLine = true,
            enabled = !uiState.loading,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.loading,
        ) {
            if (uiState.loading) {
                CircularProgressIndicator()
            } else {
                Text(primaryActionText)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSecondaryAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.loading,
        ) {
            Text(secondaryActionText)
        }
    }
}
