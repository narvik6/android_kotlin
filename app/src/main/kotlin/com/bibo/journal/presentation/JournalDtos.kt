package com.bibo.journal.presentation

import com.bibo.journal.domain.JournalItem
import com.bibo.journal.domain.JournalItemType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class JournalItemResponse(
    val type: JournalItemType,
    val id: String,
    val dateTime: Instant? = null,
    val text: String? = null,
    val mood: Int? = null,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val durationSeconds: Long? = null,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun JournalItem.toResponse(): JournalItemResponse =
    JournalItemResponse(
        type = type,
        id = id,
        dateTime = dateTime,
        text = text,
        mood = mood,
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
