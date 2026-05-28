package com.bibo.meditationsessions.domain

import com.bibo.core.error.NotFoundException

class GetMeditationSessionByIdUseCase(
    private val repository: MeditationSessionRepository,
) {
    suspend fun execute(ownerUserId: String, sessionId: String): MeditationSession =
        repository.findByIdForUser(ownerUserId, sessionId)
            ?: throw NotFoundException("Meditation session not found")
}
