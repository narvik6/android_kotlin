package com.bibo.android.features.main.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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

private enum class MainTab(val title: String) {
    Meditation("Медитация"),
    Diary("Дневник"),
    Journal("Журнал"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    email: String,
    darkThemeEnabled: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.Meditation) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("BIBO")
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Тёмная")
                        Switch(
                            checked = darkThemeEnabled,
                            onCheckedChange = onDarkThemeChange,
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
                MainTab.Meditation -> PlaceholderTab(
                    title = "Медитация",
                    message = "Раздел медитаций будет реализован в следующей итерации.",
                    email = email,
                )
                MainTab.Diary -> PlaceholderTab(
                    title = "Дневник",
                    message = "Записи дневника появятся после реализации offline-first ядра.",
                    email = email,
                )
                MainTab.Journal -> PlaceholderTab(
                    title = "Журнал",
                    message = "Журнал будет собираться из локальных записей пользователя.",
                    email = email,
                )
            }
        }
    }
}

@Composable
private fun PlaceholderTab(
    title: String,
    message: String,
    email: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (email.isNotBlank()) {
            Text(
                text = "Пользователь: $email",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
