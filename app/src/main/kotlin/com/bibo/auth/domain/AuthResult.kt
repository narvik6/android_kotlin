package com.bibo.auth.domain

data class AuthResult(
    val token: String,
    val user: User,
)
