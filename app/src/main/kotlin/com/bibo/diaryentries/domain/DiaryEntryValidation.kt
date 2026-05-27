package com.bibo.diaryentries.domain

import com.bibo.core.error.ValidationException

fun DiaryEntryInput.validated(): DiaryEntryInput {
    if (dateTime == null) {
        throw ValidationException("dateTime is required")
    }
    if (createdAt == null) {
        throw ValidationException("createdAt is required")
    }
    if (updatedAt == null) {
        throw ValidationException("updatedAt is required")
    }
    if (mood != null && mood !in ALLOWED_MOODS) {
        throw ValidationException("mood must be -1, 0, 1, or null")
    }

    return copy(
        text = text?.trim()?.takeIf(String::isNotEmpty),
    )
}

private val ALLOWED_MOODS = setOf(-1, 0, 1)
