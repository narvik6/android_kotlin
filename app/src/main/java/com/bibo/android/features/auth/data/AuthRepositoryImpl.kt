package com.bibo.android.features.auth.data

import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.network.ApiClient
import com.bibo.android.core.util.AppResult
import com.bibo.android.features.auth.domain.AuthRepository
import com.bibo.android.features.auth.domain.User
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val apiClient: ApiClient,
    private val userPreferences: UserPreferences,
) : AuthRepository {
    override fun observeCurrentUser(): Flow<AuthLocalData> = userPreferences.authData

    override suspend fun login(email: String, password: String): AppResult<User> =
        authenticate { apiClient.login(email, password) }

    override suspend fun register(email: String, password: String): AppResult<User> =
        authenticate { apiClient.register(email, password) }

    override suspend fun logout() {
        userPreferences.clearAuth()
    }

    private suspend fun authenticate(
        request: suspend () -> AppResult<com.bibo.android.core.network.AuthResponse>,
    ): AppResult<User> =
        when (val result = request()) {
            is AppResult.Error -> result
            is AppResult.Success -> {
                val response = result.value
                userPreferences.saveAuth(
                    token = response.token,
                    userId = response.user.id,
                    email = response.user.email,
                )
                AppResult.Success(User(id = response.user.id, email = response.user.email))
            }
        }
}
