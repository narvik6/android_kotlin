package com.bibo.auth.data

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.bibo.auth.domain.AuthRepository
import com.bibo.auth.domain.User
import com.bibo.core.error.ConflictException
import com.bibo.core.firestore.FirestoreProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreAuthRepository(
    private val firestoreProvider: FirestoreProvider,
) : AuthRepository {
    override suspend fun findByEmail(email: String): User? =
        withContext(Dispatchers.IO) {
            usersCollection()
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .get()
                .documents
                .firstOrNull()
                ?.toFirestoreUserModel()
                ?.toDomain()
        }

    override suspend fun createUser(email: String, passwordHash: String): User =
        withContext(Dispatchers.IO) {
            if (findByEmail(email) != null) {
                throw ConflictException("User with this email already exists")
            }

            val document = usersCollection().document()
            val model = FirestoreUserModel(
                id = document.id,
                email = email,
                passwordHash = passwordHash,
                createdAt = Timestamp.now(),
            )

            document.set(model.toFirestoreMap()).get()

            model.toDomain()
        }

    private fun usersCollection() =
        firestore().collection(USERS_COLLECTION)

    private fun firestore(): Firestore =
        firestoreProvider.get()

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
