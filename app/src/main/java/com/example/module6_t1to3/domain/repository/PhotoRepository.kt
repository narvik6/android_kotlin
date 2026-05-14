package com.example.module6_t1to3.domain.repository

import com.example.module6_t1to3.domain.model.Photo

interface PhotoRepository {
    suspend fun getPhotos(): Result<List<Photo>>
}