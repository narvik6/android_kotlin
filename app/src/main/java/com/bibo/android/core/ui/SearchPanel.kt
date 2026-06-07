package com.bibo.android.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    history: List<String>,
    loading: Boolean,
    empty: Boolean,
    error: String?,
    onClearHistory: () -> Unit,
    onHistoryClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(hint) },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Text(
                        text = "Очистить",
                        modifier = Modifier
                            .clickable {
                                onQueryChange("")
                                focusManager.clearFocus()
                            }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("История", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onClearHistory) {
                    Text("Очистить историю")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                history.forEach { item ->
                    AssistChip(
                        onClick = { onHistoryClick(item) },
                        label = { Text(item) },
                    )
                }
            }
        }

        when {
            loading -> CircularProgressIndicator()
            error != null -> Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRefresh) {
                    Text("Обновить")
                }
            }
            empty -> Text("Ничего не найдено", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
