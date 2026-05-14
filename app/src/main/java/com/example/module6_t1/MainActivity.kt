package com.example.module6_t1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.module6_t1.presentation.Screen
import com.example.module6_t1.presentation.photo_detail.PhotoDetailScreen
import com.example.module6_t1.presentation.photos_list.PhotosListScreen
import com.example.module6_t1.presentation.photos_list.PhotosViewModel

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

                    // Создаем ViewModel с помощью нашей ручной фабрики
                    val viewModel: PhotosViewModel = viewModel(factory = PhotosViewModel.Factory)

                    NavHost(
                        navController = navController,
                        startDestination = Screen.PhotoList.route
                    ) {
                        composable(route = Screen.PhotoList.route) {
                            PhotosListScreen(
                                viewModel = viewModel,
                                onPhotoClick = { photoId ->
                                    navController.navigate(Screen.PhotoDetail.createRoute(photoId))
                                }
                            )
                        }

                        composable(route = Screen.PhotoDetail.route) { backStackEntry ->
                            val photoId = backStackEntry.arguments?.getString("photoId")
                            // Вытаскиваем нужную фотку из уже загруженных в ViewModel
                            val photo = photoId?.let { viewModel.getPhotoById(it) }

                            PhotoDetailScreen(
                                photo = photo,
                                onBackClick = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}