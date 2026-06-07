package com.bibo.android.core.datastore

import kotlinx.coroutines.flow.Flow

interface CurrentUserProvider {
    val authData: Flow<AuthLocalData>
    suspend fun currentUserId(): String?
}
