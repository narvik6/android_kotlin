package com.example.module6_t2.presentation.nobel_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.module6_t2.presentation.model.LaureateUiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NobelListScreen(
    viewModel: NobelViewModel,
    onLaureateClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var yearQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "physics", "chemistry", "medicine", "literature", "peace", "economics")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Нобелевские премии") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- БЛОК ФИЛЬТРОВ ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = yearQuery,
                    onValueChange = { yearQuery = it },
                    label = { Text("Год") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = it },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.loadPrizes(yearQuery, selectedCategory) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Фильтр")
                }
            }

            // --- БЛОК СПИСКА И СОСТОЯНИЙ ---
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val state = uiState) {
                    is NobelUiState.Loading -> CircularProgressIndicator()
                    is NobelUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadPrizes(yearQuery, selectedCategory) }) {
                                Text("Повторить")
                            }
                        }
                    }
                    is NobelUiState.Success -> {
                        if (state.laureates.isEmpty()) {
                            Text("Лауреаты не найдены")
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.laureates) { laureate ->
                                    LaureateCard(laureate, onClick = { onLaureateClick(laureate.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LaureateCard(laureate: LaureateUiItem, onClick: () -> Unit) {
    val shortMotivation = laureate.motivation.take(100)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${laureate.year} — ${laureate.category}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = laureate.fullName, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = shortMotivation,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
