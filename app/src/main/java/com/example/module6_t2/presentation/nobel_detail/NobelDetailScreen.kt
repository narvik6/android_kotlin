package com.example.module6_t2.presentation.nobel_detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.module6_t2.presentation.model.LaureateUiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NobelDetailScreen(
    laureate: LaureateUiItem?,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали премии") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (laureate == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Информация не найдена")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Год: ${laureate.year}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Категория: ${laureate.category}", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = laureate.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    laureate.birthCountry?.let { country ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Страна: $country", style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Мотивация:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = laureate.motivation, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}