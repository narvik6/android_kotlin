package com.bibo.auth.domain

import com.bibo.core.error.ValidationException

fun normalizeAndValidateEmail(email: String): String {
    val normalized = email.trim().lowercase()

    if (!EMAIL_REGEX.matches(normalized)) {
        throw ValidationException("Invalid email")
    }

    return normalized
}

fun validatePassword(password: String) {
    if (password.length < MIN_PASSWORD_LENGTH) {
        throw ValidationException("Password must contain at least $MIN_PASSWORD_LENGTH characters")
    }
}

private const val MIN_PASSWORD_LENGTH = 6
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
