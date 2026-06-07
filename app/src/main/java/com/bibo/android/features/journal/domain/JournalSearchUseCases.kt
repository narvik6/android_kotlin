package com.bibo.android.features.journal.domain

import com.bibo.android.core.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class SearchJournalItemsUseCase {
    operator fun invoke(items: List<JournalItem>, query: String): List<JournalItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return items
        return items.filter { item -> item.searchableText().contains(normalized) }
    }
}

class ObserveSearchHistoryUseCase(
    private val userPreferences: UserPreferences,
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<String>> =
        userPreferences.authData.flatMapLatest { auth ->
            val ownerUserId = auth.userId
            if (ownerUserId == null) flowOf(emptyList()) else userPreferences.searchHistory(ownerUserId)
        }
}

class AddSearchHistoryItemUseCase(
    private val userPreferences: UserPreferences,
) {
    suspend operator fun invoke(query: String) {
        val ownerUserId = userPreferences.currentUserId() ?: return
        userPreferences.addSearchHistoryItem(ownerUserId, query)
    }
}

class ClearSearchHistoryUseCase(
    private val userPreferences: UserPreferences,
) {
    suspend operator fun invoke() {
        val ownerUserId = userPreferences.currentUserId() ?: return
        userPreferences.clearSearchHistory(ownerUserId)
    }
}

private fun JournalItem.searchableText(): String = when (this) {
    is JournalItem.Diary -> listOfNotNull(
        "дневник",
        entry.text,
        entry.mood?.toString(),
        entry.dateTime,
        entry.createdAt,
        entry.updatedAt,
    ).joinToString(" ").lowercase()

    is JournalItem.Meditation -> listOfNotNull(
        "медитация",
        session.note,
        session.startedAt,
        session.endedAt,
        session.durationSeconds.toString(),
        session.createdAt,
        session.updatedAt,
    ).joinToString(" ").lowercase()
}
