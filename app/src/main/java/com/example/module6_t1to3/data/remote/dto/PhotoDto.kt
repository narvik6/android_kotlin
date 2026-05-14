package com.example.module6_t1to3.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotoDto(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    @SerialName("download_url") val downloadUrl: String
)