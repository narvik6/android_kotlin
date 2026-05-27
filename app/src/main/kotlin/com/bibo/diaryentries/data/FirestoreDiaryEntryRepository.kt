package com.bibo.diaryentries.data

import com.google.cloud.firestore.Firestore
import com.bibo.core.firestore.FirestoreProvider
import com.bibo.diaryentries.domain.DiaryEntry
import com.bibo.diaryentries.domain.DiaryEntryInput
import com.bibo.diaryentries.domain.DiaryEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreDiaryEntryRepository(
    private val firestoreProvider: FirestoreProvider,
) : DiaryEntryRepository {
    override suspend fun findByUserId(ownerUserId: String, query: String?): List<DiaryEntry> =
        withContext(Dispatchers.IO) {
            val entries = diaryEntriesCollection()
                .whereEqualTo("ownerUserId", ownerUserId)
                .get()
                .get()
                .documents
                .mapNotNull { it.toFirestoreDiaryEntryModel()?.toDomain() }
                .sortedByDescending { it.dateTime }

            query?.let { entries.filterByQuery(it) } ?: entries
        }

    override suspend fun findByIdForUser(ownerUserId: String, entryId: String): DiaryEntry? =
        withContext(Dispatchers.IO) {
            diaryEntriesCollection()
                .document(entryId)
                .get()
                .get()
                .toFirestoreDiaryEntryModel()
                ?.toDomain()
                ?.takeIf { it.ownerUserId == ownerUserId }
        }

    override suspend fun create(ownerUserId: String, input: DiaryEntryInput): DiaryEntry =
        withContext(Dispatchers.IO) {
            val document = diaryEntriesCollection().document()
            val model = FirestoreDiaryEntryModel(
                id = document.id,
                ownerUserId = ownerUserId,
                text = input.text,
                mood = input.mood,
                dateTime = requireNotNull(input.dateTime).toFirestoreTimestamp(),
                createdAt = requireNotNull(input.createdAt).toFirestoreTimestamp(),
                updatedAt = requireNotNull(input.updatedAt).toFirestoreTimestamp(),
            )

            document.set(model.toFirestoreMap()).get()

            model.toDomain()
        }

    override suspend fun update(ownerUserId: String, entryId: String, input: DiaryEntryInput): DiaryEntry? =
        withContext(Dispatchers.IO) {
            val existing = findByIdForUser(ownerUserId, entryId) ?: return@withContext null
            val model = FirestoreDiaryEntryModel(
                id = existing.id,
                ownerUserId = existing.ownerUserId,
                text = input.text,
                mood = input.mood,
                dateTime = requireNotNull(input.dateTime).toFirestoreTimestamp(),
                createdAt = requireNotNull(input.createdAt).toFirestoreTimestamp(),
                updatedAt = requireNotNull(input.updatedAt).toFirestoreTimestamp(),
            )

            diaryEntriesCollection().document(entryId).set(model.toFirestoreMap()).get()

            model.toDomain()
        }

    override suspend fun delete(ownerUserId: String, entryId: String): Boolean =
        withContext(Dispatchers.IO) {
            findByIdForUser(ownerUserId, entryId) ?: return@withContext false

            diaryEntriesCollection().document(entryId).delete().get()
            true
        }

    private fun List<DiaryEntry>.filterByQuery(query: String): List<DiaryEntry> {
        val normalizedQuery = query.lowercase()

        return filter { entry ->
            entry.text?.contains(normalizedQuery, ignoreCase = true) == true ||
                entry.mood?.toString()?.contains(normalizedQuery) == true ||
                entry.dateTime.toString().lowercase().contains(normalizedQuery) ||
                entry.createdAt.toString().lowercase().contains(normalizedQuery) ||
                entry.updatedAt.toString().lowercase().contains(normalizedQuery)
        }
    }

    private fun diaryEntriesCollection() =
        firestore().collection(DIARY_ENTRIES_COLLECTION)

    private fun firestore(): Firestore =
        firestoreProvider.get()

    private companion object {
        const val DIARY_ENTRIES_COLLECTION = "diary_entries"
    }
}
