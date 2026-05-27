package com.bibo.core.firestore

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.bibo.core.config.FirestoreConfig
import java.io.Closeable
import java.io.File
import java.io.FileInputStream

class FirestoreProvider(
    private val config: FirestoreConfig,
) : Closeable {
    @Volatile
    private var firestore: Firestore? = null

    fun get(): Firestore =
        firestore ?: synchronized(this) {
            firestore ?: createFirestore().also { firestore = it }
        }

    private fun createFirestore(): Firestore {
        val credentials = loadCredentials()
        val projectId = (credentials as? ServiceAccountCredentials)?.projectId
            ?: error("Service account JSON must contain project_id")

        val builder = FirestoreOptions.newBuilder().setProjectId(projectId)

        builder.setCredentials(credentials)

        return builder.build().service
    }

    private fun loadCredentials(): GoogleCredentials {
        val credentialsPath = requireNotNull(config.credentialsPath) {
            "FIRESTORE_CREDENTIALS_PATH is required to create Firestore client"
        }
        val credentialsFile = File(credentialsPath)

        if (!credentialsFile.isFile) {
            error("Firestore credentials file not found: $credentialsPath")
        }

        return FileInputStream(credentialsFile).use { credentialsStream ->
            GoogleCredentials.fromStream(credentialsStream)
        }
    }

    override fun close() {
        firestore?.close()
        firestore = null
    }
}
