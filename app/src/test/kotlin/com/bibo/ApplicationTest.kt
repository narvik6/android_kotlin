package com.bibo

import com.bibo.core.error.ConflictException
import com.bibo.core.error.NotFoundException
import com.bibo.core.error.UnauthorizedException
import com.bibo.core.error.ValidationException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationTest {
    @Test
    fun `health endpoint returns ok`() = testApplication {
        application {
            module()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"status":"ok"}""", response.body<String>())
    }

    @Test
    fun `application exceptions use api error format`() = testApplication {
        application {
            module()
            routing {
                get("/test-validation-error") {
                    throw ValidationException("Invalid test payload")
                }
            }
        }

        val response = client.get("/test-validation-error")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            """{"code":"VALIDATION_ERROR","message":"Invalid test payload"}""",
            response.body<String>(),
        )
    }

    @Test
    fun `application exceptions use expected http statuses`() = testApplication {
        application {
            module()
            routing {
                get("/test-not-found") {
                    throw NotFoundException("Missing test resource")
                }
                get("/test-conflict") {
                    throw ConflictException("Duplicate test resource")
                }
                get("/test-unauthorized") {
                    throw UnauthorizedException()
                }
            }
        }

        val notFound = client.get("/test-not-found")
        val conflict = client.get("/test-conflict")
        val unauthorized = client.get("/test-unauthorized")

        assertEquals(HttpStatusCode.NotFound, notFound.status)
        assertEquals("""{"code":"NOT_FOUND","message":"Missing test resource"}""", notFound.body<String>())
        assertEquals(HttpStatusCode.Conflict, conflict.status)
        assertEquals("""{"code":"CONFLICT","message":"Duplicate test resource"}""", conflict.body<String>())
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)
        assertEquals("""{"code":"UNAUTHORIZED","message":"Unauthorized"}""", unauthorized.body<String>())
    }

    @Test
    fun `protected endpoints return unauthorized api error without token`() = testApplication {
        application {
            module()
        }

        val response = client.get("/diary-entries")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(
            """{"code":"UNAUTHORIZED","message":"Unauthorized"}""",
            response.body<String>(),
        )
    }

    @Test
    fun `protected endpoints return unauthorized api error with invalid token`() = testApplication {
        application {
            module()
        }

        val response = client.get("/diary-entries") {
            header(HttpHeaders.Authorization, "Bearer invalid-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(
            """{"code":"UNAUTHORIZED","message":"Unauthorized"}""",
            response.body<String>(),
        )
    }
}
