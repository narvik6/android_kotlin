package com.example.module6_t3.data.repository

import com.example.module6_t3.data.local.TokenManager
import com.example.module6_t3.data.remote.dto.LoginRequestDto
import com.example.module6_t3.data.remote.dto.LoginResponseDto
import com.example.module6_t3.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val response: LoginResponseDto = client.post("https://dummyjson.com/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(username, password))
            }.body()

            // Сохраняем токен в DataStore
            tokenManager.saveToken(response.accessToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось войти: проверьте логин и пароль"))
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
    }

    override suspend fun getToken(): String? {
        return tokenManager.getToken()
    }
}