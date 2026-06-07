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
import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.core.datastore.CurrentUserProvider
import com.bibo.android.core.network.AuthResponse
import com.bibo.android.core.network.BiboApi
import com.bibo.android.core.network.CreateDiaryEntryRequest
import com.bibo.android.core.network.CreateMeditationSessionRequest
import com.bibo.android.core.network.DiaryEntryResponse
import com.bibo.android.core.network.MeditationSessionResponse
import com.bibo.android.core.network.ServerAvailabilityMonitor
import com.bibo.android.core.network.UpdateDiaryEntryRequest
import com.bibo.android.core.network.UpdateMeditationSessionRequest
import com.bibo.android.core.util.AppResult
import com.bibo.android.core.util.DomainError
import com.bibo.android.features.diary.data.DiaryRepositoryImpl
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val UserA = "user-a"
private const val UserB = "user-b"
private const val T0 = "2026-01-01T10:00:00Z"
private const val T1 = "2026-01-01T10:05:00Z"

class SyncManagerTest {
    @Test
    fun syncProcessesCreateUpdateDeleteInStoredOrder() = runBlocking {
        val fixture = Fixture(UserA)
        fixture.diaryDao.upsert(
            diaryEntity(
                ownerUserId = UserA,
                localId = "local-1",
                text = "updated",
                syncStatus = SyncStatus.PENDING_DELETE,
                deletedLocally = true,
            ),
        )
        fixture.manager.enqueueDiaryCreate(UserA, "local-1", createDiaryRequest("created"))
        fixture.manager.enqueueDiaryUpdate(UserA, "local-1", updateDiaryRequest("updated"))
        fixture.manager.enqueueDiaryDelete(UserA, "local-1")

        val synced = fixture.manager.syncCurrentUser()

        assertTrue(synced)
        assertEquals(listOf("create-diary:created", "update-diary:updated", "delete-diary:diary-1"), fixture.api.calls)
        assertNull(fixture.diaryDao.getByLocalId(UserA, "local-1"))
        assertTrue(fixture.operationDao.getOperations(UserA).isEmpty())
    }

    @Test
    fun failedOperationStaysQueuedAndRetriesLater() = runBlocking {
        val fixture = Fixture(UserA)
        fixture.api.failNextDiaryCreate = true
        fixture.diaryDao.upsert(diaryEntity(ownerUserId = UserA, localId = "local-1", text = "created"))
        fixture.manager.enqueueDiaryCreate(UserA, "local-1", createDiaryRequest("created"))

        assertFalse(fixture.manager.syncCurrentUser())
        assertEquals(listOf("create-diary:created"), fixture.api.calls)
        assertEquals(1, fixture.operationDao.getOperations(UserA).size)

        fixture.api.calls.clear()
        assertTrue(fixture.manager.syncCurrentUser())

        assertEquals(listOf("create-diary:created"), fixture.api.calls)
        assertTrue(fixture.operationDao.getOperations(UserA).isEmpty())
        assertEquals(SyncStatus.SYNCED, fixture.diaryDao.getByLocalId(UserA, "local-1")?.syncStatus)
    }

    @Test
    fun syncOnlyProcessesCurrentUsersOperations() = runBlocking {
        val fixture = Fixture(UserA)
        fixture.diaryDao.upsert(diaryEntity(ownerUserId = UserB, localId = "local-b", text = "other"))
        fixture.manager.enqueueDiaryCreate(UserB, "local-b", createDiaryRequest("other"))
        fixture.api.calls.clear()

        assertTrue(fixture.manager.syncCurrentUser())

        assertTrue(fixture.api.calls.none { it.startsWith("create-diary") })
        assertEquals(1, fixture.operationDao.getOperations(UserB).size)
    }
}

class DiaryRepositoryImplTest {
    @Test
    fun updateWithNoActualChangesDoesNotQueueOperation() = runBlocking {
        val diaryDao = FakeDiaryEntryDao()
        val queue = FakeSyncQueue()
        val repository = DiaryRepositoryImpl(diaryDao, FakeCurrentUserProvider(UserA), queue)
        diaryDao.upsert(
            diaryEntity(
                ownerUserId = UserA,
                localId = "local-1",
                remoteId = "remote-1",
                text = "same",
                mood = 1,
                syncStatus = SyncStatus.SYNCED,
            ),
        )

        repository.updateEntry("local-1", "same", 1)

        assertTrue(queue.calls.isEmpty())
        assertEquals(SyncStatus.SYNCED, diaryDao.getByLocalId(UserA, "local-1")?.syncStatus)
    }

    @Test
    fun deleteMarksLocalTombstoneAndQueuesDeleteInsteadOfDeletingImmediately() = runBlocking {
        val diaryDao = FakeDiaryEntryDao()
        val queue = FakeSyncQueue()
        val repository = DiaryRepositoryImpl(diaryDao, FakeCurrentUserProvider(UserA), queue)
        diaryDao.upsert(
            diaryEntity(
                ownerUserId = UserA,
                localId = "local-1",
                remoteId = "remote-1",
                text = "delete me",
                syncStatus = SyncStatus.SYNCED,
            ),
        )

        repository.deleteEntry("local-1")

        val stored = diaryDao.getByLocalId(UserA, "local-1")
        assertEquals(SyncStatus.PENDING_DELETE, stored?.syncStatus)
        assertTrue(stored?.deletedLocally == true)
        assertEquals(listOf("diary-delete:local-1"), queue.calls)
    }

    @Test
    fun getEntryByIdIsScopedToCurrentUser() = runBlocking {
        val diaryDao = FakeDiaryEntryDao()
        val repository = DiaryRepositoryImpl(diaryDao, FakeCurrentUserProvider(UserA), FakeSyncQueue())
        diaryDao.upsert(diaryEntity(ownerUserId = UserB, localId = "local-1", text = "other"))

        assertNull(repository.getEntryById("local-1"))
    }
}

private class Fixture(userId: String) {
    val api = FakeBiboApi()
    val diaryDao = FakeDiaryEntryDao()
    val meditationDao = FakeMeditationSessionDao()
    val operationDao = FakeSyncOperationDao()
    val scheduler = FakeSyncScheduler()
    val manager = SyncManager(
        diaryEntryDao = diaryDao,
        meditationSessionDao = meditationDao,
        syncOperationDao = operationDao,
        apiClient = api,
        currentUserProvider = FakeCurrentUserProvider(userId),
        json = Json { ignoreUnknownKeys = true },
        serverAvailabilityMonitor = ServerAvailabilityMonitor(api),
        syncScheduler = scheduler,
    )
}

private class FakeCurrentUserProvider(userId: String?) : CurrentUserProvider {
    override val authData: Flow<AuthLocalData> = flowOf(
        AuthLocalData(token = userId?.let { "token" }, userId = userId, email = "user@example.com"),
    )

    override suspend fun currentUserId(): String? = authDataValue.userId

    private val authDataValue = AuthLocalData(token = userId?.let { "token" }, userId = userId, email = "user@example.com")
}

private class FakeSyncScheduler : SyncScheduler {
    var scheduledCount = 0
    override fun schedule() {
        scheduledCount++
    }
}

private class FakeSyncQueue : SyncQueue {
    val calls = mutableListOf<String>()

    override suspend fun enqueueDiaryCreate(ownerUserId: String, localId: String, request: CreateDiaryEntryRequest) {
        calls += "diary-create:$localId"
    }

    override suspend fun enqueueDiaryUpdate(ownerUserId: String, localId: String, request: UpdateDiaryEntryRequest) {
        calls += "diary-update:$localId"
    }

    override suspend fun enqueueDiaryDelete(ownerUserId: String, localId: String) {
        calls += "diary-delete:$localId"
    }

    override suspend fun enqueueMeditationCreate(
        ownerUserId: String,
        localId: String,
        request: CreateMeditationSessionRequest,
    ) {
        calls += "meditation-create:$localId"
    }

    override suspend fun enqueueMeditationUpdate(
        ownerUserId: String,
        localId: String,
        request: UpdateMeditationSessionRequest,
    ) {
        calls += "meditation-update:$localId"
    }

    override suspend fun enqueueMeditationDelete(ownerUserId: String, localId: String) {
        calls += "meditation-delete:$localId"
    }

    override suspend fun syncCurrentUser(): Boolean = true
}

private class FakeBiboApi : BiboApi {
    val calls = mutableListOf<String>()
    var failNextDiaryCreate = false
    private var nextDiaryId = 1
    private var nextMeditationId = 1
    private val diaryEntries = linkedMapOf<String, DiaryEntryResponse>()
    private val meditationSessions = linkedMapOf<String, MeditationSessionResponse>()

    override suspend fun checkHealth(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun getDiaryEntries(): AppResult<List<DiaryEntryResponse>> =
        AppResult.Success(diaryEntries.values.toList())

    override suspend fun createDiaryEntry(request: CreateDiaryEntryRequest): AppResult<DiaryEntryResponse> {
        calls += "create-diary:${request.text}"
        if (failNextDiaryCreate) {
            failNextDiaryCreate = false
            return AppResult.Error(DomainError("NETWORK_ERROR", "offline"))
        }
        val id = "diary-${nextDiaryId++}"
        val response = DiaryEntryResponse(
            id = id,
            text = request.text,
            mood = request.mood,
            dateTime = request.dateTime,
            createdAt = request.createdAt,
            updatedAt = request.updatedAt,
        )
        diaryEntries[id] = response
        return AppResult.Success(response)
    }

    override suspend fun updateDiaryEntry(
        remoteId: String,
        request: UpdateDiaryEntryRequest,
    ): AppResult<DiaryEntryResponse> {
        calls += "update-diary:${request.text}"
        val response = DiaryEntryResponse(
            id = remoteId,
            text = request.text,
            mood = request.mood,
            dateTime = request.dateTime,
            createdAt = request.createdAt,
            updatedAt = request.updatedAt,
        )
        diaryEntries[remoteId] = response
        return AppResult.Success(response)
    }

    override suspend fun deleteDiaryEntry(remoteId: String): AppResult<Unit> {
        calls += "delete-diary:$remoteId"
        diaryEntries.remove(remoteId)
        return AppResult.Success(Unit)
    }

    override suspend fun getMeditationSessions(): AppResult<List<MeditationSessionResponse>> =
        AppResult.Success(meditationSessions.values.toList())

    override suspend fun createMeditationSession(
        request: CreateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse> {
        calls += "create-meditation:${request.note}"
        val id = "meditation-${nextMeditationId++}"
        val response = MeditationSessionResponse(
            id = id,
            startedAt = request.startedAt,
            endedAt = request.endedAt,
            durationSeconds = request.durationSeconds,
            note = request.note,
            createdAt = request.createdAt,
            updatedAt = request.updatedAt,
        )
        meditationSessions[id] = response
        return AppResult.Success(response)
    }

    override suspend fun updateMeditationSession(
        remoteId: String,
        request: UpdateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse> {
        calls += "update-meditation:${request.note}"
        val response = MeditationSessionResponse(
            id = remoteId,
            startedAt = request.startedAt,
            endedAt = request.endedAt,
            durationSeconds = request.durationSeconds,
            note = request.note,
            createdAt = request.createdAt,
            updatedAt = request.updatedAt,
        )
        meditationSessions[remoteId] = response
        return AppResult.Success(response)
    }

    override suspend fun deleteMeditationSession(remoteId: String): AppResult<Unit> {
        calls += "delete-meditation:$remoteId"
        meditationSessions.remove(remoteId)
        return AppResult.Success(Unit)
    }
}

private class FakeSyncOperationDao : SyncOperationDao {
    private val operations = mutableListOf<SyncOperationEntity>()

    override suspend fun getOperations(ownerUserId: String): List<SyncOperationEntity> =
        operations.filter { it.ownerUserId == ownerUserId }

    override suspend fun getOperationsForEntity(
        ownerUserId: String,
        entityType: SyncEntityType,
        entityLocalId: String,
    ): List<SyncOperationEntity> =
        getOperations(ownerUserId).filter {
            it.entityType == entityType && it.entityLocalId == entityLocalId
        }

    override suspend fun countOperationsForEntity(
        ownerUserId: String,
        entityType: SyncEntityType,
        entityLocalId: String,
    ): Int = getOperationsForEntity(ownerUserId, entityType, entityLocalId).size

    override suspend fun insert(operation: SyncOperationEntity) {
        operations.removeAll { it.id == operation.id }
        operations += operation
    }

    override suspend fun delete(operation: SyncOperationEntity) {
        operations.removeAll { it.id == operation.id }
    }
}

private class FakeDiaryEntryDao : DiaryEntryDao {
    private val entries = linkedMapOf<String, DiaryEntryEntity>()

    override fun observeActiveEntries(ownerUserId: String): Flow<List<DiaryEntryEntity>> =
        flowOf(entries.values.filter { it.ownerUserId == ownerUserId && !it.deletedLocally })

    override suspend fun getByLocalId(ownerUserId: String, localId: String): DiaryEntryEntity? =
        entries[localId]?.takeIf { it.ownerUserId == ownerUserId }

    override suspend fun getByRemoteId(ownerUserId: String, remoteId: String): DiaryEntryEntity? =
        entries.values.firstOrNull { it.ownerUserId == ownerUserId && it.remoteId == remoteId }

    override suspend fun getSyncedRemoteEntries(ownerUserId: String): List<DiaryEntryEntity> =
        entries.values.filter { it.ownerUserId == ownerUserId && it.remoteId != null && it.syncStatus == SyncStatus.SYNCED }

    override suspend fun getPendingEntries(ownerUserId: String): List<DiaryEntryEntity> =
        entries.values.filter { it.ownerUserId == ownerUserId && it.syncStatus != SyncStatus.SYNCED }

    override suspend fun getAllEntries(ownerUserId: String): List<DiaryEntryEntity> =
        entries.values.filter { it.ownerUserId == ownerUserId }

    override suspend fun upsert(entity: DiaryEntryEntity) {
        entries[entity.localId] = entity
    }

    override suspend fun update(entity: DiaryEntryEntity) {
        entries[entity.localId] = entity
    }

    override suspend fun delete(entity: DiaryEntryEntity) {
        entries.remove(entity.localId)
    }
}

private class FakeMeditationSessionDao : MeditationSessionDao {
    private val sessions = linkedMapOf<String, MeditationSessionEntity>()

    override fun observeActiveSessions(ownerUserId: String): Flow<List<MeditationSessionEntity>> =
        flowOf(sessions.values.filter { it.ownerUserId == ownerUserId && !it.deletedLocally })

    override suspend fun getByLocalId(ownerUserId: String, localId: String): MeditationSessionEntity? =
        sessions[localId]?.takeIf { it.ownerUserId == ownerUserId }

    override suspend fun getByRemoteId(ownerUserId: String, remoteId: String): MeditationSessionEntity? =
        sessions.values.firstOrNull { it.ownerUserId == ownerUserId && it.remoteId == remoteId }

    override suspend fun getSyncedRemoteSessions(ownerUserId: String): List<MeditationSessionEntity> =
        sessions.values.filter { it.ownerUserId == ownerUserId && it.remoteId != null && it.syncStatus == SyncStatus.SYNCED }

    override suspend fun getPendingSessions(ownerUserId: String): List<MeditationSessionEntity> =
        sessions.values.filter { it.ownerUserId == ownerUserId && it.syncStatus != SyncStatus.SYNCED }

    override suspend fun getAllSessions(ownerUserId: String): List<MeditationSessionEntity> =
        sessions.values.filter { it.ownerUserId == ownerUserId }

    override suspend fun upsert(entity: MeditationSessionEntity) {
        sessions[entity.localId] = entity
    }

    override suspend fun update(entity: MeditationSessionEntity) {
        sessions[entity.localId] = entity
    }

    override suspend fun delete(entity: MeditationSessionEntity) {
        sessions.remove(entity.localId)
    }
}

private fun diaryEntity(
    ownerUserId: String,
    localId: String = UUID.randomUUID().toString(),
    remoteId: String? = null,
    text: String? = "text",
    mood: Int? = null,
    syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    deletedLocally: Boolean = false,
): DiaryEntryEntity =
    DiaryEntryEntity(
        localId = localId,
        remoteId = remoteId,
        ownerUserId = ownerUserId,
        text = text,
        mood = mood,
        dateTime = T0,
        syncStatus = syncStatus,
        deletedLocally = deletedLocally,
        createdAt = T0,
        updatedAt = T1,
    )

private fun createDiaryRequest(text: String): CreateDiaryEntryRequest =
    CreateDiaryEntryRequest(
        text = text,
        mood = null,
        dateTime = Instant.parse(T0),
        createdAt = Instant.parse(T0),
        updatedAt = Instant.parse(T0),
    )

private fun updateDiaryRequest(text: String): UpdateDiaryEntryRequest =
    UpdateDiaryEntryRequest(
        text = text,
        mood = null,
        dateTime = Instant.parse(T0),
        createdAt = Instant.parse(T0),
        updatedAt = Instant.parse(T1),
    )
