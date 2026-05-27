package com.bibo.auth.data

import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import com.bibo.auth.domain.User
import java.time.Instant

fun FirestoreUserModel.toDomain(): User =
    User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        createdAt = Instant.ofEpochSecond(createdAt.seconds, createdAt.nanos.toLong()),
    )

fun DocumentSnapshot.toFirestoreUserModel(): FirestoreUserModel? {
    val data = data ?: return null

    return FirestoreUserModel(
        id = data["id"] as? String ?: id,
        email = data["email"] as? String ?: return null,
        passwordHash = data["passwordHash"] as? String ?: return null,
        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
    )
}

fun FirestoreUserModel.toFirestoreMap(): Map<String, Any> =
    mapOf(
        "id" to id,
        "email" to email,
        "passwordHash" to passwordHash,
        "createdAt" to createdAt,
    )
