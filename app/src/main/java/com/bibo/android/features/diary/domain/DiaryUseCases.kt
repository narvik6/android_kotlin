package com.bibo.android.features.diary.domain

import kotlinx.coroutines.flow.Flow

class GetDiaryEntriesUseCase(
    private val repository: DiaryRepository,
) {
    operator fun invoke(): Flow<List<DiaryEntry>> = repository.observeEntries()
}

class GetDiaryEntryByIdUseCase(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(localId: String): DiaryEntry? = repository.getEntryById(localId)
}

class CreateDiaryEntryUseCase(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(text: String?, mood: Int?) = repository.createEntry(text, mood)
}

class UpdateDiaryEntryUseCase(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(localId: String, text: String?, mood: Int?) =
        repository.updateEntry(localId, text, mood)
}

class DeleteDiaryEntryUseCase(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(localId: String) = repository.deleteEntry(localId)
}

class SyncPendingChangesUseCase(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke() = repository.syncPendingChanges()
}

class GetJournalItemsUseCase(
    private val repository: DiaryRepository,
) {
    operator fun invoke(): Flow<List<DiaryEntry>> = repository.observeJournalItems()
}

class CalculateDailyMoodUseCase {
    operator fun invoke(entries: List<DiaryEntry>): DailyMood? {
        val moods = entries
            .sortedBy { it.dateTime }
            .mapNotNull { it.mood }
        if (moods.isEmpty()) return null

        val average = moods.average()
        val emoji = when {
            average > 0.33 -> ":)"
            average < -0.33 -> ":("
            else -> "._."
        }
        val trend = when {
            moods.last() > moods.first() -> MoodTrend.Better
            moods.last() < moods.first() -> MoodTrend.Worse
            else -> MoodTrend.Same
        }
        return DailyMood(emoji = emoji, trend = trend)
    }
}
