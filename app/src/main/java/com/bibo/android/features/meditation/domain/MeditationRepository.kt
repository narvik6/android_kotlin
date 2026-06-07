package com.bibo.android.features.meditation.domain

import kotlinx.coroutines.flow.Flow

interface MeditationRepository {
    fun observeSessions(): Flow<List<MeditationSession>>
    suspend fun getSessionById(localId: String): MeditationSession?
    suspend fun createSession(startedAt: String, endedAt: String, durationSeconds: Long, note: String?)
    suspend fun updateSession(localId: String, startedAt: String, endedAt: String, durationSeconds: Long, note: String?)
    suspend fun deleteSession(localId: String)
    suspend fun refreshFromServer()
}
