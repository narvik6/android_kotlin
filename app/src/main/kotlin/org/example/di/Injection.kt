package org.example.di

import org.example.data.repository.PrizeRepositoryImpl
import org.example.data.repository.UserRepositoryImpl
import org.example.domain.usecase.AuthUseCase
import org.example.domain.usecase.SyncPrizesUseCase

object Injection {
    val userRepository = UserRepositoryImpl()
    val prizeRepository = PrizeRepositoryImpl()
    val authUseCase = AuthUseCase(userRepository)
    val syncPrizesUseCase = SyncPrizesUseCase()
}