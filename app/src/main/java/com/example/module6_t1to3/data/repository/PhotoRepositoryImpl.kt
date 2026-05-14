package com.example.module6_t1to3.data.repository

import com.example.module6_t1to3.data.mapper.toDomain
import com.example.module6_t1to3.data.remote.PicsumApi
import com.example.module6_t1to3.domain.model.Photo
import com.example.module6_t1to3.domain.repository.PhotoRepository

class PhotoRepositoryImpl(
    private val api: PicsumApi
) : PhotoRepository {
    override suspend fun getPhotos(): Result<List<Photo>> {
        return try {
            val response = api.getPhotos()
            Result.success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}