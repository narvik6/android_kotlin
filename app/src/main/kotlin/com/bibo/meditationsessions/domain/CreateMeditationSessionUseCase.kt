package com.bibo.meditationsessions.domain

class CreateMeditationSessionUseCase(
    private val repository: MeditationSessionRepository,
) {
    suspend fun execute(ownerUserId: String, input: MeditationSessionInput): MeditationSession =
        repository.create(ownerUserId, input.validated())
}
