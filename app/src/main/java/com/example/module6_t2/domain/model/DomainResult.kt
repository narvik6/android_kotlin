package com.example.module6_t2.domain.model

sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Error(val error: DomainError) : DomainResult<Nothing>
}

sealed interface DomainError {
    val message: String

    data object Network : DomainError {
        override val message = "Проблема с подключением к серверу"
    }

    data class Server(val code: Int? = null) : DomainError {
        override val message = code?.let { "Ошибка сервера: $it" } ?: "Ошибка сервера"
    }

    data object Parsing : DomainError {
        override val message = "Не удалось обработать ответ сервера"
    }

    data class Unknown(override val message: String = "Неизвестная ошибка") : DomainError
}

inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onError: (DomainError) -> R
): R {
    return when (this) {
        is DomainResult.Success -> onSuccess(data)
        is DomainResult.Error -> onError(error)
    }
}
