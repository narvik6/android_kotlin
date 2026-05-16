package com.example.module6_t3.presentation.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: Int,
    viewModel: UsersViewModel,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val uiState by viewModel.detailState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserDetail(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали пользователя") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is UserDetailUiState.Loading -> CircularProgressIndicator()
                is UserDetailUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadUserDetail(userId) }) { Text("Повторить") }
                    }
                }
                is UserDetailUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = state.user.image,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = state.user.fullName, style = MaterialTheme.typography.headlineMedium)
                        Text(text = state.user.email, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "ID: ${state.user.id}", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.weight(1f)) // Отодвигает кнопку вниз

                        Button(
                            onClick = { viewModel.logout(onLogoutComplete = onLogoutClick) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Выйти")
                        }
                    }
                }
            }
        }
    }
}