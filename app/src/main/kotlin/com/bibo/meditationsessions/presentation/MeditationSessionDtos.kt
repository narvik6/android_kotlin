package com.bibo.meditationsessions.presentation

import com.bibo.meditationsessions.domain.MeditationSession
import com.bibo.meditationsessions.domain.MeditationSessionInput
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateMeditationSessionRequest(
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val durationSeconds: Long? = null,
    val note: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class UpdateMeditationSessionRequest(
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val durationSeconds: Long? = null,
    val note: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class MeditationSessionResponse(
    val id: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun CreateMeditationSessionRequest.toInput(): MeditationSessionInput =
    MeditationSessionInput(
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun UpdateMeditationSessionRequest.toInput(): MeditationSessionInput =
    MeditationSessionInput(
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun MeditationSession.toResponse(): MeditationSessionResponse =
    MeditationSessionResponse(
        id = id,
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
