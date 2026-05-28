package com.bibo.meditationsessions.domain

import com.bibo.core.error.ValidationException

fun MeditationSessionInput.validated(): MeditationSessionInput {
    if (startedAt == null) {
        throw ValidationException("startedAt is required")
    }
    if (endedAt == null) {
        throw ValidationException("endedAt is required")
    }
    if (durationSeconds == null) {
        throw ValidationException("durationSeconds is required")
    }
    if (durationSeconds < 0) {
        throw ValidationException("durationSeconds must be greater than or equal to 0")
    }
    if (createdAt == null) {
        throw ValidationException("createdAt is required")
    }
    if (updatedAt == null) {
        throw ValidationException("updatedAt is required")
    }

    return copy(
        note = note?.trim()?.takeIf(String::isNotEmpty),
    )
}
