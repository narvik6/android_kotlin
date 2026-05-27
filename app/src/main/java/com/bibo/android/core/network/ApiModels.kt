@file:OptIn(kotlin.time.ExperimentalTime::class)
@file:Suppress("DEPRECATION")

package com.bibo.android.core.network

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)

@Serializable
data class CreateDiaryEntryRequest(
    val text: String? = null,
    val mood: Int? = null,
    val dateTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class UpdateDiaryEntryRequest(
    val text: String? = null,
    val mood: Int? = null,
    val dateTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class DiaryEntryResponse(
    val id: String,
    val text: String?,
    val mood: Int?,
    val dateTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class CreateMeditationSessionRequest(
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class UpdateMeditationSessionRequest(
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
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

@Serializable
enum class JournalItemType {
    DIARY_ENTRY,
    MEDITATION_SESSION,
}
