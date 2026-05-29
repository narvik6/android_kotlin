package com.bibo.android.features.meditation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bibo.android.features.diary.presentation.UnsyncedMarker
import com.bibo.android.features.diary.presentation.readableDate
import com.bibo.android.features.meditation.domain.MeditationSession
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeditationScreen(
    viewModel: MeditationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<MeditationSession?>(null) }

    editing?.let { session ->
        AddEditMeditationSessionScreen(
            session = session,
            onSave = { note ->
                viewModel.updateSession(session, note)
                editing = null
            },
            onCancel = { editing = null },
        )
        return
    }

    if (uiState.awaitingConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::cancelCompletion,
            title = { Text("Завершить медитацию?") },
            text = { Text("Сохранить завершённую практику в журнал.") },
            confirmButton = {
                Button(onClick = viewModel::confirmCompletion) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelCompletion) {
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
        Text("Медитация", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = uiState.durationMinutes.toString(),
            onValueChange = { viewModel.setDurationMinutes(it.toIntOrNull() ?: 1) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Длительность, минут") },
            enabled = !uiState.active,
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::setNote,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Заметка") },
            minLines = 3,
        )
        Text(
            text = if (uiState.active) {
                "Осталось: ${uiState.remainingSeconds.formatDuration()}"
            } else {
                "Таймер не запущен"
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::start, enabled = !uiState.active) {
                Text("Старт")
            }
            OutlinedButton(onClick = viewModel::stop, enabled = uiState.active) {
                Text("Остановить")
            }
        }

        Text("Последние сессии", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.sessions.take(5), key = { it.localId }) { session ->
                MeditationSessionCard(
                    session = session,
                    onClick = { editing = session },
                )
            }
        }
    }
}

@Composable
fun AddEditMeditationSessionScreen(
    session: MeditationSession,
    onSave: (note: String?) -> Unit,
    onCancel: () -> Unit,
) {
    var note by remember(session.localId) { mutableStateOf(session.note.orEmpty()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Редактирование медитации", style = MaterialTheme.typography.headlineSmall)
        Text("Начало: ${session.startedAt.readableDate()}")
        Text("Длительность: ${session.durationSeconds.formatDuration()}")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Заметка") },
            minLines = 4,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(note) }) {
                Text("Сохранить")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Отмена")
            }
        }
    }
}

@Composable
fun MeditationSessionCard(
    session: MeditationSession,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(session.startedAt.readableDate(), style = MaterialTheme.typography.titleSmall)
                if (!session.isSynced) UnsyncedMarker(session.syncStatus)
            }
            Text("Длительность: ${session.durationSeconds.formatDuration()}")
            Text(session.note ?: "Без заметки")
            onDelete?.let {
                TextButton(onClick = it) {
                    Text("Удалить")
                }
            }
        }
    }
}

fun Long.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
