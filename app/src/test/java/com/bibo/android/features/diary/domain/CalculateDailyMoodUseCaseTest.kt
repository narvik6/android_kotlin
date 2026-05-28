package com.bibo.android.features.diary.domain

import com.bibo.android.core.database.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculateDailyMoodUseCaseTest {
    private val useCase = CalculateDailyMoodUseCase()

    @Test
    fun returnsPositiveMoodAndBetterTrend() {
        val result = useCase(
            listOf(
                entry("2026-06-04T08:00:00Z", -1),
                entry("2026-06-04T12:00:00Z", 1),
                entry("2026-06-04T18:00:00Z", 1),
            ),
        )

        assertEquals(":)", result?.emoji)
        assertEquals(MoodTrend.Better, result?.trend)
    }

    @Test
    fun returnsNeutralMoodForAverageInsideThreshold() {
        val result = useCase(
            listOf(
                entry("2026-06-04T08:00:00Z", -1),
                entry("2026-06-04T18:00:00Z", 1),
            ),
        )

        assertEquals("._.", result?.emoji)
        assertEquals(MoodTrend.Better, result?.trend)
    }

    @Test
    fun ignoresEntriesWithoutMood() {
        assertNull(useCase(listOf(entry("2026-06-04T08:00:00Z", null))))
    }

    private fun entry(dateTime: String, mood: Int?): DiaryEntry =
        DiaryEntry(
            localId = dateTime,
            remoteId = null,
            ownerUserId = "user-id",
            text = null,
            mood = mood,
            dateTime = dateTime,
            syncStatus = SyncStatus.SYNCED,
            createdAt = dateTime,
            updatedAt = dateTime,
        )
}
