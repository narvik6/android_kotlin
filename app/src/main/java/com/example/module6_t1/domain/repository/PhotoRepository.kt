package com.example.module6_t1.domain.repository

import com.example.module6_t1.domain.model.Photo

interface PhotoRepository {
    suspend fun getPhotos(): Result<List<Photo>>
}