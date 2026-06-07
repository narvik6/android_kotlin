package com.bibo.android.core.sync

import com.bibo.android.core.network.CreateDiaryEntryRequest
import com.bibo.android.core.network.CreateMeditationSessionRequest
import com.bibo.android.core.network.UpdateDiaryEntryRequest
import com.bibo.android.core.network.UpdateMeditationSessionRequest

interface SyncQueue {
    suspend fun enqueueDiaryCreate(ownerUserId: String, localId: String, request: CreateDiaryEntryRequest)
    suspend fun enqueueDiaryUpdate(ownerUserId: String, localId: String, request: UpdateDiaryEntryRequest)
    suspend fun enqueueDiaryDelete(ownerUserId: String, localId: String)
    suspend fun enqueueMeditationCreate(
        ownerUserId: String,
        localId: String,
        request: CreateMeditationSessionRequest,
    )
    suspend fun enqueueMeditationUpdate(
        ownerUserId: String,
        localId: String,
        request: UpdateMeditationSessionRequest,
    )
    suspend fun enqueueMeditationDelete(ownerUserId: String, localId: String)
    suspend fun syncCurrentUser(): Boolean
}
