package com.bibo.meditationsessions.data

import com.google.cloud.firestore.Firestore
import com.bibo.core.firestore.FirestoreProvider
import com.bibo.meditationsessions.domain.MeditationSession
import com.bibo.meditationsessions.domain.MeditationSessionInput
import com.bibo.meditationsessions.domain.MeditationSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreMeditationSessionRepository(
    private val firestoreProvider: FirestoreProvider,
) : MeditationSessionRepository {
    override suspend fun findByUserId(ownerUserId: String, query: String?): List<MeditationSession> =
        withContext(Dispatchers.IO) {
            val sessions = meditationSessionsCollection()
                .whereEqualTo("ownerUserId", ownerUserId)
                .get()
                .get()
                .documents
                .mapNotNull { it.toFirestoreMeditationSessionModel()?.toDomain() }
                .sortedByDescending { it.startedAt }

            query?.let { sessions.filterByQuery(it) } ?: sessions
        }

    override suspend fun findByIdForUser(ownerUserId: String, sessionId: String): MeditationSession? =
        withContext(Dispatchers.IO) {
            meditationSessionsCollection()
                .document(sessionId)
                .get()
                .get()
                .toFirestoreMeditationSessionModel()
                ?.toDomain()
                ?.takeIf { it.ownerUserId == ownerUserId }
        }

    override suspend fun create(ownerUserId: String, input: MeditationSessionInput): MeditationSession =
        withContext(Dispatchers.IO) {
            val document = meditationSessionsCollection().document()
            val model = FirestoreMeditationSessionModel(
                id = document.id,
                ownerUserId = ownerUserId,
                startedAt = requireNotNull(input.startedAt).toFirestoreTimestamp(),
                endedAt = requireNotNull(input.endedAt).toFirestoreTimestamp(),
                durationSeconds = requireNotNull(input.durationSeconds),
                note = input.note,
                createdAt = requireNotNull(input.createdAt).toFirestoreTimestamp(),
                updatedAt = requireNotNull(input.updatedAt).toFirestoreTimestamp(),
            )

            document.set(model.toFirestoreMap()).get()

            model.toDomain()
        }

    override suspend fun update(
        ownerUserId: String,
        sessionId: String,
        input: MeditationSessionInput,
    ): MeditationSession? =
        withContext(Dispatchers.IO) {
            val existing = findByIdForUser(ownerUserId, sessionId) ?: return@withContext null
            val model = FirestoreMeditationSessionModel(
                id = existing.id,
                ownerUserId = existing.ownerUserId,
                startedAt = requireNotNull(input.startedAt).toFirestoreTimestamp(),
                endedAt = requireNotNull(input.endedAt).toFirestoreTimestamp(),
                durationSeconds = requireNotNull(input.durationSeconds),
                note = input.note,
                createdAt = requireNotNull(input.createdAt).toFirestoreTimestamp(),
                updatedAt = requireNotNull(input.updatedAt).toFirestoreTimestamp(),
            )

            meditationSessionsCollection().document(sessionId).set(model.toFirestoreMap()).get()

            model.toDomain()
        }

    override suspend fun delete(ownerUserId: String, sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            findByIdForUser(ownerUserId, sessionId) ?: return@withContext false

            meditationSessionsCollection().document(sessionId).delete().get()
            true
        }

    private fun List<MeditationSession>.filterByQuery(query: String): List<MeditationSession> {
        val normalizedQuery = query.lowercase()

        return filter { session ->
            session.note?.contains(normalizedQuery, ignoreCase = true) == true ||
                session.startedAt.toString().lowercase().contains(normalizedQuery) ||
                session.endedAt.toString().lowercase().contains(normalizedQuery) ||
                session.durationSeconds.toString().contains(normalizedQuery) ||
                session.createdAt.toString().lowercase().contains(normalizedQuery) ||
                session.updatedAt.toString().lowercase().contains(normalizedQuery)
        }
    }

    private fun meditationSessionsCollection() =
        firestore().collection(MEDITATION_SESSIONS_COLLECTION)

    private fun firestore(): Firestore =
        firestoreProvider.get()

    private companion object {
        const val MEDITATION_SESSIONS_COLLECTION = "meditation_sessions"
    }
}
