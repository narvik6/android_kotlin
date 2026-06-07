package com.bibo.android.features.diary.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.diary.domain.MoodTrend
import org.koin.androidx.compose.koinViewModel

@Composable
fun DiaryScreen(
    userScopeKey: String,
    viewModel: DiaryViewModel = koinViewModel(key = "diary-$userScopeKey"),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Дневник", style = MaterialTheme.typography.headlineSmall)
        uiState.dailyMood?.let { mood ->
            Text(
                text = "Настроение дня: ${mood.emoji} ${mood.trend.label()}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AddEditDiaryEntryScreen(
            entry = null,
            operationsEnabled = true,
            onSave = { text, mood ->
                viewModel.createEntry(text, mood)
                Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
            },
            onCancel = {},
        )
    }
}

@Composable
fun AddEditDiaryEntryScreen(
    entry: DiaryEntry?,
    operationsEnabled: Boolean,
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
                Text("Очистить")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSave(text, mood)
                    if (entry == null) {
                        text = ""
                        mood = null
                    }
                },
                enabled = operationsEnabled,
            ) {
                Text("Сохранить")
            }
            if (entry != null) {
                OutlinedButton(onClick = onCancel) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean = true,
    operationsEnabled: Boolean = true,
    contentMaxLines: Int = Int.MAX_VALUE,
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
                    if (entry.pendingOperation != null) {
                        PendingSyncMarker()
                    }
                    entry.mood?.let {
                        Text(it.emoji(), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Text(
                text = entry.text ?: "Без текста",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = contentMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (showActions) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClick, enabled = operationsEnabled) {
                        Text("Редактировать")
                    }
                    TextButton(onClick = onDelete, enabled = operationsEnabled) {
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

@Composable
fun PendingSyncMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = 8.dp)
            .size(10.dp)
            .background(Color(0xFFFFC107), CircleShape),
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

fun Int?.emoji(): String = when (this) {
    1 -> ":)"
    0 -> "._."
    -1 -> ":("
    else -> ""
}

fun String.readableDate(): String {
    val withoutZone = removeSuffix("Z")
    val withoutFraction = withoutZone.substringBefore(".")
    return withoutFraction.replace("T", " ").take(16)
}

private fun MoodTrend.label(): String = when (this) {
    MoodTrend.Better -> "↑"
    MoodTrend.Worse -> "↓"
    MoodTrend.Same -> ""
}
