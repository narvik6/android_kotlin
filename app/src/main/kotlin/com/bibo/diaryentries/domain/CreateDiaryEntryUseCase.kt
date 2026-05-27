package com.bibo.diaryentries.domain

class CreateDiaryEntryUseCase(
    private val repository: DiaryEntryRepository,
) {
    suspend fun execute(ownerUserId: String, input: DiaryEntryInput): DiaryEntry =
        repository.create(ownerUserId, input.validated())
}
