package com.bibo.android.features.diary.domain

import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    fun observeEntries(): Flow<List<DiaryEntry>>
    fun observeJournalItems(): Flow<List<DiaryEntry>>
    suspend fun getEntryById(localId: String): DiaryEntry?
    suspend fun createEntry(text: String?, mood: Int?)
    suspend fun updateEntry(localId: String, text: String?, mood: Int?)
    suspend fun deleteEntry(localId: String)
    suspend fun syncPendingChanges()
}
