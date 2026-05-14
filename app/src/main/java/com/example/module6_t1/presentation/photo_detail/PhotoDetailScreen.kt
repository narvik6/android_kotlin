package com.example.module6_t1.presentation.photo_detail

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.module6_t1.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photo: Photo?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    // Лаунчер SAF для выбора места сохранения файла
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        if (uri != null && photo != null) {
            isDownloading = true
            coroutineScope.launch {
                downloadAndSaveImage(context, photo.downloadUrl, uri)
                isDownloading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фото") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (photo == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Фото не найдено")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = photo.downloadUrl,
                contentDescription = "Original photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Автор: ${photo.author}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Размер: ${photo.width} × ${photo.height}")
                Text(text = "Ссылка: ${photo.url}", color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Предлагаем имя файла по умолчанию
                        createDocumentLauncher.launch("photo_${photo.id}.jpg")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Скачать фото")
                    }
                }
            }
        }
    }
}

// Вспомогательная функция для скачивания потока данных в файл по URI
suspend fun downloadAndSaveImage(context: Context, imageUrl: String, uri: Uri) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            url.openStream().use { input ->
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Фото успешно сохранено", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка скачивания: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}