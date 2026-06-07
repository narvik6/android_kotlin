package com.bibo.auth.domain

import com.auth0.jwt.JWT
import com.bibo.core.config.JwtConfig
import com.bibo.core.error.ConflictException
import com.bibo.core.error.UnauthorizedException
import com.bibo.core.error.ValidationException
import com.bibo.core.security.JwtService
import com.bibo.core.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthUseCaseTest {
    private val passwordHasher = PasswordHasher()
    private val jwtService = JwtService(
        JwtConfig(
            secret = "test-secret-with-enough-length",
            issuer = "test-issuer",
            audience = "test-audience",
            tokenTtlSeconds = 3_600,
        ),
    )

    @Test
    fun `register creates user and token with user id claim`() = runBlocking {
        val repository = InMemoryAuthRepository()
        val useCase = RegisterUseCase(repository, passwordHasher, jwtService)

        val result = useCase.execute("Hunter@Test.Com", "secret-password")
        val decodedToken = jwtService.verifier().verify(result.token)

        assertEquals("hunter@test.com", result.user.email)
        assertEquals(result.user.id, decodedToken.getClaim(JwtService.USER_ID_CLAIM).asString())
        assertFalse(repository.users.single().passwordHash.contains("secret-password"))
    }

    @Test
    fun `register returns conflict for duplicate email`() = runBlocking {
        val repository = InMemoryAuthRepository()
        val useCase = RegisterUseCase(repository, passwordHasher, jwtService)

        useCase.execute("hunter@test.com", "secret-password")

        assertThrows(ConflictException::class.java) {
            runBlocking {
                useCase.execute("hunter@test.com", "another-password")
            }
        }
        Unit
    }

    @Test
    fun `register validates email and password`() = runBlocking {
        val repository = InMemoryAuthRepository()
        val useCase = RegisterUseCase(repository, passwordHasher, jwtService)

        assertThrows(ValidationException::class.java) {
            runBlocking {
                useCase.execute("not-an-email", "secret-password")
            }
        }
        assertThrows(ValidationException::class.java) {
            runBlocking {
                useCase.execute("hunter@test.com", "short")
            }
        }
        assertEquals(0, repository.users.size)
        Unit
    }

    @Test
    fun `login returns token for existing user`() = runBlocking {
        val repository = InMemoryAuthRepository()
        val registerUseCase = RegisterUseCase(repository, passwordHasher, jwtService)
        val loginUseCase = LoginUseCase(repository, passwordHasher, jwtService)
        val registered = registerUseCase.execute("hunter@test.com", "secret-password")

        val loggedIn = loginUseCase.execute("hunter@test.com", "secret-password")
        val decodedToken = JWT.decode(loggedIn.token)

        assertEquals(registered.user.id, loggedIn.user.id)
        assertNotNull(loggedIn.token)
        assertEquals(registered.user.id, decodedToken.getClaim(JwtService.USER_ID_CLAIM).asString())
    }

    @Test
    fun `login returns unauthorized for wrong password`() = runBlocking {
        val repository = InMemoryAuthRepository()
        val registerUseCase = RegisterUseCase(repository, passwordHasher, jwtService)
        val loginUseCase = LoginUseCase(repository, passwordHasher, jwtService)

        registerUseCase.execute("hunter@test.com", "secret-password")

        assertThrows(UnauthorizedException::class.java) {
            runBlocking {
                loginUseCase.execute("hunter@test.com", "wrong-password")
            }
        }
        Unit
    }

    @Test
    fun `login validates email before checking credentials`() = runBlocking {
        val repository = InMemoryAuthRepository()
        val loginUseCase = LoginUseCase(repository, passwordHasher, jwtService)

        assertThrows(ValidationException::class.java) {
            runBlocking {
                loginUseCase.execute("not-an-email", "secret-password")
            }
        }
        Unit
    }

    private class InMemoryAuthRepository : AuthRepository {
        val users = mutableListOf<User>()

        override suspend fun findByEmail(email: String): User? =
            users.firstOrNull { it.email == email }

        override suspend fun createUser(email: String, passwordHash: String): User {
            val user = User(
                id = "user-${users.size + 1}",
                email = email,
                passwordHash = passwordHash,
                createdAt = Instant.now(),
            )

            users += user

            return user
        }
    }
}
