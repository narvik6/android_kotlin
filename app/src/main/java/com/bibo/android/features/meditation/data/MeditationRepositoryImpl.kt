@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bibo.android.features.meditation.data

import com.bibo.android.core.database.MeditationSessionDao
import com.bibo.android.core.database.MeditationSessionEntity
import com.bibo.android.core.database.SyncStatus
import com.bibo.android.core.datastore.CurrentUserProvider
import com.bibo.android.core.network.CreateMeditationSessionRequest
import com.bibo.android.core.network.UpdateMeditationSessionRequest
import com.bibo.android.core.sync.SyncQueue
import com.bibo.android.features.meditation.domain.MeditationRepository
import com.bibo.android.features.meditation.domain.MeditationSession
import com.bibo.android.features.meditation.domain.PendingSyncOperation
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class MeditationRepositoryImpl(
    private val meditationSessionDao: MeditationSessionDao,
    private val currentUserProvider: CurrentUserProvider,
    private val syncQueue: SyncQueue,
) : MeditationRepository {
    override fun observeSessions(): Flow<List<MeditationSession>> =
        currentUserProvider.authData.flatMapLatest { auth ->
            val ownerUserId = auth.userId
            if (ownerUserId == null) {
                flowOf(emptyList())
            } else {
                meditationSessionDao.observeActiveSessions(ownerUserId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override suspend fun getSessionById(localId: String): MeditationSession? {
        val ownerUserId = currentOwnerUserId() ?: return null
        return meditationSessionDao.getByLocalId(ownerUserId, localId)
            ?.takeUnless { it.deletedLocally }
            ?.takeIf { it.remoteId != null || it.syncStatus != SyncStatus.SYNCED }
            ?.toDomain()
    }

    override suspend fun createSession(
        startedAt: String,
        endedAt: String,
        durationSeconds: Long,
        note: String?,
    ) {
        val ownerUserId = currentOwnerUserId() ?: return
        val localId = UUID.randomUUID().toString()
        val cleanedNote = note.cleaned()
        val normalizedDurationSeconds = durationSeconds.coerceAtLeast(0)
        val request = CreateMeditationSessionRequest(
            startedAt = Instant.parse(startedAt),
            endedAt = Instant.parse(endedAt),
            durationSeconds = normalizedDurationSeconds,
            note = cleanedNote,
            createdAt = Instant.parse(startedAt),
            updatedAt = Instant.parse(endedAt),
        )
        meditationSessionDao.upsert(
            MeditationSessionEntity(
                localId = localId,
                remoteId = null,
                ownerUserId = ownerUserId,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = normalizedDurationSeconds,
                note = cleanedNote,
                syncStatus = SyncStatus.PENDING_CREATE,
                deletedLocally = false,
                createdAt = startedAt,
                updatedAt = endedAt,
            ),
        )
        syncQueue.enqueueMeditationCreate(ownerUserId, localId, request)
    }

    override suspend fun updateSession(
        localId: String,
        startedAt: String,
        endedAt: String,
        durationSeconds: Long,
        note: String?,
    ) {
        val ownerUserId = currentOwnerUserId() ?: return
        val existing = meditationSessionDao.getByLocalId(ownerUserId, localId)
            ?.takeUnless { it.deletedLocally }
            ?: return
        val cleanedNote = note.cleaned()
        val normalizedDurationSeconds = durationSeconds.coerceAtLeast(0)
        if (
            existing.startedAt == startedAt &&
            existing.endedAt == endedAt &&
            existing.durationSeconds == normalizedDurationSeconds &&
            existing.note == cleanedNote
        ) {
            return
        }

        val updated = existing.copy(
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = normalizedDurationSeconds,
            note = cleanedNote,
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = endedAt,
        )
        meditationSessionDao.update(updated)
        syncQueue.enqueueMeditationUpdate(ownerUserId, localId, updated.toUpdateRequest())
    }

    override suspend fun deleteSession(localId: String) {
        val ownerUserId = currentOwnerUserId() ?: return
        val existing = meditationSessionDao.getByLocalId(ownerUserId, localId)
            ?.takeUnless { it.deletedLocally }
            ?: return
        meditationSessionDao.update(
            existing.copy(
                syncStatus = SyncStatus.PENDING_DELETE,
                deletedLocally = true,
            ),
        )
        syncQueue.enqueueMeditationDelete(ownerUserId, localId)
    }

    override suspend fun refreshFromServer() {
        syncQueue.syncCurrentUser()
    }

    private suspend fun currentOwnerUserId(): String? = currentUserProvider.currentUserId()
}

private fun MeditationSessionEntity.toDomain(): MeditationSession =
    MeditationSession(
        localId = localId,
        remoteId = remoteId,
        ownerUserId = ownerUserId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pendingOperation = syncStatus.toPendingOperation(),
    )

private fun MeditationSessionEntity.toUpdateRequest(): UpdateMeditationSessionRequest =
    UpdateMeditationSessionRequest(
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(endedAt),
        durationSeconds = durationSeconds.coerceAtLeast(0),
        note = note,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun SyncStatus.toPendingOperation(): PendingSyncOperation? = when (this) {
    SyncStatus.PENDING_CREATE -> PendingSyncOperation.Create
    SyncStatus.PENDING_UPDATE -> PendingSyncOperation.Update
    SyncStatus.PENDING_DELETE -> PendingSyncOperation.Delete
    SyncStatus.SYNCED -> null
}
