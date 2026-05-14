package com.example.module6_t2.domain.usecase

import com.example.module6_t2.domain.model.NobelPrize
import com.example.module6_t2.domain.repository.NobelRepository

class GetNobelPrizesUseCase(private val repository: NobelRepository) {
    suspend operator fun invoke(year: String? = null, category: String? = null): Result<List<NobelPrize>> {
        return repository.getNobelPrizes(year, category)
    }
}