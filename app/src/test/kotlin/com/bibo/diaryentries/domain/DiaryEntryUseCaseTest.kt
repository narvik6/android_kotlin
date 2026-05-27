package com.bibo.diaryentries.domain

import com.bibo.core.error.NotFoundException
import com.bibo.core.error.ValidationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DiaryEntryUseCaseTest {
    @Test
    fun `create diary entry stores entry for user`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val useCase = CreateDiaryEntryUseCase(repository)

        val entry = useCase.execute("user-1", sampleInput(text = "Calm day", mood = 1))

        assertEquals("user-1", entry.ownerUserId)
        assertEquals("Calm day", entry.text)
        assertEquals(1, entry.mood)
        assertEquals(entry, repository.findByIdForUser("user-1", entry.id))
    }

    @Test
    fun `create diary entry validates required timestamps and mood`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val useCase = CreateDiaryEntryUseCase(repository)

        assertThrows(ValidationException::class.java) {
            runBlocking { useCase.execute("user-1", sampleInput(dateTime = null)) }
        }
        assertThrows(ValidationException::class.java) {
            runBlocking { useCase.execute("user-1", sampleInput(mood = 2)) }
        }
        Unit
    }

    @Test
    fun `get diary entries returns only own entries`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val createUseCase = CreateDiaryEntryUseCase(repository)
        val getUseCase = GetDiaryEntriesUseCase(repository)

        createUseCase.execute("user-1", sampleInput(text = "Owner"))
        createUseCase.execute("user-2", sampleInput(text = "Stranger"))

        val entries = getUseCase.execute("user-1", null)

        assertEquals(1, entries.size)
        assertEquals("Owner", entries.single().text)
    }

    @Test
    fun `get diary entry by id returns entry only for owner`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val createUseCase = CreateDiaryEntryUseCase(repository)
        val getByIdUseCase = GetDiaryEntryByIdUseCase(repository)
        val entry = createUseCase.execute("owner", sampleInput())

        assertEquals(entry, getByIdUseCase.execute("owner", entry.id))
        assertThrows(NotFoundException::class.java) {
            runBlocking { getByIdUseCase.execute("stranger", entry.id) }
        }
        Unit
    }

    @Test
    fun `update diary entry only for owner`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val createUseCase = CreateDiaryEntryUseCase(repository)
        val updateUseCase = UpdateDiaryEntryUseCase(repository)
        val entry = createUseCase.execute("owner", sampleInput(text = "Before"))

        val updated = updateUseCase.execute("owner", entry.id, sampleInput(text = "After", mood = 0))

        assertEquals("After", updated.text)
        assertEquals(0, updated.mood)
        assertThrows(NotFoundException::class.java) {
            runBlocking { updateUseCase.execute("stranger", entry.id, sampleInput(text = "Hack")) }
        }
        assertEquals("After", repository.findByIdForUser("owner", entry.id)?.text)
    }

    @Test
    fun `delete diary entry only for owner`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val createUseCase = CreateDiaryEntryUseCase(repository)
        val deleteUseCase = DeleteDiaryEntryUseCase(repository)
        val entry = createUseCase.execute("owner", sampleInput())

        assertThrows(NotFoundException::class.java) {
            runBlocking { deleteUseCase.execute("stranger", entry.id) }
        }

        assertEquals(entry, repository.findByIdForUser("owner", entry.id))
        deleteUseCase.execute("owner", entry.id)
        assertThrows(NotFoundException::class.java) {
            runBlocking { deleteUseCase.execute("owner", entry.id) }
        }
        Unit
    }

    @Test
    fun `search diary entries checks text mood and timestamps`() = runBlocking {
        val repository = InMemoryDiaryEntryRepository()
        val createUseCase = CreateDiaryEntryUseCase(repository)
        val getUseCase = GetDiaryEntriesUseCase(repository)

        createUseCase.execute("user-1", sampleInput(text = "Meditation helped", mood = 1))
        createUseCase.execute("user-1", sampleInput(text = "Sad note", mood = -1))

        assertEquals(1, getUseCase.execute("user-1", "helped").size)
        assertEquals(1, getUseCase.execute("user-1", "-1").size)
        assertEquals(2, getUseCase.execute("user-1", "2026-06-04").size)
    }

    private fun sampleInput(
        text: String? = "Test entry",
        mood: Int? = 1,
        dateTime: Instant? = Instant.parse("2026-06-04T18:30:00Z"),
        createdAt: Instant? = Instant.parse("2026-06-04T18:30:00Z"),
        updatedAt: Instant? = Instant.parse("2026-06-04T18:30:00Z"),
    ): DiaryEntryInput =
        DiaryEntryInput(
            text = text,
            mood = mood,
            dateTime = dateTime,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private class InMemoryDiaryEntryRepository : DiaryEntryRepository {
        private val entries = mutableListOf<DiaryEntry>()

        override suspend fun findByUserId(ownerUserId: String, query: String?): List<DiaryEntry> {
            val userEntries = entries.filter { it.ownerUserId == ownerUserId }
            val normalizedQuery = query?.lowercase()

            return if (normalizedQuery == null) {
                userEntries
            } else {
                userEntries.filter { entry ->
                    entry.text?.contains(normalizedQuery, ignoreCase = true) == true ||
                        entry.mood?.toString()?.contains(normalizedQuery) == true ||
                        entry.dateTime.toString().lowercase().contains(normalizedQuery) ||
                        entry.createdAt.toString().lowercase().contains(normalizedQuery) ||
                        entry.updatedAt.toString().lowercase().contains(normalizedQuery)
                }
            }
        }

        override suspend fun findByIdForUser(ownerUserId: String, entryId: String): DiaryEntry? =
            entries.firstOrNull { it.id == entryId && it.ownerUserId == ownerUserId }

        override suspend fun create(ownerUserId: String, input: DiaryEntryInput): DiaryEntry {
            val entry = DiaryEntry(
                id = "entry-${entries.size + 1}",
                ownerUserId = ownerUserId,
                text = input.text,
                mood = input.mood,
                dateTime = requireNotNull(input.dateTime),
                createdAt = requireNotNull(input.createdAt),
                updatedAt = requireNotNull(input.updatedAt),
            )

            entries += entry

            return entry
        }

        override suspend fun update(ownerUserId: String, entryId: String, input: DiaryEntryInput): DiaryEntry? {
            val index = entries.indexOfFirst { it.id == entryId && it.ownerUserId == ownerUserId }
            if (index == -1) {
                return null
            }

            val updated = entries[index].copy(
                text = input.text,
                mood = input.mood,
                dateTime = requireNotNull(input.dateTime),
                createdAt = requireNotNull(input.createdAt),
                updatedAt = requireNotNull(input.updatedAt),
            )
            entries[index] = updated

            return updated
        }

        override suspend fun delete(ownerUserId: String, entryId: String): Boolean =
            entries.removeIf { it.id == entryId && it.ownerUserId == ownerUserId }
    }
}
