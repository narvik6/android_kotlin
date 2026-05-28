package com.bibo.meditationsessions.domain

class GetMeditationSessionsUseCase(
    private val repository: MeditationSessionRepository,
) {
    suspend fun execute(ownerUserId: String, query: String?): List<MeditationSession> =
        repository.findByUserId(ownerUserId, query?.trim()?.takeIf(String::isNotEmpty))
}
