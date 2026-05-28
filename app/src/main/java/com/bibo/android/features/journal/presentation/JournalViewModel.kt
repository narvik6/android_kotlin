package com.bibo.android.features.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.features.diary.domain.DeleteDiaryEntryUseCase
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.domain.GetJournalItemsUseCase
import com.bibo.android.features.diary.domain.UpdateDiaryEntryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalUiState(
    val items: List<DiaryEntry> = emptyList(),
    val loading: Boolean = true,
)

class JournalViewModel(
    getJournalItemsUseCase: GetJournalItemsUseCase,
    private val deleteDiaryEntryUseCase: DeleteDiaryEntryUseCase,
    private val updateDiaryEntryUseCase: UpdateDiaryEntryUseCase,
) : ViewModel() {
    val uiState: StateFlow<JournalUiState> = getJournalItemsUseCase()
        .map { JournalUiState(items = it, loading = false) }
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
}
