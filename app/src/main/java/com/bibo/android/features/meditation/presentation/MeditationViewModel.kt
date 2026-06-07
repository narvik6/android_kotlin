@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.bibo.android.features.meditation.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.features.meditation.domain.FinishMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.GetMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.MeditationSession
import com.bibo.android.features.meditation.domain.RefreshMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.StartMeditationTimerUseCase
import com.bibo.android.features.meditation.domain.UpdateMeditationSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class MeditationUiState(
    val sessions: List<MeditationSession> = emptyList(),
    val active: Boolean = false,
    val paused: Boolean = false,
    val durationInput: String = "",
    val durationInputLoaded: Boolean = false,
    val totalDurationSeconds: Long = 600,
    val segmentDurationSeconds: Long = 600,
    val remainingSeconds: Long = 0,
    val note: String = "",
    val awaitingConfirmation: Boolean = false,
    val startedAtMillis: Long? = null,
    val sessionStartedAtMillis: Long? = null,
    val completedDurationSeconds: Long = 0,
) {
    val canStart: Boolean = !active &&
        !awaitingConfirmation &&
        durationInputLoaded &&
        (paused || durationInput.toLongOrNull()?.let { it > 0 } == true)
    val canConfirm: Boolean = paused || awaitingConfirmation
    val canEditDuration: Boolean = !active && !paused && !awaitingConfirmation
    val progress: Float =
        if (totalDurationSeconds <= 0L) 0f else remainingSeconds.toFloat() / totalDurationSeconds.toFloat()
}

class MeditationViewModel(
    getMeditationSessionsUseCase: GetMeditationSessionsUseCase,
    private val startMeditationTimerUseCase: StartMeditationTimerUseCase,
    private val finishMeditationSessionUseCase: FinishMeditationSessionUseCase,
    private val updateMeditationSessionUseCase: UpdateMeditationSessionUseCase,
    private val refreshMeditationSessionsUseCase: RefreshMeditationSessionsUseCase,
    private val userPreferences: UserPreferences,
    private val appContext: Context,
) : ViewModel() {
    private val timerState = MutableStateFlow(MeditationUiState())
    private var ticker: Job? = null
    private var durationInputEditedByUser = false
    private var currentOwnerUserId: String? = null

    val uiState: StateFlow<MeditationUiState> = combine(
        getMeditationSessionsUseCase(),
        timerState,
    ) { sessions, timer ->
        timer.copy(sessions = sessions)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeditationUiState())

    init {
        viewModelScope.launch {
            refreshMeditationSessionsUseCase()
        }
        viewModelScope.launch {
            userPreferences.authData
                .distinctUntilChanged { old, new -> old.userId == new.userId }
                .collect { auth ->
                    currentOwnerUserId = auth.userId
                    durationInputEditedByUser = false
                    timerState.update {
                        if (it.canEditDuration) {
                            it.copy(durationInput = "", durationInputLoaded = false)
                        } else {
                            it.copy(durationInputLoaded = false)
                        }
                    }
                    val ownerUserId = auth.userId
                    val savedDurationInput = if (ownerUserId == null) {
                        "10"
                    } else {
                        userPreferences.meditationDurationInput(ownerUserId).first()
                    }
                    timerState.update {
                        if (!durationInputEditedByUser && it.canEditDuration) {
                            it.copy(
                                durationInput = savedDurationInput,
                                durationInputLoaded = true,
                            )
                        } else {
                            it.copy(durationInputLoaded = true)
                        }
                    }
                }
        }
    }

    fun setDurationInput(value: String) {
        if (value.all { it.isDigit() }) {
            durationInputEditedByUser = true
            timerState.update { it.copy(durationInput = value) }
        }
    }

    fun setNote(value: String) {
        timerState.update { it.copy(note = value) }
    }

    fun start() {
        if (timerState.value.active) return
        val state = timerState.value
        val durationMinutes = state.durationInput.toLongOrNull()?.takeIf { it > 0 } ?: return
        if (!state.paused) {
            val ownerUserId = currentOwnerUserId
            viewModelScope.launch {
                if (ownerUserId != null) {
                    userPreferences.setMeditationDurationInput(ownerUserId, state.durationInput)
                }
            }
        }
        val durationSeconds = if (state.paused && state.remainingSeconds > 0) {
            state.remainingSeconds
        } else {
            durationMinutes * 60L
        }
        val nowMillis = System.currentTimeMillis()
        val snapshot = startMeditationTimerUseCase(durationSeconds, nowMillis)
        timerState.update {
            it.copy(
                active = true,
                paused = false,
                totalDurationSeconds = if (state.paused) state.totalDurationSeconds else durationSeconds,
                segmentDurationSeconds = durationSeconds,
                remainingSeconds = snapshot.durationSeconds,
                awaitingConfirmation = false,
                startedAtMillis = snapshot.startedAtMillis,
                sessionStartedAtMillis = state.sessionStartedAtMillis ?: nowMillis,
                completedDurationSeconds = if (state.paused) state.completedDurationSeconds else 0,
            )
        }
        MeditationTimerService.start(appContext, durationSeconds)
        startTicker()
    }

    fun pause() {
        val state = timerState.value
        if (!state.active) return
        val segmentStartedAtMillis = state.startedAtMillis ?: return
        val segmentElapsedSeconds = ((System.currentTimeMillis() - segmentStartedAtMillis) / 1000)
            .coerceAtLeast(0)
        ticker?.cancel()
        MeditationTimerService.stop(appContext)
        timerState.update {
            it.copy(
                active = false,
                paused = true,
                startedAtMillis = null,
                completedDurationSeconds = it.completedDurationSeconds + segmentElapsedSeconds,
            )
        }
    }

    fun reset() {
        ticker?.cancel()
        MeditationTimerService.stop(appContext)
        timerState.update {
            it.copy(
                active = false,
                paused = false,
                remainingSeconds = 0,
                segmentDurationSeconds = it.totalDurationSeconds,
                awaitingConfirmation = false,
                startedAtMillis = null,
                sessionStartedAtMillis = null,
                completedDurationSeconds = 0,
            )
        }
    }

    fun confirmCompletion() {
        val state = timerState.value
        val startedAtMillis = state.sessionStartedAtMillis ?: return
        val endedAtMillis = System.currentTimeMillis()
        val completedSeconds = if (state.paused) {
            state.completedDurationSeconds
        } else {
            state.completedDurationSeconds.takeIf { it > 0 } ?: state.totalDurationSeconds
        }
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
                    paused = false,
                    awaitingConfirmation = false,
                    startedAtMillis = null,
                    sessionStartedAtMillis = null,
                    completedDurationSeconds = 0,
                )
            }
        }
    }

    fun cancelCompletion() {
        timerState.update {
            it.copy(
                awaitingConfirmation = false,
                startedAtMillis = null,
                sessionStartedAtMillis = null,
                completedDurationSeconds = 0,
            )
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
                val remaining = (state.segmentDurationSeconds - elapsedSeconds).coerceAtLeast(0)
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
        val startedAtMillis = state.sessionStartedAtMillis ?: state.startedAtMillis ?: return
        ticker?.cancel()
        MeditationTimerService.stop(appContext)
        val elapsedSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000).coerceAtLeast(0)
        val completedSeconds = (state.completedDurationSeconds + elapsedSeconds)
            .coerceAtMost(state.totalDurationSeconds)
        timerState.update {
            it.copy(
                active = false,
                remainingSeconds = 0,
                awaitingConfirmation = true,
                completedDurationSeconds = completedSeconds,
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
