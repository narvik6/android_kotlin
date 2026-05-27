package com.bibo.core.error

import io.ktor.http.HttpStatusCode

open class AppException(
    val code: String,
    override val message: String,
    val status: HttpStatusCode,
) : RuntimeException(message)

class ValidationException(message: String) :
    AppException("VALIDATION_ERROR", message, HttpStatusCode.BadRequest)

class NotFoundException(message: String) :
    AppException("NOT_FOUND", message, HttpStatusCode.NotFound)

class UnauthorizedException(message: String = "Unauthorized") :
    AppException("UNAUTHORIZED", message, HttpStatusCode.Unauthorized)

class ConflictException(message: String) :
    AppException("CONFLICT", message, HttpStatusCode.Conflict)
