package com.bibo.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.bibo.core.config.JwtConfig
import java.time.Instant
import java.util.Date

class JwtService(
    private val config: JwtConfig,
) {
    fun createToken(userId: String): String {
        val now = Instant.now()

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim(USER_ID_CLAIM, userId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(config.tokenTtlSeconds)))
            .sign(algorithm(requireSecret()))
    }

    fun verifier(secret: String = requireSecret()): JWTVerifier =
        JWT.require(algorithm(secret))
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()

    private fun requireSecret(): String =
        requireNotNull(config.secret) {
            "JWT_SECRET is required to create or verify JWT tokens"
        }

    private fun algorithm(secret: String): Algorithm =
        Algorithm.HMAC256(secret)

    companion object {
        const val AUTH_PROVIDER = "auth-jwt"
        const val USER_ID_CLAIM = "userId"
    }
}
