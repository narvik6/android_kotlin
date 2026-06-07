package com.bibo.android.features.meditation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bibo.android.features.diary.presentation.PendingSyncMarker
import com.bibo.android.features.diary.presentation.readableDate
import com.bibo.android.features.meditation.domain.MeditationSession
import org.koin.androidx.compose.koinViewModel

@Composable
fun MeditationScreen(
    userScopeKey: String,
    viewModel: MeditationViewModel = koinViewModel(key = "meditation-$userScopeKey"),
) {
    val uiState by viewModel.uiState.collectAsState()
    var resetRequested by remember { mutableStateOf(false) }

    if (resetRequested) {
        AlertDialog(
            onDismissRequest = { resetRequested = false },
            title = { Text("Сбросить медитацию?") },
            text = { Text("Текущая практика не будет сохранена.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reset()
                        resetRequested = false
                    },
                ) {
                    Text("Сброс")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetRequested = false }) {
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
        if (!uiState.durationInputLoaded) {
            CircularProgressIndicator()
            return@Column
        }
        OutlinedTextField(
            value = uiState.durationInput,
            onValueChange = viewModel::setDurationInput,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Длительность, минут") },
            enabled = uiState.canEditDuration,
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::setNote,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Заметка") },
            minLines = 3,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimerDot(progress = uiState.progress)
            Button(
                onClick = viewModel::confirmCompletion,
                enabled = uiState.canConfirm,
            ) {
                Text("Подтвердить")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::start, enabled = uiState.canStart) {
                Text("Старт")
            }
            OutlinedButton(onClick = viewModel::pause, enabled = uiState.active) {
                Text("Пауза")
            }
            OutlinedButton(
                onClick = { resetRequested = true },
                enabled = uiState.active || uiState.paused || uiState.awaitingConfirmation,
            ) {
                Text("Сброс")
            }
        }
    }
}

@Composable
private fun TimerDot(
    progress: Float,
    size: Dp = 42.dp,
) {
    CircularProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .padding(top = 2.dp)
            .size(size),
        strokeWidth = 3.dp,
    )
}

@Composable
fun AddEditMeditationSessionScreen(
    session: MeditationSession,
    operationsEnabled: Boolean,
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
            Button(onClick = { onSave(note) }, enabled = operationsEnabled) {
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
    showActions: Boolean = true,
    operationsEnabled: Boolean = true,
    contentMaxLines: Int = Int.MAX_VALUE,
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
                if (session.pendingOperation != null) {
                    PendingSyncMarker()
                }
            }
            Text("Длительность: ${session.durationSeconds.formatDuration()}")
            Text(
                text = session.note ?: "Без заметки",
                maxLines = contentMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (showActions) onDelete?.let {
                TextButton(onClick = it, enabled = operationsEnabled) {
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
