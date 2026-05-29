@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bibo.android.features.meditation.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.features.meditation.domain.FinishMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.GetMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.MeditationSession
import com.bibo.android.features.meditation.domain.StartMeditationTimerUseCase
import com.bibo.android.features.meditation.domain.SyncMeditationPendingChangesUseCase
import com.bibo.android.features.meditation.domain.UpdateMeditationSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class MeditationUiState(
    val sessions: List<MeditationSession> = emptyList(),
    val active: Boolean = false,
    val durationMinutes: Int = 10,
    val remainingSeconds: Long = 0,
    val note: String = "",
    val awaitingConfirmation: Boolean = false,
    val startedAtMillis: Long? = null,
    val completedDurationSeconds: Long = 0,
)

class MeditationViewModel(
    getMeditationSessionsUseCase: GetMeditationSessionsUseCase,
    private val startMeditationTimerUseCase: StartMeditationTimerUseCase,
    private val finishMeditationSessionUseCase: FinishMeditationSessionUseCase,
    private val updateMeditationSessionUseCase: UpdateMeditationSessionUseCase,
    private val syncMeditationPendingChangesUseCase: SyncMeditationPendingChangesUseCase,
    private val appContext: Context,
) : ViewModel() {
    private val timerState = MutableStateFlow(MeditationUiState())
    private var ticker: Job? = null

    val uiState: StateFlow<MeditationUiState> = combine(
        getMeditationSessionsUseCase(),
        timerState,
    ) { sessions, timer ->
        timer.copy(sessions = sessions)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeditationUiState())

    init {
        viewModelScope.launch {
            syncMeditationPendingChangesUseCase()
        }
    }

    fun setDurationMinutes(value: Int) {
        timerState.update { it.copy(durationMinutes = value.coerceIn(1, 120)) }
    }

    fun setNote(value: String) {
        timerState.update { it.copy(note = value) }
    }

    fun start() {
        if (timerState.value.active) return
        val durationSeconds = timerState.value.durationMinutes * 60L
        val snapshot = startMeditationTimerUseCase(durationSeconds, System.currentTimeMillis())
        timerState.update {
            it.copy(
                active = true,
                remainingSeconds = snapshot.durationSeconds,
                awaitingConfirmation = false,
                startedAtMillis = snapshot.startedAtMillis,
                completedDurationSeconds = 0,
            )
        }
        MeditationTimerService.start(appContext, durationSeconds)
        startTicker()
    }

    fun stop() {
        completeTimer()
    }

    fun confirmCompletion() {
        val state = timerState.value
        val startedAtMillis = state.startedAtMillis ?: return
        val endedAtMillis = System.currentTimeMillis()
        val completedSeconds = state.completedDurationSeconds.takeIf { it > 0 }
            ?: ((endedAtMillis - startedAtMillis) / 1000).coerceAtLeast(0)
        viewModelScope.launch {
            finishMeditationSessionUseCase(
                startedAt = startedAtMillis.toIsoString(),
                endedAt = endedAtMillis.toIsoString(),
                durationSeconds = completedSeconds,
                note = state.note,
            )
            timerState.update {
                it.copy(
                    active = false,
                    remainingSeconds = 0,
                    note = "",
                    awaitingConfirmation = false,
                    startedAtMillis = null,
                    completedDurationSeconds = 0,
                )
            }
        }
    }

    fun cancelCompletion() {
        timerState.update {
            it.copy(awaitingConfirmation = false, startedAtMillis = null, completedDurationSeconds = 0)
        }
    }

    fun updateSession(session: MeditationSession, note: String?) {
        viewModelScope.launch {
            updateMeditationSessionUseCase(
                localId = session.localId,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                durationSeconds = session.durationSeconds,
                note = note,
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (timerState.value.active) {
                val state = timerState.value
                val startedAtMillis = state.startedAtMillis ?: break
                val elapsedSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000).coerceAtLeast(0)
                val totalSeconds = state.durationMinutes * 60L
                val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0)
                timerState.update { it.copy(remainingSeconds = remaining) }
                if (remaining == 0L) {
                    completeTimer()
                    break
                }
                delay(1_000)
            }
        }
    }

    private fun completeTimer() {
        val state = timerState.value
        val startedAtMillis = state.startedAtMillis ?: return
        ticker?.cancel()
        MeditationTimerService.stop(appContext)
        val elapsedSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000).coerceAtLeast(0)
        timerState.update {
            it.copy(
                active = false,
                remainingSeconds = 0,
                awaitingConfirmation = true,
                completedDurationSeconds = elapsedSeconds,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ticker?.cancel()
    }
}

private fun Long.toIsoString(): String =
    kotlin.time.Instant.fromEpochMilliseconds(this).toString()
