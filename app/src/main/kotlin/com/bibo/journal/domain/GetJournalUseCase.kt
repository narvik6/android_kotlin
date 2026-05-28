package com.bibo.journal.domain

import com.bibo.diaryentries.domain.DiaryEntry
import com.bibo.diaryentries.domain.DiaryEntryRepository
import com.bibo.meditationsessions.domain.MeditationSession
import com.bibo.meditationsessions.domain.MeditationSessionRepository

class GetJournalUseCase(
    private val diaryEntryRepository: DiaryEntryRepository,
    private val meditationSessionRepository: MeditationSessionRepository,
) {
    suspend fun execute(ownerUserId: String, query: String?): List<JournalItem> {
        val normalizedQuery = query?.trim()?.takeIf(String::isNotEmpty)
        val entries = diaryEntryRepository.findByUserId(ownerUserId, normalizedQuery).map { it.toJournalItem() }
        val sessions = meditationSessionRepository.findByUserId(ownerUserId, normalizedQuery).map { it.toJournalItem() }

        return (entries + sessions).sortedByDescending { it.dateTime ?: it.startedAt }
    }

    private fun DiaryEntry.toJournalItem(): JournalItem =
        JournalItem(
            type = JournalItemType.DIARY_ENTRY,
            id = id,
            dateTime = dateTime,
            text = text,
            mood = mood,
            startedAt = null,
            endedAt = null,
            durationSeconds = null,
            note = null,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun MeditationSession.toJournalItem(): JournalItem =
        JournalItem(
            type = JournalItemType.MEDITATION_SESSION,
            id = id,
            dateTime = null,
            text = null,
            mood = null,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
