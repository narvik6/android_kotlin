package org.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {

    private const val SECRET = "my-super-secret-key-for-nobel-prize-api-32"
    private const val ISSUER = "http://localhost:8080"
    private const val VALIDITY_IN_MS = 30 * 60 * 1000L
    private val algorithm = Algorithm.HMAC256(SECRET)


    val verifier = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .build()


    fun generateToken(username: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_IN_MS))
            .sign(algorithm)
    }
}