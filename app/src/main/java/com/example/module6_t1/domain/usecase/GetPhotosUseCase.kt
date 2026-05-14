package com.example.module6_t1.domain.usecase

import com.example.module6_t1.domain.model.Photo
import com.example.module6_t1.domain.repository.PhotoRepository

class GetPhotosUseCase(private val repository: PhotoRepository) {
    suspend operator fun invoke(): Result<List<Photo>> {
        return repository.getPhotos()
    }
}