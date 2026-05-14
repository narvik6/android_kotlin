package com.example.module6_t2.presentation

sealed class Screen(val route: String) {
    data object NobelList : Screen("nobel_list")
    data object NobelDetail : Screen("nobel_detail/{laureateId}") {
        fun createRoute(laureateId: String) = "nobel_detail/$laureateId"
    }
}