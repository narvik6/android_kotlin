package com.bibo.meditationsessions.domain

import com.bibo.core.error.NotFoundException
import com.bibo.core.error.ValidationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MeditationSessionUseCaseTest {
    @Test
    fun `create meditation session stores session for user`() = runBlocking {
        val repository = InMemoryMeditationSessionRepository()
        val useCase = CreateMeditationSessionUseCase(repository)

        val session = useCase.execute("user-1", sampleInput(note = "Short practice"))

        assertEquals("user-1", session.ownerUserId)
        assertEquals(600, session.durationSeconds)
        assertEquals("Short practice", session.note)
    }

    @Test
    fun `create meditation session validates required fields and duration`() = runBlocking {
        val repository = InMemoryMeditationSessionRepository()
        val useCase = CreateMeditationSessionUseCase(repository)

        assertThrows(ValidationException::class.java) {
            runBlocking { useCase.execute("user-1", sampleInput(startedAt = null)) }
        }
        assertThrows(ValidationException::class.java) {
            runBlocking { useCase.execute("user-1", sampleInput(durationSeconds = -1)) }
        }
        Unit
    }

    @Test
    fun `meditation sessions are accessible only for owner`() = runBlocking {
        val repository = InMemoryMeditationSessionRepository()
        val createUseCase = CreateMeditationSessionUseCase(repository)
        val getByIdUseCase = GetMeditationSessionByIdUseCase(repository)
        val updateUseCase = UpdateMeditationSessionUseCase(repository)
        val deleteUseCase = DeleteMeditationSessionUseCase(repository)
        val session = createUseCase.execute("owner", sampleInput(note = "Before"))

        assertEquals(session, getByIdUseCase.execute("owner", session.id))
        assertThrows(NotFoundException::class.java) {
            runBlocking { getByIdUseCase.execute("stranger", session.id) }
        }
        val updated = updateUseCase.execute("owner", session.id, sampleInput(note = "After", durationSeconds = 720))
        assertEquals("After", updated.note)
        assertThrows(NotFoundException::class.java) {
            runBlocking { deleteUseCase.execute("stranger", session.id) }
        }
        deleteUseCase.execute("owner", session.id)
        Unit
    }

    @Test
    fun `search meditation sessions checks note timestamps and duration`() = runBlocking {
        val repository = InMemoryMeditationSessionRepository()
        val createUseCase = CreateMeditationSessionUseCase(repository)
        val getUseCase = GetMeditationSessionsUseCase(repository)

        createUseCase.execute("user-1", sampleInput(note = "Breathing", durationSeconds = 600))
        createUseCase.execute("user-1", sampleInput(note = "Evening calm", durationSeconds = 720))

        assertEquals(1, getUseCase.execute("user-1", "breathing").size)
        assertEquals(1, getUseCase.execute("user-1", "720").size)
        assertEquals(2, getUseCase.execute("user-1", "2026-06-04").size)
    }

    private fun sampleInput(
        startedAt: Instant? = Instant.parse("2026-06-04T18:00:00Z"),
        endedAt: Instant? = Instant.parse("2026-06-04T18:10:00Z"),
        durationSeconds: Long? = 600,
        note: String? = "Practice",
        createdAt: Instant? = Instant.parse("2026-06-04T18:10:00Z"),
        updatedAt: Instant? = Instant.parse("2026-06-04T18:10:00Z"),
    ): MeditationSessionInput =
        MeditationSessionInput(
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private class InMemoryMeditationSessionRepository : MeditationSessionRepository {
        private val sessions = mutableListOf<MeditationSession>()

        override suspend fun findByUserId(ownerUserId: String, query: String?): List<MeditationSession> {
            val userSessions = sessions.filter { it.ownerUserId == ownerUserId }
            val normalizedQuery = query?.lowercase()

            return if (normalizedQuery == null) {
                userSessions
            } else {
                userSessions.filter { session ->
                    session.note?.contains(normalizedQuery, ignoreCase = true) == true ||
                        session.startedAt.toString().lowercase().contains(normalizedQuery) ||
                        session.endedAt.toString().lowercase().contains(normalizedQuery) ||
                        session.durationSeconds.toString().contains(normalizedQuery) ||
                        session.createdAt.toString().lowercase().contains(normalizedQuery) ||
                        session.updatedAt.toString().lowercase().contains(normalizedQuery)
                }
            }
        }

        override suspend fun findByIdForUser(ownerUserId: String, sessionId: String): MeditationSession? =
            sessions.firstOrNull { it.id == sessionId && it.ownerUserId == ownerUserId }

        override suspend fun create(ownerUserId: String, input: MeditationSessionInput): MeditationSession {
            val session = MeditationSession(
                id = "session-${sessions.size + 1}",
                ownerUserId = ownerUserId,
                startedAt = requireNotNull(input.startedAt),
                endedAt = requireNotNull(input.endedAt),
                durationSeconds = requireNotNull(input.durationSeconds),
                note = input.note,
                createdAt = requireNotNull(input.createdAt),
                updatedAt = requireNotNull(input.updatedAt),
            )

            sessions += session

            return session
        }

        override suspend fun update(
            ownerUserId: String,
            sessionId: String,
            input: MeditationSessionInput,
        ): MeditationSession? {
            val index = sessions.indexOfFirst { it.id == sessionId && it.ownerUserId == ownerUserId }
            if (index == -1) {
                return null
            }

            val updated = sessions[index].copy(
                startedAt = requireNotNull(input.startedAt),
                endedAt = requireNotNull(input.endedAt),
                durationSeconds = requireNotNull(input.durationSeconds),
                note = input.note,
                createdAt = requireNotNull(input.createdAt),
                updatedAt = requireNotNull(input.updatedAt),
            )
            sessions[index] = updated

            return updated
        }

        override suspend fun delete(ownerUserId: String, sessionId: String): Boolean =
            sessions.removeIf { it.id == sessionId && it.ownerUserId == ownerUserId }
    }
}
