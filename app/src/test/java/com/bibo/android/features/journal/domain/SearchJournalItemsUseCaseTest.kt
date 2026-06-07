package com.bibo.android.features.journal.domain

import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.meditation.domain.MeditationSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchJournalItemsUseCaseTest {
    private val useCase = SearchJournalItemsUseCase()

    @Test
    fun findsDiaryEntryByText() {
        val items = listOf(
            JournalItem.Diary(diary(text = "Спокойный вечер после прогулки")),
            JournalItem.Meditation(session(note = "Дыхательная практика")),
        )

        val result = useCase(items, "прогулки")

        assertEquals(1, result.size)
        assertEquals("diary:diary-id", result.single().id)
    }

    @Test
    fun findsMeditationByNoteAndDuration() {
        val items = listOf(
            JournalItem.Diary(diary(text = "Обычный день")),
            JournalItem.Meditation(session(note = "Практика концентрации", durationSeconds = 600)),
        )

        assertEquals("meditation:meditation-id", useCase(items, "концентрации").single().id)
        assertEquals("meditation:meditation-id", useCase(items, "600").single().id)
    }

    @Test
    fun blankQueryReturnsAllItems() {
        val items = listOf(JournalItem.Diary(diary(text = "Текст")))

        assertEquals(items, useCase(items, " "))
    }

    private fun diary(text: String): DiaryEntry =
        DiaryEntry(
            localId = "diary-id",
            remoteId = null,
            ownerUserId = "user-id",
            text = text,
            mood = 1,
            dateTime = "2026-06-04T08:00:00Z",
            createdAt = "2026-06-04T08:00:00Z",
            updatedAt = "2026-06-04T08:00:00Z",
        )

    private fun session(note: String, durationSeconds: Long = 300): MeditationSession =
        MeditationSession(
            localId = "meditation-id",
            remoteId = null,
            ownerUserId = "user-id",
            startedAt = "2026-06-04T18:00:00Z",
            endedAt = "2026-06-04T18:10:00Z",
            durationSeconds = durationSeconds,
            note = note,
            createdAt = "2026-06-04T18:00:00Z",
            updatedAt = "2026-06-04T18:00:00Z",
        )
}
