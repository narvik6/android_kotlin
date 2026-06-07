package com.bibo.core.config

data class AppConfig(
    val host: String,
    val port: Int,
    val jwt: JwtConfig,
    val firestore: FirestoreConfig,
)

data class JwtConfig(
    val secret: String?,
    val issuer: String,
    val audience: String,
    val tokenTtlSeconds: Long,
)

data class FirestoreConfig(
    val credentialsPath: String?,
)

object AppConfigLoader {
    fun load(): AppConfig =
        AppConfig(
            host = value("HOST") ?: DEFAULT_HOST,
            port = value("PORT")?.toIntOrNull() ?: DEFAULT_PORT,
            jwt = JwtConfig(
                secret = value("JWT_SECRET")?.takeIf(String::isNotBlank),
                issuer = value("JWT_ISSUER") ?: DEFAULT_JWT_ISSUER,
                audience = value("JWT_AUDIENCE") ?: DEFAULT_JWT_AUDIENCE,
                tokenTtlSeconds = value("JWT_TTL_SECONDS")?.toLongOrNull() ?: DEFAULT_JWT_TTL_SECONDS,
            ),
            firestore = FirestoreConfig(
                credentialsPath = value("FIRESTORE_CREDENTIALS_PATH")?.takeIf(String::isNotBlank),
            ),
        )

    private fun value(name: String): String? =
        System.getenv(name) ?: System.getProperty(name)

    private const val DEFAULT_HOST = "localhost"
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_JWT_ISSUER = "bibo"
    private const val DEFAULT_JWT_AUDIENCE = "bibo-api"
    private const val DEFAULT_JWT_TTL_SECONDS = 86_400L
}
