@file:Suppress("DEPRECATION")
@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bibo.android.features.diary.data

import com.bibo.android.core.database.DiaryEntryDao
import com.bibo.android.core.database.DiaryEntryEntity
import com.bibo.android.core.database.SyncStatus
import com.bibo.android.core.datastore.CurrentUserProvider
import com.bibo.android.core.network.CreateDiaryEntryRequest
import com.bibo.android.core.network.UpdateDiaryEntryRequest
import com.bibo.android.core.sync.SyncQueue
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.domain.DiaryRepository
import com.bibo.android.features.diary.domain.PendingSyncOperation
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlin.time.Clock

class DiaryRepositoryImpl(
    private val diaryEntryDao: DiaryEntryDao,
    private val currentUserProvider: CurrentUserProvider,
    private val syncQueue: SyncQueue,
) : DiaryRepository {
    override fun observeEntries(): Flow<List<DiaryEntry>> =
        currentUserProvider.authData.flatMapLatest { auth ->
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

    override suspend fun getEntryById(localId: String): DiaryEntry? {
        val ownerUserId = currentOwnerUserId() ?: return null
        return diaryEntryDao.getByLocalId(ownerUserId, localId)
            ?.takeUnless { it.deletedLocally }
            ?.takeIf { it.remoteId != null || it.syncStatus != SyncStatus.SYNCED }
            ?.toDomain()
    }

    override suspend fun createEntry(text: String?, mood: Int?) {
        val ownerUserId = currentOwnerUserId() ?: return
        val now = nowIso()
        val localId = UUID.randomUUID().toString()
        val cleanedText = text.cleaned()
        val request = CreateDiaryEntryRequest(
            text = cleanedText,
            mood = mood,
            dateTime = Instant.parse(now),
            createdAt = Instant.parse(now),
            updatedAt = Instant.parse(now),
        )
        diaryEntryDao.upsert(
            DiaryEntryEntity(
                localId = localId,
                remoteId = null,
                ownerUserId = ownerUserId,
                text = cleanedText,
                mood = mood,
                dateTime = now,
                syncStatus = SyncStatus.PENDING_CREATE,
                deletedLocally = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        syncQueue.enqueueDiaryCreate(ownerUserId, localId, request)
    }

    override suspend fun updateEntry(localId: String, text: String?, mood: Int?) {
        val ownerUserId = currentOwnerUserId() ?: return
        val existing = diaryEntryDao.getByLocalId(ownerUserId, localId)
            ?.takeUnless { it.deletedLocally }
            ?: return
        val cleanedText = text.cleaned()
        if (existing.text == cleanedText && existing.mood == mood) return

        val updatedAt = nowIso()
        val updated = existing.copy(
            text = cleanedText,
            mood = mood,
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = updatedAt,
        )
        diaryEntryDao.update(updated)
        syncQueue.enqueueDiaryUpdate(ownerUserId, localId, updated.toUpdateRequest())
    }

    override suspend fun deleteEntry(localId: String) {
        val ownerUserId = currentOwnerUserId() ?: return
        val existing = diaryEntryDao.getByLocalId(ownerUserId, localId)
            ?.takeUnless { it.deletedLocally }
            ?: return
        diaryEntryDao.update(
            existing.copy(
                syncStatus = SyncStatus.PENDING_DELETE,
                deletedLocally = true,
            ),
        )
        syncQueue.enqueueDiaryDelete(ownerUserId, localId)
    }

    override suspend fun refreshFromServer() {
        syncQueue.syncCurrentUser()
    }

    private suspend fun currentOwnerUserId(): String? = currentUserProvider.currentUserId()
}

private fun DiaryEntryEntity.toDomain(): DiaryEntry =
    DiaryEntry(
        localId = localId,
        remoteId = remoteId,
        ownerUserId = ownerUserId,
        text = text,
        mood = mood,
        dateTime = dateTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pendingOperation = syncStatus.toPendingOperation(),
    )

private fun DiaryEntryEntity.toUpdateRequest(): UpdateDiaryEntryRequest =
    UpdateDiaryEntryRequest(
        text = text,
        mood = mood,
        dateTime = Instant.parse(dateTime),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
    )

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun nowIso(): String = Clock.System.now().toString()

private fun SyncStatus.toPendingOperation(): PendingSyncOperation? = when (this) {
    SyncStatus.PENDING_CREATE -> PendingSyncOperation.Create
    SyncStatus.PENDING_UPDATE -> PendingSyncOperation.Update
    SyncStatus.PENDING_DELETE -> PendingSyncOperation.Delete
    SyncStatus.SYNCED -> null
}
