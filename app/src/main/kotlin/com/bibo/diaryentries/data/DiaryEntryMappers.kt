package com.bibo.diaryentries.data

import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import com.bibo.diaryentries.domain.DiaryEntry
import kotlinx.datetime.Instant

fun FirestoreDiaryEntryModel.toDomain(): DiaryEntry =
    DiaryEntry(
        id = id,
        ownerUserId = ownerUserId,
        text = text,
        mood = mood,
        dateTime = dateTime.toKotlinInstant(),
        createdAt = createdAt.toKotlinInstant(),
        updatedAt = updatedAt.toKotlinInstant(),
    )

fun DocumentSnapshot.toFirestoreDiaryEntryModel(): FirestoreDiaryEntryModel? {
    val data = data ?: return null

    return FirestoreDiaryEntryModel(
        id = data["id"] as? String ?: id,
        ownerUserId = data["ownerUserId"] as? String ?: return null,
        text = data["text"] as? String,
        mood = (data["mood"] as? Number)?.toInt(),
        dateTime = data["dateTime"] as? Timestamp ?: return null,
        createdAt = data["createdAt"] as? Timestamp ?: return null,
        updatedAt = data["updatedAt"] as? Timestamp ?: return null,
    )
}

fun FirestoreDiaryEntryModel.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "text" to text,
        "mood" to mood,
        "dateTime" to dateTime,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

fun Instant.toFirestoreTimestamp(): Timestamp =
    Timestamp.ofTimeSecondsAndNanos(epochSeconds, nanosecondsOfSecond)

fun Timestamp.toKotlinInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanos.toLong())
