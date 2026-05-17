package org.example.di

import org.example.data.repository.PrizeRepositoryImpl
import org.example.domain.usecase.AuthUseCase

object Injection {
    val prizeRepository = PrizeRepositoryImpl()
    val authUseCase = AuthUseCase()
}