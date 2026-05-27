package com.bibo.android.core.util

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val error: DomainError) : AppResult<Nothing>
}

data class DomainError(
    val code: String,
    val message: String,
)
