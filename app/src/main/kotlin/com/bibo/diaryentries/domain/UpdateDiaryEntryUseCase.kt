package com.bibo.diaryentries.domain

import com.bibo.core.error.NotFoundException

class UpdateDiaryEntryUseCase(
    private val repository: DiaryEntryRepository,
) {
    suspend fun execute(ownerUserId: String, entryId: String, input: DiaryEntryInput): DiaryEntry =
        repository.update(ownerUserId, entryId, input.validated())
            ?: throw NotFoundException("Diary entry not found")
}
