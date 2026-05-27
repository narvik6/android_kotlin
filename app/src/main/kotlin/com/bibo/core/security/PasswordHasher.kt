package com.bibo.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasher {
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)

        val hash = pbkdf2(password, salt, ITERATIONS)

        return listOf(
            FORMAT,
            ITERATIONS.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash),
        ).joinToString(SEPARATOR)
    }

    fun verify(password: String, passwordHash: String): Boolean {
        val parts = passwordHash.split(SEPARATOR)
        if (parts.size != 4 || parts[0] != FORMAT) {
            return false
        }

        val iterations = parts[1].toIntOrNull() ?: return false

        return runCatching {
            val salt = Base64.getDecoder().decode(parts[2])
            val expectedHash = Base64.getDecoder().decode(parts[3])
            val actualHash = pbkdf2(password, salt, iterations)

            MessageDigest.isEqual(expectedHash, actualHash)
        }.getOrDefault(false)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)

        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val FORMAT = "pbkdf2_sha256"
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
        const val SALT_BYTES = 16
        const val SEPARATOR = "$"
    }
}
