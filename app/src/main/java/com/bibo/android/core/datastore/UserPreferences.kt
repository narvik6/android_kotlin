package com.bibo.android.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
) : CurrentUserProvider {
    private val dataStore = context.userPreferencesDataStore
    private val json = Json { ignoreUnknownKeys = true }

    override val authData: Flow<AuthLocalData> = dataStore.data.map { preferences ->
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

    fun meditationDurationInput(ownerUserId: String): Flow<String> =
        dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Keys.meditationDurationInput(ownerUserId))] ?: "10"
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

    suspend fun setMeditationDurationInput(ownerUserId: String, value: String) {
        if (value.toLongOrNull()?.let { it > 0 } != true) return
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Keys.meditationDurationInput(ownerUserId))] = value
        }
    }

    fun searchHistory(ownerUserId: String): Flow<List<String>> =
        dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(Keys.searchHistory(ownerUserId))]
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
                ?: emptyList()
        }

    suspend fun addSearchHistoryItem(ownerUserId: String, query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        dataStore.edit { preferences ->
            val key = stringPreferencesKey(Keys.searchHistory(ownerUserId))
            val current = preferences[key]
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
                ?: emptyList()
            val updated = SearchHistoryPolicy.add(current, cleaned)
            preferences[key] = json.encodeToString(updated)
        }
    }

    suspend fun clearSearchHistory(ownerUserId: String) {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(Keys.searchHistory(ownerUserId)))
        }
    }

    override suspend fun currentUserId(): String? = authData.first().userId

    private object Keys {
        val Token = stringPreferencesKey("auth_token")
        val UserId = stringPreferencesKey("auth_user_id")
        val Email = stringPreferencesKey("auth_email")
        val DarkThemeEnabled = booleanPreferencesKey("dark_theme_enabled")
        fun searchHistory(ownerUserId: String): String = "search_history_$ownerUserId"
        fun meditationDurationInput(ownerUserId: String): String = "meditation_duration_input_$ownerUserId"
    }
}
