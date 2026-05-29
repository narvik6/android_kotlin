package com.bibo.android.features.meditation.domain

import kotlinx.coroutines.flow.Flow

class StartMeditationTimerUseCase {
    operator fun invoke(durationSeconds: Long, nowMillis: Long): MeditationTimerSnapshot =
        MeditationTimerSnapshot(
            startedAtMillis = nowMillis,
            durationSeconds = durationSeconds.coerceAtLeast(1),
        )
}

data class MeditationTimerSnapshot(
    val startedAtMillis: Long,
    val durationSeconds: Long,
)

class FinishMeditationSessionUseCase(
    private val repository: MeditationRepository,
) {
    suspend operator fun invoke(startedAt: String, endedAt: String, durationSeconds: Long, note: String?) =
        repository.createSession(startedAt, endedAt, durationSeconds, note)
}

class GetMeditationSessionsUseCase(
    private val repository: MeditationRepository,
) {
    operator fun invoke(): Flow<List<MeditationSession>> = repository.observeSessions()
}

class GetMeditationSessionByIdUseCase(
    private val repository: MeditationRepository,
) {
    suspend operator fun invoke(localId: String): MeditationSession? = repository.getSessionById(localId)
}

class CreateMeditationSessionUseCase(
    private val repository: MeditationRepository,
) {
    suspend operator fun invoke(startedAt: String, endedAt: String, durationSeconds: Long, note: String?) =
        repository.createSession(startedAt, endedAt, durationSeconds, note)
}

class UpdateMeditationSessionUseCase(
    private val repository: MeditationRepository,
) {
    suspend operator fun invoke(
        localId: String,
        startedAt: String,
        endedAt: String,
        durationSeconds: Long,
        note: String?,
    ) = repository.updateSession(localId, startedAt, endedAt, durationSeconds, note)
}

class DeleteMeditationSessionUseCase(
    private val repository: MeditationRepository,
) {
    suspend operator fun invoke(localId: String) = repository.deleteSession(localId)
}

class SyncMeditationPendingChangesUseCase(
    private val repository: MeditationRepository,
) {
    suspend operator fun invoke() = repository.syncPendingChanges()
}
