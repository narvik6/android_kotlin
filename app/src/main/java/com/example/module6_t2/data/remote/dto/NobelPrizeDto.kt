package com.example.module6_t2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class NobelPrizeDto(
    val id: Int,
    val year: JsonElement? = null,
    val awardYear: JsonElement? = null,
    @SerialName("award_year")
    val awardYearSnakeCase: JsonElement? = null,
    val category: String,
    val laureates: List<LaureateDto> = emptyList(),
    val fullName: String? = null,
    @SerialName("full_name")
    val fullNameSnakeCase: String? = null,
    val motivation: String? = null,
    @SerialName("detail_link")
    val detailLink: String? = null
)

@Serializable
data class LaureateDto(
    val id: Int,
    val fullName: String? = null,
    @SerialName("full_name")
    val fullNameSnakeCase: String? = null,
    val portion: String? = null,
    val motivation: String? = null,
    val portraitUrl: String? = null,
    @SerialName("portrait_url")
    val portraitUrlSnakeCase: String? = null,
    val birthCountry: String? = null,
    @SerialName("birth_country")
    val birthCountrySnakeCase: String? = null,
    val birthPlace: String? = null,
    @SerialName("birth_place")
    val birthPlaceSnakeCase: String? = null
)
