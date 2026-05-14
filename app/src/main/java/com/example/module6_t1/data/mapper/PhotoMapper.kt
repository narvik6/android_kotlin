package com.example.module6_t1.data.mapper

import com.example.module6_t1.data.remote.dto.PhotoDto
import com.example.module6_t1.domain.model.Photo

fun PhotoDto.toDomain(): Photo {
    return Photo(
        id = id,
        author = author,
        width = width,
        height = height,
        url = url,
        downloadUrl = downloadUrl
    )
}