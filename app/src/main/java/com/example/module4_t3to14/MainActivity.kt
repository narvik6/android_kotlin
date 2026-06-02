package com.example.module4_t3to14

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.module4_t3to14.ui.theme.Module4_t3to14Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Module4_t3to14Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskMenu()
                }
            }
        }
    }
}

private data class TaskFeature(
    val title: String,
    val description: String,
    val activityClass: Class<*>
)

private val taskFeatures = listOf(
    TaskFeature("Задание 3", "Поиск репозиториев GitHub с debounce", com.example.module4_t3to14.task3.MainActivity::class.java),
    TaskFeature("Задание 4", "Социальная лента с постами и комментариями", com.example.module4_t3to14.task4.MainActivity::class.java),
    TaskFeature("Задание 5", "Foreground service: счетчик времени", com.example.module4_t3to14.task5.MainActivity::class.java),
    TaskFeature("Задание 6", "Background service: одноразовый таймер", com.example.module4_t3to14.task6.MainActivity::class.java),
    TaskFeature("Задание 7", "Bind service: случайное число", com.example.module4_t3to14.task7.MainActivity::class.java),
    TaskFeature("Задание 8", "WorkManager: цепочка обработки фото", com.example.module4_t3to14.task8.MainActivity::class.java),
    TaskFeature("Задание 9", "WorkManager: параллельный прогноз погоды", com.example.module4_t3to14.task9.MainActivity::class.java),
    TaskFeature("Задание 10", "Местоположение и обратное геокодирование", com.example.module4_t3to14.task10.MainActivity::class.java),
    TaskFeature("Задание 11", "AlarmManager: ежедневное напоминание", com.example.module4_t3to14.task11.MainActivity::class.java),
    TaskFeature("Задание 12", "Cold Flow: случайный факт", com.example.module4_t3to14.task12.MainActivity::class.java),
    TaskFeature("Задание 13", "StateFlow: живой курс валют", com.example.module4_t3to14.task13.MainActivity::class.java),
    TaskFeature("Задание 14", "Компас на датчиках устройства", com.example.module4_t3to14.task14.MainActivity::class.java)
)

@Composable
private fun TaskMenu() {
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Модуль 4",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Задания 3-14",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(taskFeatures) { feature ->
                    TaskRow(
                        feature = feature,
                        onClick = {
                            context.startActivity(Intent(context, feature.activityClass))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    feature: TaskFeature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
