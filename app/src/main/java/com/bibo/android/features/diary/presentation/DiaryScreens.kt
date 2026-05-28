package com.bibo.android.features.diary.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bibo.android.core.database.SyncStatus
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.domain.MoodTrend
import org.koin.androidx.compose.koinViewModel

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var adding by remember { mutableStateOf(false) }

    if (adding || editingEntry != null) {
        AddEditDiaryEntryScreen(
            entry = editingEntry,
            onSave = { text, mood ->
                val entry = editingEntry
                if (entry == null) {
                    viewModel.createEntry(text, mood)
                } else {
                    viewModel.updateEntry(entry.localId, text, mood)
                }
                adding = false
                editingEntry = null
            },
            onCancel = {
                adding = false
                editingEntry = null
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Дневник", style = MaterialTheme.typography.headlineSmall)
                uiState.dailyMood?.let { mood ->
                    Text(
                        text = "Настроение дня: ${mood.emoji} ${mood.trend.label()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(onClick = { adding = true }) {
                Text("Добавить")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.loading -> CircularProgressIndicator()
            uiState.entries.isEmpty() -> EmptyText("Записей пока нет")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.entries, key = { it.localId }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onClick = { editingEntry = entry },
                        onDelete = { viewModel.deleteEntry(entry.localId) },
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditDiaryEntryScreen(
    entry: DiaryEntry?,
    onSave: (text: String?, mood: Int?) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember(entry?.localId) { mutableStateOf(entry?.text.orEmpty()) }
    var mood by remember(entry?.localId) { mutableStateOf(entry?.mood) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (entry == null) "Новая запись" else "Редактирование записи",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            label = { Text("Текст записи") },
        )
        Text("Настроение", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MoodButton(":)", 1, mood) { mood = it }
            MoodButton("._.", 0, mood) { mood = it }
            MoodButton(":(", -1, mood) { mood = it }
            OutlinedButton(onClick = { mood = null }) {
                Text("Без оценки")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(text, mood) }) {
                Text("Сохранить")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Отмена")
            }
        }
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(entry.dateTime.readableDate(), style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!entry.isSynced) UnsyncedMarker(entry.syncStatus)
                    Text(entry.mood.emoji(), modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(entry.text ?: "Без текста", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClick) {
                    Text("Редактировать")
                }
                TextButton(onClick = onDelete) {
                    Text("Удалить")
                }
            }
        }
    }
}

@Composable
fun UnsyncedMarker(status: SyncStatus) {
    Text(
        text = if (status == SyncStatus.ERROR) "Ошибка синхронизации" else "Не синхронизировано",
        modifier = Modifier
            .background(Color(0xFFFFE082), shape = MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color(0xFF4E3B00),
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun MoodButton(
    label: String,
    value: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    if (selected == value) {
        Button(onClick = { onSelect(value) }) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = { onSelect(value) }) {
            Text(label)
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

fun Int?.emoji(): String = when (this) {
    1 -> ":)"
    0 -> "._."
    -1 -> ":("
    else -> "Без оценки"
}

fun String.readableDate(): String = replace("T", " ").removeSuffix("Z")

private fun MoodTrend.label(): String = when (this) {
    MoodTrend.Better -> "↑"
    MoodTrend.Worse -> "↓"
    MoodTrend.Same -> ""
}
