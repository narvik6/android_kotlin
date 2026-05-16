package com.example.module6_t3.domain.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun getToken(): String?
}