package org.example.domain.model

data class NobelPrize(
    val year: String,
    val category: String,
    val laureates: List<Laureate>
)

data class Laureate(
    val id: String,
    val fullName: String,
    val motivation: String
)