package com.bibo.meditationsessions.data

import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import com.bibo.meditationsessions.domain.MeditationSession
import kotlinx.datetime.Instant

fun FirestoreMeditationSessionModel.toDomain(): MeditationSession =
    MeditationSession(
        id = id,
        ownerUserId = ownerUserId,
        startedAt = startedAt.toKotlinInstant(),
        endedAt = endedAt.toKotlinInstant(),
        durationSeconds = durationSeconds,
        note = note,
        createdAt = createdAt.toKotlinInstant(),
        updatedAt = updatedAt.toKotlinInstant(),
    )

fun DocumentSnapshot.toFirestoreMeditationSessionModel(): FirestoreMeditationSessionModel? {
    val data = data ?: return null

    return FirestoreMeditationSessionModel(
        id = data["id"] as? String ?: id,
        ownerUserId = data["ownerUserId"] as? String ?: return null,
        startedAt = data["startedAt"] as? Timestamp ?: return null,
        endedAt = data["endedAt"] as? Timestamp ?: return null,
        durationSeconds = (data["durationSeconds"] as? Number)?.toLong() ?: return null,
        note = data["note"] as? String,
        createdAt = data["createdAt"] as? Timestamp ?: return null,
        updatedAt = data["updatedAt"] as? Timestamp ?: return null,
    )
}

fun FirestoreMeditationSessionModel.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "startedAt" to startedAt,
        "endedAt" to endedAt,
        "durationSeconds" to durationSeconds,
        "note" to note,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Instant.toFirestoreTimestamp(): Timestamp =
    Timestamp.ofTimeSecondsAndNanos(epochSeconds, nanosecondsOfSecond)

fun Timestamp.toKotlinInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanos.toLong())
