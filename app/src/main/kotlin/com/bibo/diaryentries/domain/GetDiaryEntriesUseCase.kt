package com.bibo.diaryentries.domain

class GetDiaryEntriesUseCase(
    private val repository: DiaryEntryRepository,
) {
    suspend fun execute(ownerUserId: String, query: String?): List<DiaryEntry> =
        repository.findByUserId(ownerUserId, query?.trim()?.takeIf(String::isNotEmpty))
}
