package com.bibo.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [
        DiaryEntryEntity::class,
        MeditationSessionEntity::class,
        SyncOperationEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun meditationSessionDao(): MeditationSessionDao
    abstract fun syncOperationDao(): SyncOperationDao
}

class AppTypeConverters {
    @TypeConverter
    fun syncStatusToString(value: SyncStatus): String = value.name

    @TypeConverter
    fun stringToSyncStatus(value: String): SyncStatus =
        runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.SYNCED)

    @TypeConverter
    fun syncEntityTypeToString(value: SyncEntityType): String = value.name

    @TypeConverter
    fun stringToSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

    @TypeConverter
    fun syncOperationTypeToString(value: SyncOperationType): String = value.name

    @TypeConverter
    fun stringToSyncOperationType(value: String): SyncOperationType = SyncOperationType.valueOf(value)
}
