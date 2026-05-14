package com.example.module6_t1to3.data.remote

import com.example.module6_t1to3.data.remote.dto.PhotoDto
import retrofit2.http.GET

interface PicsumApi {
    @GET("v2/list")
    suspend fun getPhotos(): List<PhotoDto>
}