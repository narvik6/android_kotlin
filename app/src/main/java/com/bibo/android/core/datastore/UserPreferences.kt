package com.bibo.android.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

data class AuthLocalData(
    val token: String?,
    val userId: String?,
    val email: String?,
) {
    val isAuthorized: Boolean = !token.isNullOrBlank() && !userId.isNullOrBlank()
}

data class ThemeLocalData(
    val darkThemeEnabled: Boolean,
)

class UserPreferences(
    context: Context,
) {
    private val dataStore = context.userPreferencesDataStore

    val authData: Flow<AuthLocalData> = dataStore.data.map { preferences ->
        AuthLocalData(
            token = preferences[Keys.Token],
            userId = preferences[Keys.UserId],
            email = preferences[Keys.Email],
        )
    }

    val themeData: Flow<ThemeLocalData> = dataStore.data.map { preferences ->
        ThemeLocalData(
            darkThemeEnabled = preferences[Keys.DarkThemeEnabled] ?: false,
        )
    }

    suspend fun saveAuth(token: String, userId: String, email: String) {
        dataStore.edit { preferences ->
            preferences[Keys.Token] = token
            preferences[Keys.UserId] = userId
            preferences[Keys.Email] = email
        }
    }

    suspend fun clearAuth() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.Token)
            preferences.remove(Keys.UserId)
            preferences.remove(Keys.Email)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DarkThemeEnabled] = enabled
        }
    }

    private object Keys {
        val Token = stringPreferencesKey("auth_token")
        val UserId = stringPreferencesKey("auth_user_id")
        val Email = stringPreferencesKey("auth_email")
        val DarkThemeEnabled = booleanPreferencesKey("dark_theme_enabled")
    }
}
