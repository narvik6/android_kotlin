package com.bibo.android.features.journal.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bibo.android.core.ui.SearchPanel
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.presentation.AddEditDiaryEntryScreen
import com.bibo.android.features.diary.presentation.DiaryEntryCard
import com.bibo.android.features.diary.presentation.PendingSyncMarker
import com.bibo.android.features.diary.presentation.emoji
import com.bibo.android.features.diary.presentation.readableDate
import com.bibo.android.features.journal.domain.JournalItem
import com.bibo.android.features.meditation.domain.MeditationSession
import com.bibo.android.features.meditation.presentation.AddEditMeditationSessionScreen
import com.bibo.android.features.meditation.presentation.MeditationSessionCard
import com.bibo.android.features.meditation.presentation.formatDuration
import org.koin.androidx.compose.koinViewModel

@Composable
fun JournalScreen(
    userScopeKey: String,
    viewModel: JournalViewModel = koinViewModel(key = "journal-$userScopeKey"),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingMeditation by remember { mutableStateOf<MeditationSession?>(null) }
    val selected = selectedId?.let { id -> uiState.items.firstOrNull { it.id == id } }

    LaunchedEffect(selectedId, selected) {
        if (selectedId != null && selected == null) {
            selectedId = null
        }
    }

    editing?.let { entry ->
        BackHandler {
            editing = null
            selectedId = null
        }
        AddEditDiaryEntryScreen(
            entry = entry,
            operationsEnabled = true,
            onSave = { text, mood ->
                viewModel.updateDiaryEntry(entry.localId, text, mood)
                editing = null
                selectedId = null
            },
            onCancel = { editing = null },
        )
        return
    }

    editingMeditation?.let { session ->
        BackHandler {
            editingMeditation = null
            selectedId = null
        }
        AddEditMeditationSessionScreen(
            session = session,
            operationsEnabled = true,
            onSave = { note ->
                viewModel.updateMeditationSession(
                    localId = session.localId,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    durationSeconds = session.durationSeconds,
                    note = note,
                )
                editingMeditation = null
                selectedId = null
            },
            onCancel = { editingMeditation = null },
        )
        return
    }

    selected?.let { item ->
        BackHandler {
            selectedId = null
        }
        JournalDetailsScreen(
            item = item,
            operationsEnabled = true,
            onBack = { selectedId = null },
            onEdit = {
                when (item) {
                    is JournalItem.Diary -> editing = item.entry
                    is JournalItem.Meditation -> editingMeditation = item.session
                }
            },
            onDelete = {
                viewModel.deleteItem(item)
                selectedId = null
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Журнал", style = MaterialTheme.typography.headlineSmall)
        SearchPanel(
            query = uiState.query,
            onQueryChange = viewModel::setQuery,
            hint = "Поиск по журналу",
            history = uiState.history,
            loading = uiState.loading,
            empty = uiState.query.isNotBlank() && uiState.items.isEmpty(),
            error = uiState.error,
            onClearHistory = viewModel::clearSearchHistory,
            onHistoryClick = viewModel::setQuery,
            onRefresh = viewModel::refreshSearch,
        )
        when {
            uiState.loading -> Unit
            uiState.items.isEmpty() && uiState.query.isBlank() -> Text(
                text = "В журнале пока нет записей",
            )
            uiState.items.isEmpty() -> Unit
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    when (item) {
                        is JournalItem.Diary -> DiaryEntryCard(
                            entry = item.entry,
                            onClick = {
                                viewModel.addCurrentQueryToHistory()
                                selectedId = item.id
                            },
                            onDelete = { viewModel.deleteItem(item) },
                            showActions = false,
                            operationsEnabled = true,
                            contentMaxLines = 3,
                        )
                        is JournalItem.Meditation -> MeditationSessionCard(
                            session = item.session,
                            onClick = {
                                viewModel.addCurrentQueryToHistory()
                                selectedId = item.id
                            },
                            onDelete = { viewModel.deleteItem(item) },
                            showActions = false,
                            operationsEnabled = true,
                            contentMaxLines = 3,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalDetailsScreen(
    item: JournalItem,
    operationsEnabled: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var deleteConfirmation by remember { mutableStateOf(false) }

    if (deleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteConfirmation = false },
            title = { Text("Удалить элемент?") },
            text = { Text("Элемент исчезнет из журнала.") },
            confirmButton = {
                Button(onClick = onDelete, enabled = operationsEnabled) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(item.title(), style = MaterialTheme.typography.headlineSmall)
            if (item.hasPendingOperation()) {
                PendingSyncMarker()
            }
        }
        when (item) {
            is JournalItem.Diary -> {
                Text("Дата: ${item.entry.dateTime.readableDate()}")
                item.entry.mood?.let {
                    Text("Настроение: ${it.emoji()}")
                }
                Text(item.entry.text ?: "Без текста", style = MaterialTheme.typography.bodyLarge)
            }
            is JournalItem.Meditation -> {
                Text("Начало: ${item.session.startedAt.readableDate()}")
                Text("Длительность: ${item.session.durationSeconds.formatDuration()}")
                Text(item.session.note ?: "Без заметки", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
            OutlinedButton(onClick = onEdit, enabled = operationsEnabled) {
                Text("Редактировать")
            }
            Button(onClick = { deleteConfirmation = true }, enabled = operationsEnabled) {
                Text("Удалить")
            }
        }
    }
}

private fun JournalItem.title(): String = when (this) {
    is JournalItem.Diary -> "Запись дневника"
    is JournalItem.Meditation -> "Медитация"
}

private fun JournalItem.hasPendingOperation(): Boolean = when (this) {
    is JournalItem.Diary -> entry.pendingOperation != null
    is JournalItem.Meditation -> session.pendingOperation != null
}
