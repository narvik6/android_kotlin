package com.example.module6_t2.domain.repository

import com.example.module6_t2.domain.model.NobelPrize

interface NobelRepository {
    suspend fun getNobelPrizes(year: String?, category: String?): Result<List<NobelPrize>>
}