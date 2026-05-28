package com.bibo.meditationsessions.domain

import kotlinx.datetime.Instant

data class MeditationSession(
    val id: String,
    val ownerUserId: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
