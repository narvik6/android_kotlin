package com.bibo.android.features.meditation.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StartMeditationTimerUseCaseTest {
    @Test
    fun startTimerCoercesDurationToAtLeastOneSecond() {
        val snapshot = StartMeditationTimerUseCase()(durationSeconds = 0, nowMillis = 1000)

        assertEquals(1000, snapshot.startedAtMillis)
        assertEquals(1, snapshot.durationSeconds)
    }
}
