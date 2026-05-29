package com.bibo.android.features.journal.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.presentation.AddEditDiaryEntryScreen
import com.bibo.android.features.diary.presentation.DiaryEntryCard
import com.bibo.android.features.diary.presentation.UnsyncedMarker
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
    viewModel: JournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<JournalItem?>(null) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingMeditation by remember { mutableStateOf<MeditationSession?>(null) }

    editing?.let { entry ->
        AddEditDiaryEntryScreen(
            entry = entry,
            onSave = { text, mood ->
                viewModel.updateDiaryEntry(entry.localId, text, mood)
                editing = null
                selected = null
            },
            onCancel = { editing = null },
        )
        return
    }

    editingMeditation?.let { session ->
        AddEditMeditationSessionScreen(
            session = session,
            onSave = { note ->
                viewModel.updateMeditationSession(
                    localId = session.localId,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    durationSeconds = session.durationSeconds,
                    note = note,
                )
                editingMeditation = null
                selected = null
            },
            onCancel = { editingMeditation = null },
        )
        return
    }

    selected?.let { item ->
        JournalDetailsScreen(
            item = item,
            onBack = { selected = null },
            onEdit = {
                when (item) {
                    is JournalItem.Diary -> editing = item.entry
                    is JournalItem.Meditation -> editingMeditation = item.session
                }
            },
            onDelete = {
                when (item) {
                    is JournalItem.Diary -> viewModel.deleteDiaryEntry(item.entry.localId)
                    is JournalItem.Meditation -> viewModel.deleteMeditationSession(item.session.localId)
                }
                selected = null
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("Журнал", style = MaterialTheme.typography.headlineSmall)
        when {
            uiState.loading -> CircularProgressIndicator()
            uiState.items.isEmpty() -> Text(
                text = "В журнале пока нет записей",
                modifier = Modifier.padding(top = 16.dp),
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    when (item) {
                        is JournalItem.Diary -> DiaryEntryCard(
                            entry = item.entry,
                            onClick = { selected = item },
                            onDelete = { viewModel.deleteDiaryEntry(item.entry.localId) },
                        )
                        is JournalItem.Meditation -> MeditationSessionCard(
                            session = item.session,
                            onClick = { selected = item },
                            onDelete = { viewModel.deleteMeditationSession(item.session.localId) },
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
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
            item.unsyncedStatus()?.let { UnsyncedMarker(it) }
        }
        when (item) {
            is JournalItem.Diary -> {
                Text("Дата: ${item.entry.dateTime.readableDate()}")
                Text("Настроение: ${item.entry.mood.emoji()}")
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
            OutlinedButton(onClick = onEdit) {
                Text("Редактировать")
            }
            Button(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}

private fun JournalItem.title(): String = when (this) {
    is JournalItem.Diary -> "Запись дневника"
    is JournalItem.Meditation -> "Медитация"
}

private fun JournalItem.unsyncedStatus() = when (this) {
    is JournalItem.Diary -> entry.syncStatus.takeUnless { entry.isSynced }
    is JournalItem.Meditation -> session.syncStatus.takeUnless { session.isSynced }
}
