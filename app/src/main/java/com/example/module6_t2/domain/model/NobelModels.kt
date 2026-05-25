package com.example.module6_t2.domain.model

data class NobelPrize(
    val year: String,
    val category: String,
    val laureates: List<Laureate>
)

data class Laureate(
    val id: String,
    val fullName: String,
    val motivation: String,
    val birthCountry: String? = null,
    val birthPlace: String? = null,
    val portraitUrl: String? = null
)
