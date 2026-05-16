package com.example.module6_t3.domain.usecase

import com.example.module6_t3.domain.model.User
import com.example.module6_t3.domain.repository.UserRepository

class GetUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<List<User>> {
        return repository.getUsers()
    }
}

class GetUserByIdUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(id: Int): Result<User> {
        return repository.getUserById(id)
    }
}