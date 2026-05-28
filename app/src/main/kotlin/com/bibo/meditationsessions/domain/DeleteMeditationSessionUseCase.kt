package com.bibo.meditationsessions.domain

import com.bibo.core.error.NotFoundException

class DeleteMeditationSessionUseCase(
    private val repository: MeditationSessionRepository,
) {
    suspend fun execute(ownerUserId: String, sessionId: String) {
        if (!repository.delete(ownerUserId, sessionId)) {
            throw NotFoundException("Meditation session not found")
        }
    }
}
