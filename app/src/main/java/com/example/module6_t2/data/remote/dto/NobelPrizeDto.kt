package com.example.module6_t2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NobelPrizeDto(
    val id: Int,
    val year: String,
    val category: String,
    val laureates: List<LaureateDto> = emptyList()
)

@Serializable
data class LaureateDto(
    val id: Int,
    val fullName: String,
    val motivation: String? = null
)