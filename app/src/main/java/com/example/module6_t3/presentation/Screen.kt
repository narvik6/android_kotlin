package com.example.module6_t3.presentation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object UsersList : Screen("users_list")
    data object UserDetail : Screen("user_detail/{userId}") {
        fun createRoute(userId: Int) = "user_detail/$userId"
    }
}