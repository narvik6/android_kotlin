package com.bibo.journal.domain

import com.bibo.diaryentries.domain.DiaryEntry
import com.bibo.diaryentries.domain.DiaryEntryInput
import com.bibo.diaryentries.domain.DiaryEntryRepository
import com.bibo.meditationsessions.domain.MeditationSession
import com.bibo.meditationsessions.domain.MeditationSessionInput
import com.bibo.meditationsessions.domain.MeditationSessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetJournalUseCaseTest {
    @Test
    fun `journal combines diary entries and meditation sessions sorted by event time`() = runBlocking {
        val useCase = GetJournalUseCase(
            diaryEntryRepository = InMemoryDiaryEntryRepository(
                sampleDiaryEntry(text = "Morning note"),
            ),
            meditationSessionRepository = InMemoryMeditationSessionRepository(
                sampleMeditationSession(note = "Breathing"),
            ),
        )

        val items = useCase.execute("user-1", null)

        assertEquals(listOf(JournalItemType.MEDITATION_SESSION, JournalItemType.DIARY_ENTRY), items.map { it.type })
        assertEquals(listOf("session-1", "entry-1"), items.map { it.id })
    }

    @Test
    fun `journal applies query to both repositories`() = runBlocking {
        val useCase = GetJournalUseCase(
            diaryEntryRepository = InMemoryDiaryEntryRepository(
                sampleDiaryEntry(text = "Focused work"),
            ),
            meditationSessionRepository = InMemoryMeditationSessionRepository(
                sampleMeditationSession(note = "Breathing"),
            ),
        )

        val items = useCase.execute("user-1", " breathing ")

        assertEquals(1, items.size)
        assertEquals(JournalItemType.MEDITATION_SESSION, items.single().type)
        assertEquals("session-1", items.single().id)
    }

    private fun sampleDiaryEntry(text: String): DiaryEntry =
        DiaryEntry(
            id = "entry-1",
            ownerUserId = "user-1",
            text = text,
            mood = 1,
            dateTime = Instant.parse("2026-06-04T09:00:00Z"),
            createdAt = Instant.parse("2026-06-04T09:01:00Z"),
            updatedAt = Instant.parse("2026-06-04T09:01:00Z"),
        )

    private fun sampleMeditationSession(note: String): MeditationSession =
        MeditationSession(
            id = "session-1",
            ownerUserId = "user-1",
            startedAt = Instant.parse("2026-06-04T10:00:00Z"),
            endedAt = Instant.parse("2026-06-04T10:10:00Z"),
            durationSeconds = 600,
            note = note,
            createdAt = Instant.parse("2026-06-04T10:10:00Z"),
            updatedAt = Instant.parse("2026-06-04T10:10:00Z"),
        )

    private class InMemoryDiaryEntryRepository(
        private vararg val entries: DiaryEntry,
    ) : DiaryEntryRepository {
        override suspend fun findByUserId(ownerUserId: String, query: String?): List<DiaryEntry> {
            val userEntries = entries.filter { it.ownerUserId == ownerUserId }
            val normalizedQuery = query?.lowercase()

            return if (normalizedQuery == null) {
                userEntries
            } else {
                userEntries.filter { it.text?.contains(normalizedQuery, ignoreCase = true) == true }
            }
        }

        override suspend fun findByIdForUser(ownerUserId: String, entryId: String): DiaryEntry? =
            entries.firstOrNull { it.ownerUserId == ownerUserId && it.id == entryId }

        override suspend fun create(ownerUserId: String, input: DiaryEntryInput): DiaryEntry =
            error("Not needed for journal tests")

        override suspend fun update(ownerUserId: String, entryId: String, input: DiaryEntryInput): DiaryEntry? =
            error("Not needed for journal tests")

        override suspend fun delete(ownerUserId: String, entryId: String): Boolean =
            error("Not needed for journal tests")
    }

    private class InMemoryMeditationSessionRepository(
        private vararg val sessions: MeditationSession,
    ) : MeditationSessionRepository {
        override suspend fun findByUserId(ownerUserId: String, query: String?): List<MeditationSession> {
            val userSessions = sessions.filter { it.ownerUserId == ownerUserId }
            val normalizedQuery = query?.lowercase()

            return if (normalizedQuery == null) {
                userSessions
            } else {
                userSessions.filter { it.note?.contains(normalizedQuery, ignoreCase = true) == true }
            }
        }

        override suspend fun findByIdForUser(ownerUserId: String, sessionId: String): MeditationSession? =
            sessions.firstOrNull { it.ownerUserId == ownerUserId && it.id == sessionId }

        override suspend fun create(ownerUserId: String, input: MeditationSessionInput): MeditationSession =
            error("Not needed for journal tests")

        override suspend fun update(
            ownerUserId: String,
            sessionId: String,
            input: MeditationSessionInput,
        ): MeditationSession? =
            error("Not needed for journal tests")

        override suspend fun delete(ownerUserId: String, sessionId: String): Boolean =
            error("Not needed for journal tests")
    }
}
