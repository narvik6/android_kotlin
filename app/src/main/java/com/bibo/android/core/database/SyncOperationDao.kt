package com.bibo.android.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOperationDao {
    @Query(
        """
        SELECT * FROM sync_operations
        WHERE ownerUserId = :ownerUserId
        ORDER BY rowid ASC
        """,
    )
    suspend fun getOperations(ownerUserId: String): List<SyncOperationEntity>

    @Query(
        """
        SELECT * FROM sync_operations
        WHERE ownerUserId = :ownerUserId
        AND entityType = :entityType
        AND entityLocalId = :entityLocalId
        ORDER BY rowid ASC
        """,
    )
    suspend fun getOperationsForEntity(
        ownerUserId: String,
        entityType: SyncEntityType,
        entityLocalId: String,
    ): List<SyncOperationEntity>

    @Query(
        """
        SELECT COUNT(*) FROM sync_operations
        WHERE ownerUserId = :ownerUserId
        AND entityType = :entityType
        AND entityLocalId = :entityLocalId
        """,
    )
    suspend fun countOperationsForEntity(
        ownerUserId: String,
        entityType: SyncEntityType,
        entityLocalId: String,
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity)

    @Delete
    suspend fun delete(operation: SyncOperationEntity)
}
