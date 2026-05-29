package com.bibo.android.features.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.features.diary.domain.DeleteDiaryEntryUseCase
import com.bibo.android.features.diary.domain.GetJournalItemsUseCase
import com.bibo.android.features.diary.domain.UpdateDiaryEntryUseCase
import com.bibo.android.features.journal.domain.JournalItem
import com.bibo.android.features.meditation.domain.DeleteMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.GetMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.UpdateMeditationSessionUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalUiState(
    val items: List<JournalItem> = emptyList(),
    val loading: Boolean = true,
)

class JournalViewModel(
    getJournalItemsUseCase: GetJournalItemsUseCase,
    getMeditationSessionsUseCase: GetMeditationSessionsUseCase,
    private val deleteDiaryEntryUseCase: DeleteDiaryEntryUseCase,
    private val updateDiaryEntryUseCase: UpdateDiaryEntryUseCase,
    private val deleteMeditationSessionUseCase: DeleteMeditationSessionUseCase,
    private val updateMeditationSessionUseCase: UpdateMeditationSessionUseCase,
) : ViewModel() {
    val uiState: StateFlow<JournalUiState> = combine(
        getJournalItemsUseCase(),
        getMeditationSessionsUseCase(),
    ) { diaryEntries, meditationSessions ->
        val items = diaryEntries.map { JournalItem.Diary(it) } +
            meditationSessions.map { JournalItem.Meditation(it) }
        JournalUiState(items = items.sortedByDescending { it.sortDateTime }, loading = false)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = JournalUiState(),
        )

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

    fun updateMeditationSession(localId: String, startedAt: String, endedAt: String, durationSeconds: Long, note: String?) {
        viewModelScope.launch {
            updateMeditationSessionUseCase(localId, startedAt, endedAt, durationSeconds, note)
        }
    }
}
