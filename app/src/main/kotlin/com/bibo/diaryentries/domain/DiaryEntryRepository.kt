package com.bibo.diaryentries.domain

interface DiaryEntryRepository {
    suspend fun findByUserId(ownerUserId: String, query: String?): List<DiaryEntry>

    suspend fun findByIdForUser(ownerUserId: String, entryId: String): DiaryEntry?

    suspend fun create(ownerUserId: String, input: DiaryEntryInput): DiaryEntry

    suspend fun update(ownerUserId: String, entryId: String, input: DiaryEntryInput): DiaryEntry?

    suspend fun delete(ownerUserId: String, entryId: String): Boolean
}
