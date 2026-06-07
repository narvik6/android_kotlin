package com.bibo.android.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationSessionDao {
    @Query(
        """
        SELECT * FROM meditation_sessions
        WHERE ownerUserId = :ownerUserId
        AND deletedLocally = 0
        AND (remoteId IS NOT NULL OR syncStatus != 'SYNCED')
        ORDER BY startedAt DESC
        """,
    )
    fun observeActiveSessions(ownerUserId: String): Flow<List<MeditationSessionEntity>>

    @Query("SELECT * FROM meditation_sessions WHERE ownerUserId = :ownerUserId AND localId = :localId LIMIT 1")
    suspend fun getByLocalId(ownerUserId: String, localId: String): MeditationSessionEntity?

    @Query("SELECT * FROM meditation_sessions WHERE ownerUserId = :ownerUserId AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(ownerUserId: String, remoteId: String): MeditationSessionEntity?

    @Query(
        """
        SELECT * FROM meditation_sessions
        WHERE ownerUserId = :ownerUserId
        AND remoteId IS NOT NULL
        AND syncStatus = 'SYNCED'
        """,
    )
    suspend fun getSyncedRemoteSessions(ownerUserId: String): List<MeditationSessionEntity>

    @Query("SELECT * FROM meditation_sessions WHERE ownerUserId = :ownerUserId AND syncStatus != 'SYNCED' ORDER BY updatedAt ASC")
    suspend fun getPendingSessions(ownerUserId: String): List<MeditationSessionEntity>

    @Query("SELECT * FROM meditation_sessions WHERE ownerUserId = :ownerUserId")
    suspend fun getAllSessions(ownerUserId: String): List<MeditationSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MeditationSessionEntity)

    @Update
    suspend fun update(entity: MeditationSessionEntity)

    @Delete
    suspend fun delete(entity: MeditationSessionEntity)
}
