package com.bibo.android.features.main.presentation

import com.bibo.android.core.datastore.ThemeLocalData
import com.bibo.android.core.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow

class ObserveThemeUseCase(
    private val userPreferences: UserPreferences,
) {
    operator fun invoke(): Flow<ThemeLocalData> = userPreferences.themeData
}

class SetDarkThemeUseCase(
    private val userPreferences: UserPreferences,
) {
    suspend operator fun invoke(enabled: Boolean) {
        userPreferences.setDarkTheme(enabled)
    }
}
