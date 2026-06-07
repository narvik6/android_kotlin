package com.bibo.android.features.journal.domain

import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.meditation.domain.MeditationSession
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalItemTest {
    @Test
    fun journalItemsSortNewestFirstAcrossDiaryAndMeditation() {
        val items = listOf(
            JournalItem.Diary(diary("2026-06-04T08:00:00Z")),
            JournalItem.Meditation(session("2026-06-04T18:00:00Z")),
        ).sortedByDescending { it.sortDateTime }

        assertEquals("meditation:meditation-id", items.first().id)
        assertEquals("diary:diary-id", items.last().id)
    }

    private fun diary(dateTime: String): DiaryEntry =
        DiaryEntry(
            localId = "diary-id",
            remoteId = null,
            ownerUserId = "user-id",
            text = null,
            mood = null,
            dateTime = dateTime,
            createdAt = dateTime,
            updatedAt = dateTime,
        )

    private fun session(startedAt: String): MeditationSession =
        MeditationSession(
            localId = "meditation-id",
            remoteId = null,
            ownerUserId = "user-id",
            startedAt = startedAt,
            endedAt = "2026-06-04T18:10:00Z",
            durationSeconds = 600,
            note = null,
            createdAt = startedAt,
            updatedAt = startedAt,
        )
}
