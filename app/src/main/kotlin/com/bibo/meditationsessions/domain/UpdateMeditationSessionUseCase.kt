package com.bibo.meditationsessions.domain

import com.bibo.core.error.NotFoundException

class UpdateMeditationSessionUseCase(
    private val repository: MeditationSessionRepository,
) {
    suspend fun execute(ownerUserId: String, sessionId: String, input: MeditationSessionInput): MeditationSession =
        repository.update(ownerUserId, sessionId, input.validated())
            ?: throw NotFoundException("Meditation session not found")
}
