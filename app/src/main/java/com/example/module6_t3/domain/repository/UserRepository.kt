package com.example.module6_t3.domain.repository

import com.example.module6_t3.domain.model.User

interface UserRepository {
    suspend fun getUsers(): Result<List<User>>
    suspend fun getUserById(id: Int): Result<User>
}