package com.example.module6_t3.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    val expiresInMins: Int = 60
)

@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val image: String
)

@Serializable
data class UsersResponseDto(
    val users: List<UserDto>
)

@Serializable
data class UserDto(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val image: String
)