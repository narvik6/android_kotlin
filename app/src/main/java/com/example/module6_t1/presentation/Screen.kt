package com.example.module6_t1.presentation

sealed class Screen(val route: String) {
    data object PhotoList : Screen("photo_list")
    data object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: String) = "photo_detail/$photoId"
    }
}