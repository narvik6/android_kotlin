package com.bibo.android.features.main.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bibo.android.features.diary.presentation.DiaryScreen
import com.bibo.android.features.diary.presentation.PendingSyncMarker
import com.bibo.android.features.journal.presentation.JournalScreen
import com.bibo.android.features.meditation.presentation.MeditationScreen

private enum class MainTab(val title: String) {
    Meditation("Медитация"),
    Diary("Дневник"),
    Journal("Журнал"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userScopeKey: String,
    darkThemeEnabled: Boolean,
    serverAvailable: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.Meditation) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("BIBO")
                        if (!serverAvailable) {
                            PendingSyncMarker()
                            Text("offline", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        ThemeToggleButton(
                            darkThemeEnabled = darkThemeEnabled,
                            onToggle = { onDarkThemeChange(!darkThemeEnabled) },
                        )
                        TextButton(onClick = onLogout) {
                            Text("Выйти")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.title) },
                        icon = {},
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                MainTab.Meditation -> MeditationScreen(
                    userScopeKey = userScopeKey,
                )
                MainTab.Diary -> DiaryScreen(
                    userScopeKey = userScopeKey,
                )
                MainTab.Journal -> JournalScreen(
                    userScopeKey = userScopeKey,
                )
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    darkThemeEnabled: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Text(
            text = if (darkThemeEnabled) "☾" else "☀",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
