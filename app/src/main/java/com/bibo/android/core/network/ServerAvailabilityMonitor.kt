package com.bibo.android.core.network

import com.bibo.android.core.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerAvailabilityMonitor(
    private val apiClient: BiboApi,
) {
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    suspend fun refresh(): Boolean {
        val available = apiClient.checkHealth() is AppResult.Success
        _available.value = available
        return available
    }

    fun markAvailable() {
        _available.value = true
    }

    fun markUnavailable() {
        _available.value = false
    }
}
