package com.bibo.meditationsessions.domain

interface MeditationSessionRepository {
    suspend fun findByUserId(ownerUserId: String, query: String?): List<MeditationSession>

    suspend fun findByIdForUser(ownerUserId: String, sessionId: String): MeditationSession?

    suspend fun create(ownerUserId: String, input: MeditationSessionInput): MeditationSession

    suspend fun update(ownerUserId: String, sessionId: String, input: MeditationSessionInput): MeditationSession?

    suspend fun delete(ownerUserId: String, sessionId: String): Boolean
}
