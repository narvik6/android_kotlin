package com.example.module5_t1to3.diary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun DiaryApp(viewModel: DiaryViewModel = viewModel()) {
    val navigation by viewModel.navigation.collectAsStateWithLifecycle()
    val entries    by viewModel.entries.collectAsStateWithLifecycle()

    AnimatedContent(targetState = navigation, label = "diary_nav") { nav ->
        when (nav) {
            is DiaryNavigation.List -> DiaryListScreen(
                entries       = entries,
                onNewEntry    = viewModel::openNewEntry,
                onEntryClick  = viewModel::openEntry,
                onDeleteEntry = viewModel::deleteEntry
            )
            is DiaryNavigation.NewEntry -> DiaryEditorScreen(
                initialTitle = "",
                initialText  = "",
                screenTitle  = "Новая запись",
                onSave       = viewModel::saveNewEntry,
                onBack       = viewModel::navigateBack
            )
            is DiaryNavigation.EditEntry -> DiaryEditorScreen(
                initialTitle = nav.title,
                initialText  = nav.text,
                screenTitle  = "Редактировать запись",
                onSave       = { t, b -> viewModel.updateEntry(nav.fileName, t, b) },
                onBack       = viewModel::navigateBack
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    entries       : List<DiaryEntry>,
    onNewEntry    : () -> Unit,
    onEntryClick  : (DiaryEntry) -> Unit,
    onDeleteEntry : (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Мой дневник") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewEntry) {
                Icon(Icons.Default.Add, contentDescription = "Новая запись")
            }
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            EmptyDiaryPlaceholder(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier        = Modifier.padding(innerPadding),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.fileName }) { entry ->
                    DiaryEntryCard(
                        entry         = entry,
                        onClick       = { onEntryClick(entry) },
                        onDeleteClick = { onDeleteEntry(entry.fileName)  },
                        modifier      = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryEntryCard(
    entry         : DiaryEntry,
    onClick       : () -> Unit,
    onDeleteClick : () -> Unit,
    modifier      : Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick     = onClick,
                    onLongClick = { menuExpanded = true }
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (entry.title.isNotBlank()) {
                    Text(
                        text     = entry.title,
                        style    = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text     = entry.preview.ifBlank { "(пустая запись)" },
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = entry.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        DropdownMenu(
            expanded         = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text    = { Text("Удалить") },
                onClick = {
                    menuExpanded = false
                    onDeleteClick()
                }
            )
        }
    }
}

@Composable
private fun EmptyDiaryPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "У вас пока нет записей",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Нажмите +, чтобы создать первую",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditorScreen(
    initialTitle : String,
    initialText  : String,
    screenTitle  : String,
    onSave       : (title: String, text: String) -> Unit,
    onBack       : () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var text  by remember { mutableStateOf(initialText) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(title, text) },
                        enabled = text.isNotBlank()
                    ) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Заголовок (необязательно)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                label         = { Text("Текст записи") },
                placeholder   = { Text("Напишите что-нибудь…") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
