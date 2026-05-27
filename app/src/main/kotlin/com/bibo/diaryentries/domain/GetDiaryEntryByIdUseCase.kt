package com.bibo.diaryentries.domain

import com.bibo.core.error.NotFoundException

class GetDiaryEntryByIdUseCase(
    private val repository: DiaryEntryRepository,
) {
    suspend fun execute(ownerUserId: String, entryId: String): DiaryEntry =
        repository.findByIdForUser(ownerUserId, entryId)
            ?: throw NotFoundException("Diary entry not found")
}
