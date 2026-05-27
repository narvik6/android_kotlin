package com.bibo.android.features.auth.domain

import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.core.util.AppResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<AuthLocalData>
    suspend fun login(email: String, password: String): AppResult<User>
    suspend fun register(email: String, password: String): AppResult<User>
    suspend fun logout()
}
