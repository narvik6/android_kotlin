package com.example.module6_t3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.module6_t3.presentation.Screen
import com.example.module6_t3.presentation.login.LoginScreen
import com.example.module6_t3.presentation.login.LoginViewModel
import com.example.module6_t3.presentation.users.UserDetailScreen
import com.example.module6_t3.presentation.users.UsersListScreen
import com.example.module6_t3.presentation.users.UsersViewModel
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Быстрая проверка токена до рендера UI, чтобы решить, с какого экрана стартовать
        val app = application as AuthApp
        val startDest = runBlocking {
            if (app.container.checkAuthUseCase()) Screen.UsersList.route else Screen.Login.route
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
                    val usersViewModel: UsersViewModel = viewModel(factory = UsersViewModel.Factory)

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        composable(route = Screen.Login.route) {
                            LoginScreen(
                                viewModel = loginViewModel,
                                onNavigateToUsers = {
                                    // Очищаем стек, чтобы кнопка "назад" не вернула на логин
                                    navController.navigate(Screen.UsersList.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(route = Screen.UsersList.route) {
                            UsersListScreen(
                                viewModel = usersViewModel,
                                onUserClick = { userId ->
                                    navController.navigate(Screen.UserDetail.createRoute(userId))
                                }
                            )
                        }

                        composable(route = Screen.UserDetail.route) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()

                            if (userId != null) {
                                UserDetailScreen(
                                    userId = userId,
                                    viewModel = usersViewModel,
                                    onBackClick = { navController.navigateUp() },
                                    onLogoutClick = {
                                        // При выходе скидываем весь стек и идем на логин
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}