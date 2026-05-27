package com.bibo.android.features.auth.domain

import com.bibo.android.core.datastore.AuthLocalData
import com.bibo.android.core.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {
    @Test
    fun loginUseCaseTrimsEmailBeforeRepositoryCall() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        useCase(" user@example.com ", "password")

        assertEquals("user@example.com", repository.lastLoginEmail)
    }

    @Test
    fun authLocalDataIsAuthorizedOnlyWhenTokenAndUserIdExist() {
        assertTrue(
            AuthLocalData(
                token = "token",
                userId = "user-id",
                email = "user@example.com",
            ).isAuthorized,
        )
        assertFalse(
            AuthLocalData(
                token = "token",
                userId = null,
                email = "user@example.com",
            ).isAuthorized,
        )
    }
}

private class FakeAuthRepository : AuthRepository {
    var lastLoginEmail: String? = null

    override fun observeCurrentUser(): Flow<AuthLocalData> =
        flowOf(AuthLocalData(token = null, userId = null, email = null))

    override suspend fun login(email: String, password: String): AppResult<User> {
        lastLoginEmail = email
        return AppResult.Success(User(id = "user-id", email = email))
    }

    override suspend fun register(email: String, password: String): AppResult<User> =
        AppResult.Success(User(id = "user-id", email = email))

    override suspend fun logout() = Unit
}
