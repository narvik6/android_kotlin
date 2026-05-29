@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bibo.android.features.meditation.data

import com.bibo.android.core.database.MeditationSessionDao
import com.bibo.android.core.database.MeditationSessionEntity
import com.bibo.android.core.database.SyncStatus
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.network.ApiClient
import com.bibo.android.core.network.CreateMeditationSessionRequest
import com.bibo.android.core.network.MeditationSessionResponse
import com.bibo.android.core.network.UpdateMeditationSessionRequest
import com.bibo.android.core.util.AppResult
import com.bibo.android.features.meditation.domain.MeditationRepository
import com.bibo.android.features.meditation.domain.MeditationSession
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlin.time.Clock

class MeditationRepositoryImpl(
    private val meditationSessionDao: MeditationSessionDao,
    private val apiClient: ApiClient,
    private val userPreferences: UserPreferences,
) : MeditationRepository {
    override fun observeSessions(): Flow<List<MeditationSession>> =
        userPreferences.authData.flatMapLatest { auth ->
            val ownerUserId = auth.userId
            if (ownerUserId == null) {
                flowOf(emptyList())
            } else {
                meditationSessionDao.observeActiveSessions(ownerUserId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override suspend fun getSessionById(localId: String): MeditationSession? =
        meditationSessionDao.getByLocalId(localId)?.takeUnless { it.deletedLocally }?.toDomain()

    override suspend fun createSession(
        startedAt: String,
        endedAt: String,
        durationSeconds: Long,
        note: String?,
    ) {
        val ownerUserId = requireOwnerUserId()
        val now = nowIso()
        meditationSessionDao.upsert(
            MeditationSessionEntity(
                localId = UUID.randomUUID().toString(),
                remoteId = null,
                ownerUserId = ownerUserId,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = durationSeconds.coerceAtLeast(0),
                note = note.cleaned(),
                syncStatus = SyncStatus.PENDING_CREATE,
                deletedLocally = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        syncPendingChanges()
    }

    override suspend fun updateSession(
        localId: String,
        startedAt: String,
        endedAt: String,
        durationSeconds: Long,
        note: String?,
    ) {
        val existing = meditationSessionDao.getByLocalId(localId) ?: return
        meditationSessionDao.update(
            existing.copy(
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = durationSeconds.coerceAtLeast(0),
                note = note.cleaned(),
                syncStatus = MeditationSyncPolicy.statusAfterLocalUpdate(existing.syncStatus),
                updatedAt = nowIso(),
            ),
        )
        syncPendingChanges()
    }

    override suspend fun deleteSession(localId: String) {
        val existing = meditationSessionDao.getByLocalId(localId) ?: return
        if (existing.remoteId == null && existing.syncStatus == SyncStatus.PENDING_CREATE) {
            meditationSessionDao.delete(existing)
            return
        }

        meditationSessionDao.update(
            existing.copy(
                deletedLocally = true,
                syncStatus = SyncStatus.PENDING_DELETE,
                updatedAt = nowIso(),
            ),
        )
        syncPendingChanges()
    }

    override suspend fun syncPendingChanges() {
        val ownerUserId = currentOwnerUserId() ?: return
        meditationSessionDao.getPendingSessions(ownerUserId).forEach { entity ->
            when (entity.syncStatus) {
                SyncStatus.PENDING_CREATE -> syncCreate(entity)
                SyncStatus.PENDING_UPDATE -> syncUpdate(entity)
                SyncStatus.PENDING_DELETE -> syncDelete(entity)
                SyncStatus.ERROR -> retryError(entity)
                SyncStatus.SYNCED -> Unit
            }
        }

        when (val remoteResult = apiClient.getMeditationSessions()) {
            is AppResult.Error -> Unit
            is AppResult.Success -> mergeRemote(ownerUserId, remoteResult.value)
        }
    }

    private suspend fun syncCreate(entity: MeditationSessionEntity) {
        when (val result = apiClient.createMeditationSession(entity.toCreateRequest())) {
            is AppResult.Success -> meditationSessionDao.upsert(entity.mergeSynced(result.value))
            is AppResult.Error -> meditationSessionDao.upsert(entity.copy(syncStatus = SyncStatus.ERROR))
        }
    }

    private suspend fun syncUpdate(entity: MeditationSessionEntity) {
        val remoteId = entity.remoteId ?: return syncCreate(entity.copy(syncStatus = SyncStatus.PENDING_CREATE))
        when (val result = apiClient.updateMeditationSession(remoteId, entity.toUpdateRequest())) {
            is AppResult.Success -> meditationSessionDao.upsert(entity.mergeSynced(result.value))
            is AppResult.Error -> meditationSessionDao.upsert(entity.copy(syncStatus = SyncStatus.ERROR))
        }
    }

    private suspend fun syncDelete(entity: MeditationSessionEntity) {
        val remoteId = entity.remoteId
        if (remoteId == null) {
            meditationSessionDao.delete(entity)
            return
        }
        when (apiClient.deleteMeditationSession(remoteId)) {
            is AppResult.Success -> meditationSessionDao.delete(entity)
            is AppResult.Error -> meditationSessionDao.upsert(entity.copy(syncStatus = SyncStatus.ERROR))
        }
    }

    private suspend fun retryError(entity: MeditationSessionEntity) {
        meditationSessionDao.upsert(
            entity.copy(
                syncStatus = MeditationSyncPolicy.statusForErrorRetry(
                    deletedLocally = entity.deletedLocally,
                    remoteId = entity.remoteId,
                ),
            ),
        )
    }

    private suspend fun mergeRemote(ownerUserId: String, remoteSessions: List<MeditationSessionResponse>) {
        remoteSessions.forEach { remote ->
            val existing = meditationSessionDao.getByRemoteId(remote.id)
            if (existing == null) {
                meditationSessionDao.upsert(remote.toEntity(ownerUserId))
                return@forEach
            }
            if (existing.syncStatus != SyncStatus.SYNCED) return@forEach
            if (remote.updatedAt.toString() >= existing.updatedAt) {
                meditationSessionDao.upsert(remote.toEntity(ownerUserId, existing.localId))
            }
        }
    }

    private suspend fun currentOwnerUserId(): String? = userPreferences.authData.first().userId

    private suspend fun requireOwnerUserId(): String =
        currentOwnerUserId() ?: error("Meditation action requires authorized user")
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
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun MeditationSessionEntity.toCreateRequest(): CreateMeditationSessionRequest =
    CreateMeditationSessionRequest(
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(endedAt),
        durationSeconds = durationSeconds,
        note = note,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

private fun MeditationSessionEntity.toUpdateRequest(): UpdateMeditationSessionRequest =
    UpdateMeditationSessionRequest(
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(endedAt),
        durationSeconds = durationSeconds,
        note = note,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

private fun MeditationSessionEntity.mergeSynced(remote: MeditationSessionResponse): MeditationSessionEntity =
    copy(
        remoteId = remote.id,
        startedAt = remote.startedAt.toString(),
        endedAt = remote.endedAt.toString(),
        durationSeconds = remote.durationSeconds,
        note = remote.note,
        syncStatus = SyncStatus.SYNCED,
        deletedLocally = false,
        createdAt = remote.createdAt.toString(),
        updatedAt = remote.updatedAt.toString(),
    )

private fun MeditationSessionResponse.toEntity(
    ownerUserId: String,
    localId: String = UUID.randomUUID().toString(),
): MeditationSessionEntity =
    MeditationSessionEntity(
        localId = localId,
        remoteId = id,
        ownerUserId = ownerUserId,
        startedAt = startedAt.toString(),
        endedAt = endedAt.toString(),
        durationSeconds = durationSeconds,
        note = note,
        syncStatus = SyncStatus.SYNCED,
        deletedLocally = false,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun nowIso(): String = Clock.System.now().toString()
