package com.bibo.meditationsessions.data

import com.google.cloud.Timestamp

data class FirestoreMeditationSessionModel(
    val id: String,
    val ownerUserId: String,
    val startedAt: Timestamp,
    val endedAt: Timestamp,
    val durationSeconds: Long,
    val note: String?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
