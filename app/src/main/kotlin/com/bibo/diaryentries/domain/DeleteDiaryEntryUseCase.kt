package com.bibo.diaryentries.domain

import com.bibo.core.error.NotFoundException

class DeleteDiaryEntryUseCase(
    private val repository: DiaryEntryRepository,
) {
    suspend fun execute(ownerUserId: String, entryId: String) {
        if (!repository.delete(ownerUserId, entryId)) {
            throw NotFoundException("Diary entry not found")
        }
    }
}
