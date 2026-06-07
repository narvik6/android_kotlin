package com.bibo.android.core.network

import com.bibo.android.core.util.AppResult

interface BiboApi {
    suspend fun checkHealth(): AppResult<Unit>
    suspend fun getDiaryEntries(): AppResult<List<DiaryEntryResponse>>
    suspend fun createDiaryEntry(request: CreateDiaryEntryRequest): AppResult<DiaryEntryResponse>
    suspend fun updateDiaryEntry(remoteId: String, request: UpdateDiaryEntryRequest): AppResult<DiaryEntryResponse>
    suspend fun deleteDiaryEntry(remoteId: String): AppResult<Unit>
    suspend fun getMeditationSessions(): AppResult<List<MeditationSessionResponse>>
    suspend fun createMeditationSession(request: CreateMeditationSessionRequest): AppResult<MeditationSessionResponse>
    suspend fun updateMeditationSession(
        remoteId: String,
        request: UpdateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse>
    suspend fun deleteMeditationSession(remoteId: String): AppResult<Unit>
}
