@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bibo.android.core.sync

import com.bibo.android.core.database.DiaryEntryDao
import com.bibo.android.core.database.DiaryEntryEntity
import com.bibo.android.core.database.MeditationSessionDao
import com.bibo.android.core.database.MeditationSessionEntity
import com.bibo.android.core.database.SyncEntityType
import com.bibo.android.core.database.SyncOperationDao
import com.bibo.android.core.database.SyncOperationEntity
import com.bibo.android.core.database.SyncOperationType
import com.bibo.android.core.database.SyncStatus
import com.bibo.android.core.datastore.CurrentUserProvider
import com.bibo.android.core.network.BiboApi
import com.bibo.android.core.network.CreateDiaryEntryRequest
import com.bibo.android.core.network.CreateMeditationSessionRequest
import com.bibo.android.core.network.DiaryEntryResponse
import com.bibo.android.core.network.MeditationSessionResponse
import com.bibo.android.core.network.ServerAvailabilityMonitor
import com.bibo.android.core.network.UpdateDiaryEntryRequest
import com.bibo.android.core.network.UpdateMeditationSessionRequest
import com.bibo.android.core.util.AppResult
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class SyncManager(
    private val diaryEntryDao: DiaryEntryDao,
    private val meditationSessionDao: MeditationSessionDao,
    private val syncOperationDao: SyncOperationDao,
    private val apiClient: BiboApi,
    private val currentUserProvider: CurrentUserProvider,
    private val json: Json,
    private val serverAvailabilityMonitor: ServerAvailabilityMonitor,
    private val syncScheduler: SyncScheduler,
) : SyncQueue {
    override suspend fun enqueueDiaryCreate(ownerUserId: String, localId: String, request: CreateDiaryEntryRequest) {
        enqueue(ownerUserId, SyncEntityType.DIARY_ENTRY, localId, SyncOperationType.CREATE, json.encodeToString(request))
    }

    override suspend fun enqueueDiaryUpdate(ownerUserId: String, localId: String, request: UpdateDiaryEntryRequest) {
        enqueue(ownerUserId, SyncEntityType.DIARY_ENTRY, localId, SyncOperationType.UPDATE, json.encodeToString(request))
    }

    override suspend fun enqueueDiaryDelete(ownerUserId: String, localId: String) {
        enqueue(ownerUserId, SyncEntityType.DIARY_ENTRY, localId, SyncOperationType.DELETE, null)
    }

    override suspend fun enqueueMeditationCreate(
        ownerUserId: String,
        localId: String,
        request: CreateMeditationSessionRequest,
    ) {
        enqueue(
            ownerUserId = ownerUserId,
            entityType = SyncEntityType.MEDITATION_SESSION,
            entityLocalId = localId,
            operationType = SyncOperationType.CREATE,
            payload = json.encodeToString(request),
        )
    }

    override suspend fun enqueueMeditationUpdate(
        ownerUserId: String,
        localId: String,
        request: UpdateMeditationSessionRequest,
    ) {
        enqueue(
            ownerUserId = ownerUserId,
            entityType = SyncEntityType.MEDITATION_SESSION,
            entityLocalId = localId,
            operationType = SyncOperationType.UPDATE,
            payload = json.encodeToString(request),
        )
    }

    override suspend fun enqueueMeditationDelete(ownerUserId: String, localId: String) {
        enqueue(ownerUserId, SyncEntityType.MEDITATION_SESSION, localId, SyncOperationType.DELETE, null)
    }

    override suspend fun syncCurrentUser(): Boolean {
        val ownerUserId = currentUserProvider.currentUserId() ?: return true
        if (apiClient.checkHealth() !is AppResult.Success) {
            serverAvailabilityMonitor.markUnavailable()
            return false
        }
        serverAvailabilityMonitor.markAvailable()
        enqueueMissingOperations(ownerUserId)
        if (!processQueue(ownerUserId)) return false
        return refreshCaches(ownerUserId)
    }

    private suspend fun enqueue(
        ownerUserId: String,
        entityType: SyncEntityType,
        entityLocalId: String,
        operationType: SyncOperationType,
        payload: String?,
    ) {
        syncOperationDao.insert(
            SyncOperationEntity(
                id = UUID.randomUUID().toString(),
                ownerUserId = ownerUserId,
                entityType = entityType,
                entityLocalId = entityLocalId,
                operationType = operationType,
                payload = payload,
                createdAt = Clock.System.now().toString(),
            ),
        )
        syncScheduler.schedule()
    }

    private suspend fun enqueueMissingOperations(ownerUserId: String) {
        diaryEntryDao.getPendingEntries(ownerUserId)
            .filter { entry ->
                syncOperationDao.countOperationsForEntity(
                    ownerUserId,
                    SyncEntityType.DIARY_ENTRY,
                    entry.localId,
                ) == 0
            }
            .forEach { entry -> enqueueMissingDiaryOperations(ownerUserId, entry) }

        meditationSessionDao.getPendingSessions(ownerUserId)
            .filter { session ->
                syncOperationDao.countOperationsForEntity(
                    ownerUserId,
                    SyncEntityType.MEDITATION_SESSION,
                    session.localId,
                ) == 0
            }
            .forEach { session -> enqueueMissingMeditationOperations(ownerUserId, session) }
    }

    private suspend fun enqueueMissingDiaryOperations(ownerUserId: String, entry: DiaryEntryEntity) {
        when (entry.syncStatus) {
            SyncStatus.PENDING_CREATE -> enqueueDiaryCreate(ownerUserId, entry.localId, entry.toCreateRequest())
            SyncStatus.PENDING_UPDATE -> enqueueDiaryUpdate(ownerUserId, entry.localId, entry.toUpdateRequest())
            SyncStatus.PENDING_DELETE -> enqueueDiaryDelete(ownerUserId, entry.localId)
            SyncStatus.SYNCED -> Unit
        }
    }

    private suspend fun enqueueMissingMeditationOperations(ownerUserId: String, session: MeditationSessionEntity) {
        when (session.syncStatus) {
            SyncStatus.PENDING_CREATE -> enqueueMeditationCreate(ownerUserId, session.localId, session.toCreateRequest())
            SyncStatus.PENDING_UPDATE -> enqueueMeditationUpdate(ownerUserId, session.localId, session.toUpdateRequest())
            SyncStatus.PENDING_DELETE -> enqueueMeditationDelete(ownerUserId, session.localId)
            SyncStatus.SYNCED -> Unit
        }
    }

    private suspend fun processQueue(ownerUserId: String): Boolean {
        for (operation in syncOperationDao.getOperations(ownerUserId)) {
            val synced = when (operation.entityType) {
                SyncEntityType.DIARY_ENTRY -> processDiaryOperation(operation)
                SyncEntityType.MEDITATION_SESSION -> processMeditationOperation(operation)
            }
            if (!synced) {
                serverAvailabilityMonitor.markUnavailable()
                return false
            }
            serverAvailabilityMonitor.markAvailable()
        }
        return true
    }

    private suspend fun processDiaryOperation(operation: SyncOperationEntity): Boolean {
        val entry = diaryEntryDao.getByLocalId(operation.ownerUserId, operation.entityLocalId)
        if (entry == null) {
            syncOperationDao.delete(operation)
            return true
        }
        return when (operation.operationType) {
            SyncOperationType.CREATE -> processDiaryCreate(operation, entry)
            SyncOperationType.UPDATE -> processDiaryUpdate(operation, entry)
            SyncOperationType.DELETE -> processDiaryDelete(operation, entry)
        }
    }

    private suspend fun processDiaryCreate(
        operation: SyncOperationEntity,
        entry: DiaryEntryEntity,
    ): Boolean {
        val request = operation.payload?.let { json.decodeFromString<CreateDiaryEntryRequest>(it) } ?: return false
        return when (val result = apiClient.createDiaryEntry(request)) {
            is AppResult.Success -> {
                syncOperationDao.delete(operation)
                val next = nextOperation(operation)
                if (next == null) {
                    diaryEntryDao.upsert(result.value.toEntity(operation.ownerUserId, entry.localId))
                } else {
                    diaryEntryDao.update(
                        entry.copy(
                            remoteId = result.value.id,
                            syncStatus = next.toSyncStatus(),
                        ),
                    )
                }
                true
            }
            is AppResult.Error -> false
        }
    }

    private suspend fun processDiaryUpdate(
        operation: SyncOperationEntity,
        entry: DiaryEntryEntity,
    ): Boolean {
        val remoteId = entry.remoteId ?: return false
        val request = operation.payload?.let { json.decodeFromString<UpdateDiaryEntryRequest>(it) } ?: return false
        return when (val result = apiClient.updateDiaryEntry(remoteId, request)) {
            is AppResult.Success -> {
                syncOperationDao.delete(operation)
                val next = nextOperation(operation)
                if (next == null) {
                    diaryEntryDao.upsert(result.value.toEntity(operation.ownerUserId, entry.localId))
                } else {
                    diaryEntryDao.update(
                        entry.copy(
                            remoteId = result.value.id,
                            syncStatus = next.toSyncStatus(),
                        ),
                    )
                }
                true
            }
            is AppResult.Error -> false
        }
    }

    private suspend fun processDiaryDelete(
        operation: SyncOperationEntity,
        entry: DiaryEntryEntity,
    ): Boolean {
        val remoteId = entry.remoteId
        if (remoteId == null) {
            syncOperationDao.delete(operation)
            diaryEntryDao.delete(entry)
            return true
        }
        return when (apiClient.deleteDiaryEntry(remoteId)) {
            is AppResult.Success -> {
                syncOperationDao.delete(operation)
                diaryEntryDao.delete(entry)
                true
            }
            is AppResult.Error -> false
        }
    }

    private suspend fun processMeditationOperation(operation: SyncOperationEntity): Boolean {
        val session = meditationSessionDao.getByLocalId(operation.ownerUserId, operation.entityLocalId)
        if (session == null) {
            syncOperationDao.delete(operation)
            return true
        }
        return when (operation.operationType) {
            SyncOperationType.CREATE -> processMeditationCreate(operation, session)
            SyncOperationType.UPDATE -> processMeditationUpdate(operation, session)
            SyncOperationType.DELETE -> processMeditationDelete(operation, session)
        }
    }

    private suspend fun processMeditationCreate(
        operation: SyncOperationEntity,
        session: MeditationSessionEntity,
    ): Boolean {
        val request = operation.payload?.let { json.decodeFromString<CreateMeditationSessionRequest>(it) } ?: return false
        return when (val result = apiClient.createMeditationSession(request)) {
            is AppResult.Success -> {
                syncOperationDao.delete(operation)
                val next = nextOperation(operation)
                if (next == null) {
                    meditationSessionDao.upsert(result.value.toEntity(operation.ownerUserId, session.localId))
                } else {
                    meditationSessionDao.update(
                        session.copy(
                            remoteId = result.value.id,
                            syncStatus = next.toSyncStatus(),
                        ),
                    )
                }
                true
            }
            is AppResult.Error -> false
        }
    }

    private suspend fun processMeditationUpdate(
        operation: SyncOperationEntity,
        session: MeditationSessionEntity,
    ): Boolean {
        val remoteId = session.remoteId ?: return false
        val request = operation.payload?.let { json.decodeFromString<UpdateMeditationSessionRequest>(it) } ?: return false
        return when (val result = apiClient.updateMeditationSession(remoteId, request)) {
            is AppResult.Success -> {
                syncOperationDao.delete(operation)
                val next = nextOperation(operation)
                if (next == null) {
                    meditationSessionDao.upsert(result.value.toEntity(operation.ownerUserId, session.localId))
                } else {
                    meditationSessionDao.update(
                        session.copy(
                            remoteId = result.value.id,
                            syncStatus = next.toSyncStatus(),
                        ),
                    )
                }
                true
            }
            is AppResult.Error -> false
        }
    }

    private suspend fun processMeditationDelete(
        operation: SyncOperationEntity,
        session: MeditationSessionEntity,
    ): Boolean {
        val remoteId = session.remoteId
        if (remoteId == null) {
            syncOperationDao.delete(operation)
            meditationSessionDao.delete(session)
            return true
        }
        return when (apiClient.deleteMeditationSession(remoteId)) {
            is AppResult.Success -> {
                syncOperationDao.delete(operation)
                meditationSessionDao.delete(session)
                true
            }
            is AppResult.Error -> false
        }
    }

    private suspend fun nextOperation(operation: SyncOperationEntity): SyncOperationEntity? =
        syncOperationDao.getOperationsForEntity(
            ownerUserId = operation.ownerUserId,
            entityType = operation.entityType,
            entityLocalId = operation.entityLocalId,
        ).firstOrNull { it.id != operation.id }

    private suspend fun refreshCaches(ownerUserId: String): Boolean {
        val diaryResult = apiClient.getDiaryEntries()
        if (diaryResult !is AppResult.Success) {
            serverAvailabilityMonitor.markUnavailable()
            return false
        }
        replaceDiaryCache(ownerUserId, diaryResult.value)

        val meditationResult = apiClient.getMeditationSessions()
        if (meditationResult !is AppResult.Success) {
            serverAvailabilityMonitor.markUnavailable()
            return false
        }
        replaceMeditationCache(ownerUserId, meditationResult.value)
        serverAvailabilityMonitor.markAvailable()
        return true
    }

    private suspend fun replaceDiaryCache(ownerUserId: String, remoteEntries: List<DiaryEntryResponse>) {
        val remoteIds = remoteEntries.mapTo(mutableSetOf()) { it.id }
        remoteEntries.forEach { remote ->
            val existing = diaryEntryDao.getByRemoteId(ownerUserId, remote.id)
            if (existing != null && existing.syncStatus != SyncStatus.SYNCED) return@forEach
            diaryEntryDao.upsert(remote.toEntity(ownerUserId, existing?.localId ?: UUID.randomUUID().toString()))
        }
        diaryEntryDao.getAllEntries(ownerUserId)
            .filter { entity -> entity.syncStatus == SyncStatus.SYNCED && entity.remoteId !in remoteIds }
            .forEach { entity -> diaryEntryDao.delete(entity) }
    }

    private suspend fun replaceMeditationCache(ownerUserId: String, remoteSessions: List<MeditationSessionResponse>) {
        val remoteIds = remoteSessions.mapTo(mutableSetOf()) { it.id }
        remoteSessions.forEach { remote ->
            val existing = meditationSessionDao.getByRemoteId(ownerUserId, remote.id)
            if (existing != null && existing.syncStatus != SyncStatus.SYNCED) return@forEach
            meditationSessionDao.upsert(remote.toEntity(ownerUserId, existing?.localId ?: UUID.randomUUID().toString()))
        }
        meditationSessionDao.getAllSessions(ownerUserId)
            .filter { entity -> entity.syncStatus == SyncStatus.SYNCED && entity.remoteId !in remoteIds }
            .forEach { entity -> meditationSessionDao.delete(entity) }
    }
}

private fun SyncOperationEntity.toSyncStatus(): SyncStatus = when (operationType) {
    SyncOperationType.CREATE -> SyncStatus.PENDING_CREATE
    SyncOperationType.UPDATE -> SyncStatus.PENDING_UPDATE
    SyncOperationType.DELETE -> SyncStatus.PENDING_DELETE
}

private fun DiaryEntryEntity.toCreateRequest(): CreateDiaryEntryRequest =
    CreateDiaryEntryRequest(
        text = text,
        mood = mood,
        dateTime = Instant.parse(dateTime),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

private fun DiaryEntryEntity.toUpdateRequest(): UpdateDiaryEntryRequest =
    UpdateDiaryEntryRequest(
        text = text,
        mood = mood,
        dateTime = Instant.parse(dateTime),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

private fun DiaryEntryResponse.toEntity(
    ownerUserId: String,
    localId: String,
): DiaryEntryEntity =
    DiaryEntryEntity(
        localId = localId,
        remoteId = id,
        ownerUserId = ownerUserId,
        text = text,
        mood = mood,
        dateTime = dateTime.toString(),
        syncStatus = SyncStatus.SYNCED,
        deletedLocally = false,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

private fun MeditationSessionEntity.toCreateRequest(): CreateMeditationSessionRequest =
    CreateMeditationSessionRequest(
        startedAt = Instant.parse(startedAt),
        endedAt = Instant.parse(endedAt),
        durationSeconds = durationSeconds.coerceAtLeast(0),
        note = note,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
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

private fun MeditationSessionResponse.toEntity(
    ownerUserId: String,
    localId: String,
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
