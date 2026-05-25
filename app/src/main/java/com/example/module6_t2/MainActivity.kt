package com.example.module6_t2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.module6_t2.presentation.Screen
import com.example.module6_t2.presentation.nobel_detail.NobelDetailScreen
import com.example.module6_t2.presentation.nobel_list.NobelListScreen
import com.example.module6_t2.presentation.nobel_list.NobelUiState
import com.example.module6_t2.presentation.nobel_list.NobelViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val viewModel: NobelViewModel = viewModel(factory = NobelViewModel.Factory)

                    NavHost(
                        navController = navController,
                        startDestination = Screen.NobelList.route
                    ) {
                        composable(route = Screen.NobelList.route) {
                            NobelListScreen(
                                viewModel = viewModel,
                                onLaureateClick = { id ->
                                    navController.navigate(Screen.NobelDetail.createRoute(id))
                                }
                            )
                        }

                        composable(route = Screen.NobelDetail.route) { backStackEntry ->
                            val laureateId = backStackEntry.arguments?.getString("laureateId")
                            val uiState by viewModel.uiState.collectAsState()
                            val laureate = laureateId?.let { viewModel.getLaureateById(it) }

                            NobelDetailScreen(
                                laureate = laureate,
                                onBackClick = { navController.navigateUp() },
                                isLoading = uiState is NobelUiState.Loading,
                                errorMessage = (uiState as? NobelUiState.Error)?.message,
                                onRetry = { viewModel.loadPrizes() }
                            )
                        }
                    }
                }
            }
        }
    }
}
