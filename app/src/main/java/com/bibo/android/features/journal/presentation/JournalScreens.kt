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
import org.koin.androidx.compose.koinViewModel

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<DiaryEntry?>(null) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }

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

    selected?.let { entry ->
        JournalDetailsScreen(
            entry = entry,
            onBack = { selected = null },
            onEdit = { editing = entry },
            onDelete = {
                viewModel.deleteDiaryEntry(entry.localId)
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
                items(uiState.items, key = { it.localId }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onClick = { selected = entry },
                        onDelete = { viewModel.deleteDiaryEntry(entry.localId) },
                    )
                }
            }
        }
    }
}

@Composable
fun JournalDetailsScreen(
    entry: DiaryEntry,
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
            Text("Запись дневника", style = MaterialTheme.typography.headlineSmall)
            if (!entry.isSynced) UnsyncedMarker(entry.syncStatus)
        }
        Text("Дата: ${entry.dateTime.readableDate()}")
        Text("Настроение: ${entry.mood.emoji()}")
        Text(entry.text ?: "Без текста", style = MaterialTheme.typography.bodyLarge)
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
