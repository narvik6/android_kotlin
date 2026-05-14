package com.example.module6_t2.presentation.model

data class LaureateUiItem(
    val id: String,
    val year: String,
    val category: String,
    val fullName: String,
    val motivation: String,
    val birthCountry: String? = null
)