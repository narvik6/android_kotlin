package com.bibo.journal.domain

import kotlinx.datetime.Instant

data class JournalItem(
    val type: JournalItemType,
    val id: String,
    val dateTime: Instant?,
    val text: String?,
    val mood: Int?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class JournalItemType {
    DIARY_ENTRY,
    MEDITATION_SESSION,
}
