package com.bibo.android.features.diary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.features.diary.domain.CalculateDailyMoodUseCase
import com.bibo.android.features.diary.domain.CreateDiaryEntryUseCase
import com.bibo.android.features.diary.domain.DailyMood
import com.bibo.android.features.diary.domain.DeleteDiaryEntryUseCase
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.domain.GetDiaryEntriesUseCase
import com.bibo.android.features.diary.domain.RefreshDiaryEntriesUseCase
import com.bibo.android.features.diary.domain.UpdateDiaryEntryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiaryUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val dailyMood: DailyMood? = null,
    val loading: Boolean = true,
)

class DiaryViewModel(
    getDiaryEntriesUseCase: GetDiaryEntriesUseCase,
    private val createDiaryEntryUseCase: CreateDiaryEntryUseCase,
    private val updateDiaryEntryUseCase: UpdateDiaryEntryUseCase,
    private val deleteDiaryEntryUseCase: DeleteDiaryEntryUseCase,
    private val calculateDailyMoodUseCase: CalculateDailyMoodUseCase,
    private val refreshDiaryEntriesUseCase: RefreshDiaryEntriesUseCase,
) : ViewModel() {
    val uiState: StateFlow<DiaryUiState> = getDiaryEntriesUseCase()
        .map { entries ->
            DiaryUiState(
                entries = entries,
                dailyMood = calculateDiaryMood(entries),
                loading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DiaryUiState(),
        )

    init {
        viewModelScope.launch {
            refreshDiaryEntriesUseCase()
        }
    }

    fun createEntry(text: String?, mood: Int?) {
        viewModelScope.launch {
            createDiaryEntryUseCase(text, mood)
        }
    }

    fun updateEntry(localId: String, text: String?, mood: Int?) {
        viewModelScope.launch {
            updateDiaryEntryUseCase(localId, text, mood)
        }
    }

    fun deleteEntry(localId: String) {
        viewModelScope.launch {
            deleteDiaryEntryUseCase(localId)
        }
    }

    private fun calculateDiaryMood(entries: List<DiaryEntry>): DailyMood? {
        val todayPrefix = entries.firstOrNull()?.dateTime?.take(10) ?: return null
        return calculateDailyMoodUseCase(entries.filter { it.dateTime.startsWith(todayPrefix) })
    }
}
