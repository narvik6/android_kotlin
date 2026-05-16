package com.example.module6_t3.data.repository

import com.example.module6_t3.data.mapper.toDomain
import com.example.module6_t3.data.remote.dto.UserDto
import com.example.module6_t3.data.remote.dto.UsersResponseDto
import com.example.module6_t3.domain.model.User
import com.example.module6_t3.domain.repository.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UserRepositoryImpl(
    private val client: HttpClient
) : UserRepository {

    override suspend fun getUsers(): Result<List<User>> {
        return try {
            val response: UsersResponseDto = client.get("https://dummyjson.com/users").body()
            Result.success(response.users.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserById(id: Int): Result<User> {
        return try {
            val response: UserDto = client.get("https://dummyjson.com/users/$id").body()
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}