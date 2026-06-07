package com.bibo.android.features.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.features.diary.domain.DeleteDiaryEntryUseCase
import com.bibo.android.features.diary.domain.GetJournalItemsUseCase
import com.bibo.android.features.diary.domain.UpdateDiaryEntryUseCase
import com.bibo.android.features.journal.domain.AddSearchHistoryItemUseCase
import com.bibo.android.features.journal.domain.ClearSearchHistoryUseCase
import com.bibo.android.features.journal.domain.JournalItem
import com.bibo.android.features.journal.domain.ObserveSearchHistoryUseCase
import com.bibo.android.features.journal.domain.SearchJournalItemsUseCase
import com.bibo.android.features.meditation.domain.DeleteMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.GetMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.UpdateMeditationSessionUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalUiState(
    val items: List<JournalItem> = emptyList(),
    val query: String = "",
    val history: List<String> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class JournalViewModel(
    getJournalItemsUseCase: GetJournalItemsUseCase,
    getMeditationSessionsUseCase: GetMeditationSessionsUseCase,
    private val deleteDiaryEntryUseCase: DeleteDiaryEntryUseCase,
    private val updateDiaryEntryUseCase: UpdateDiaryEntryUseCase,
    private val deleteMeditationSessionUseCase: DeleteMeditationSessionUseCase,
    private val updateMeditationSessionUseCase: UpdateMeditationSessionUseCase,
    private val searchJournalItemsUseCase: SearchJournalItemsUseCase,
    private val observeSearchHistoryUseCase: ObserveSearchHistoryUseCase,
    private val addSearchHistoryItemUseCase: AddSearchHistoryItemUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
) : ViewModel() {
    private val queryInput = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private val debouncedQuery = queryInput
        .debounce(300)
        .distinctUntilChanged()

    private val journalControls = combine(
        queryInput,
        debouncedQuery,
        observeSearchHistoryUseCase(),
    ) { queryInput, debouncedQuery, history ->
        JournalControls(
            queryInput = queryInput,
            debouncedQuery = debouncedQuery,
            history = history,
        )
    }

    val uiState: StateFlow<JournalUiState> = combine(
        getJournalItemsUseCase(),
        getMeditationSessionsUseCase(),
        journalControls,
    ) { diaryEntries, meditationSessions, controls ->
        val items = diaryEntries.map { JournalItem.Diary(it) } +
            meditationSessions.map { JournalItem.Meditation(it) }
        val sorted = items.sortedByDescending { it.sortDateTime }
        JournalUiState(
            items = searchJournalItemsUseCase(sorted, controls.debouncedQuery),
            query = controls.queryInput,
            history = controls.history,
            loading = controls.queryInput != controls.debouncedQuery,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = JournalUiState(),
        )

    fun setQuery(value: String) {
        queryInput.update { value }
    }

    fun refreshSearch() {
        queryInput.update { it }
    }

    fun addCurrentQueryToHistory() {
        val current = queryInput.value
        viewModelScope.launch {
            addSearchHistoryItemUseCase(current)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            clearSearchHistoryUseCase()
        }
    }

    fun deleteDiaryEntry(localId: String) {
        viewModelScope.launch {
            deleteDiaryEntryUseCase(localId)
        }
    }

    fun updateDiaryEntry(localId: String, text: String?, mood: Int?) {
        viewModelScope.launch {
            updateDiaryEntryUseCase(localId, text, mood)
        }
    }

    fun deleteMeditationSession(localId: String) {
        viewModelScope.launch {
            deleteMeditationSessionUseCase(localId)
        }
    }

    fun deleteItem(item: JournalItem) {
        when (item) {
            is JournalItem.Diary -> deleteDiaryEntry(item.entry.localId)
            is JournalItem.Meditation -> deleteMeditationSession(item.session.localId)
        }
    }

    fun updateMeditationSession(localId: String, startedAt: String, endedAt: String, durationSeconds: Long, note: String?) {
        viewModelScope.launch {
            updateMeditationSessionUseCase(localId, startedAt, endedAt, durationSeconds, note)
        }
    }
}

private data class JournalControls(
    val queryInput: String,
    val debouncedQuery: String,
    val history: List<String>,
)
