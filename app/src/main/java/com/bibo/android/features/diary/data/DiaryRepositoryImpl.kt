@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bibo.android.features.diary.data

import com.bibo.android.core.database.DiaryEntryDao
import com.bibo.android.core.database.DiaryEntryEntity
import com.bibo.android.core.database.SyncStatus
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.network.ApiClient
import com.bibo.android.core.network.CreateDiaryEntryRequest
import com.bibo.android.core.network.DiaryEntryResponse
import com.bibo.android.core.network.UpdateDiaryEntryRequest
import com.bibo.android.core.util.AppResult
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.domain.DiaryRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlin.time.Clock

class DiaryRepositoryImpl(
    private val diaryEntryDao: DiaryEntryDao,
    private val apiClient: ApiClient,
    private val userPreferences: UserPreferences,
) : DiaryRepository {
    override fun observeEntries(): Flow<List<DiaryEntry>> =
        userPreferences.authData.flatMapLatest { auth ->
            val ownerUserId = auth.userId
            if (ownerUserId == null) {
                flowOf(emptyList())
            } else {
                diaryEntryDao.observeActiveEntries(ownerUserId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override fun observeJournalItems(): Flow<List<DiaryEntry>> = observeEntries()

    override suspend fun getEntryById(localId: String): DiaryEntry? =
        diaryEntryDao.getByLocalId(localId)?.takeUnless { it.deletedLocally }?.toDomain()

    override suspend fun createEntry(text: String?, mood: Int?) {
        val ownerUserId = requireOwnerUserId()
        val now = nowIso()
        diaryEntryDao.upsert(
            DiaryEntryEntity(
                localId = UUID.randomUUID().toString(),
                remoteId = null,
                ownerUserId = ownerUserId,
                text = text.cleaned(),
                mood = mood,
                dateTime = now,
                syncStatus = SyncStatus.PENDING_CREATE,
                deletedLocally = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        syncPendingChanges()
    }

    override suspend fun updateEntry(localId: String, text: String?, mood: Int?) {
        val existing = diaryEntryDao.getByLocalId(localId) ?: return
        val nextStatus = DiarySyncPolicy.statusAfterLocalUpdate(existing.syncStatus)
        diaryEntryDao.update(
            existing.copy(
                text = text.cleaned(),
                mood = mood,
                syncStatus = nextStatus,
                updatedAt = nowIso(),
            ),
        )
        syncPendingChanges()
    }

    override suspend fun deleteEntry(localId: String) {
        val existing = diaryEntryDao.getByLocalId(localId) ?: return
        if (existing.remoteId == null && existing.syncStatus == SyncStatus.PENDING_CREATE) {
            diaryEntryDao.delete(existing)
            return
        }

        diaryEntryDao.update(
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

        diaryEntryDao.getPendingEntries(ownerUserId).forEach { entity ->
            when (entity.syncStatus) {
                SyncStatus.PENDING_CREATE -> syncCreate(entity)
                SyncStatus.PENDING_UPDATE -> syncUpdate(entity)
                SyncStatus.PENDING_DELETE -> syncDelete(entity)
                SyncStatus.ERROR -> retryError(entity)
                SyncStatus.SYNCED -> Unit
            }
        }

        when (val remoteResult = apiClient.getDiaryEntries()) {
            is AppResult.Error -> Unit
            is AppResult.Success -> mergeRemote(ownerUserId, remoteResult.value)
        }
    }

    private suspend fun syncCreate(entity: DiaryEntryEntity) {
        val result = apiClient.createDiaryEntry(entity.toCreateRequest())
        when (result) {
            is AppResult.Success -> diaryEntryDao.upsert(entity.mergeSynced(result.value))
            is AppResult.Error -> diaryEntryDao.upsert(entity.copy(syncStatus = SyncStatus.ERROR))
        }
    }

    private suspend fun syncUpdate(entity: DiaryEntryEntity) {
        val remoteId = entity.remoteId ?: return syncCreate(entity.copy(syncStatus = SyncStatus.PENDING_CREATE))
        val result = apiClient.updateDiaryEntry(remoteId, entity.toUpdateRequest())
        when (result) {
            is AppResult.Success -> diaryEntryDao.upsert(entity.mergeSynced(result.value))
            is AppResult.Error -> diaryEntryDao.upsert(entity.copy(syncStatus = SyncStatus.ERROR))
        }
    }

    private suspend fun syncDelete(entity: DiaryEntryEntity) {
        val remoteId = entity.remoteId
        if (remoteId == null) {
            diaryEntryDao.delete(entity)
            return
        }
        when (apiClient.deleteDiaryEntry(remoteId)) {
            is AppResult.Success -> diaryEntryDao.delete(entity)
            is AppResult.Error -> diaryEntryDao.upsert(entity.copy(syncStatus = SyncStatus.ERROR))
        }
    }

    private suspend fun retryError(entity: DiaryEntryEntity) {
        val next = entity.copy(
            syncStatus = DiarySyncPolicy.statusForErrorRetry(
                deletedLocally = entity.deletedLocally,
                remoteId = entity.remoteId,
            ),
        )
        diaryEntryDao.upsert(next)
    }

    private suspend fun mergeRemote(ownerUserId: String, remoteEntries: List<DiaryEntryResponse>) {
        remoteEntries.forEach { remote ->
            val existing = diaryEntryDao.getByRemoteId(remote.id)
            if (existing == null) {
                diaryEntryDao.upsert(remote.toEntity(ownerUserId))
                return@forEach
            }
            if (existing.syncStatus != SyncStatus.SYNCED) return@forEach
            if (remote.updatedAt.toString() >= existing.updatedAt) {
                diaryEntryDao.upsert(remote.toEntity(ownerUserId, existing.localId))
            }
        }
    }

    private suspend fun currentOwnerUserId(): String? = userPreferences.authData.first().userId

    private suspend fun requireOwnerUserId(): String =
        currentOwnerUserId() ?: error("Diary action requires authorized user")
}

private fun DiaryEntryEntity.toDomain(): DiaryEntry =
    DiaryEntry(
        localId = localId,
        remoteId = remoteId,
        ownerUserId = ownerUserId,
        text = text,
        mood = mood,
        dateTime = dateTime,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

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

private fun DiaryEntryEntity.mergeSynced(remote: DiaryEntryResponse): DiaryEntryEntity =
    copy(
        remoteId = remote.id,
        text = remote.text,
        mood = remote.mood,
        dateTime = remote.dateTime.toString(),
        syncStatus = SyncStatus.SYNCED,
        deletedLocally = false,
        createdAt = remote.createdAt.toString(),
        updatedAt = remote.updatedAt.toString(),
    )

private fun DiaryEntryResponse.toEntity(
    ownerUserId: String,
    localId: String = UUID.randomUUID().toString(),
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

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun nowIso(): String = Clock.System.now().toString()
