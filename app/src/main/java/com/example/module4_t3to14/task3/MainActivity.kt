package com.example.module4_t3to14.task3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.module4_t3to14.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GithubSearchScreen()
                }
            }
        }
    }
}

data class GithubRepo(
    val id: Long,
    val fullName: String,
    val description: String,
    val stars: Int,
    val language: String
)

@Composable
fun GithubSearchScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var repos by remember { mutableStateOf<List<GithubRepo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Поиск репозиториев GitHub",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { value ->
                query = value
                searchJob?.cancel()

                if (value.isBlank()) {
                    repos = emptyList()
                    isLoading = false
                    return@OutlinedTextField
                }

                searchJob = coroutineScope.launch {
                    isLoading = true
                    delay(500)

                    val deferred = async(Dispatchers.Default) {
                        searchRepos(context.resources.openRawResource(R.raw.github_repos).bufferedReader().use { it.readText() }, value)
                    }

                    repos = deferred.await()
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Название, язык или описание") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (query.isBlank()) "Введите запрос" else "Найдено: ${repos.size}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (isLoading) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(repos, key = { it.id }) { repo ->
                RepoCard(repo)
            }
        }
    }
}

@Composable
private fun RepoCard(repo: GithubRepo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = repo.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = repo.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${repo.language} | stars: ${repo.stars}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private suspend fun searchRepos(json: String, query: String): List<GithubRepo> = withContext(Dispatchers.Default) {
    delay(350)
    val normalizedQuery = query.trim().lowercase()
    val array = JSONArray(json)
    val result = mutableListOf<GithubRepo>()

    for (index in 0 until array.length()) {
        val item = array.getJSONObject(index)
        val repo = GithubRepo(
            id = item.getLong("id"),
            fullName = item.getString("full_name"),
            description = item.getString("description"),
            stars = item.getInt("stargazers_count"),
            language = item.getString("language")
        )

        if (
            repo.fullName.lowercase().contains(normalizedQuery) ||
            repo.description.lowercase().contains(normalizedQuery) ||
            repo.language.lowercase().contains(normalizedQuery)
        ) {
            result.add(repo)
        }
    }

    result
}
