package com.bibo.core.security

import com.bibo.core.config.JwtConfig
import com.bibo.core.error.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.util.UUID

fun Application.configureSecurity(
    jwtConfig: JwtConfig,
    jwtService: JwtService,
) {
    val verificationSecret = jwtConfig.secret?.takeIf(String::isNotBlank)
        ?: UUID.randomUUID().toString().also {
            environment.log.warn("JWT_SECRET is not configured; protected endpoints will reject persisted tokens")
        }

    install(Authentication) {
        jwt(JwtService.AUTH_PROVIDER) {
            verifier(jwtService.verifier(verificationSecret))
            validate { credential ->
                val userId = credential.payload.getClaim(JwtService.USER_ID_CLAIM).asString()

                if (userId.isNullOrBlank()) {
                    null
                } else {
                    JWTPrincipal(credential.payload)
                }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiError("UNAUTHORIZED", "Unauthorized"),
                )
            }
        }
    }
}
