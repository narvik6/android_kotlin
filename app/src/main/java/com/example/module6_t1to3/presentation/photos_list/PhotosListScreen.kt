package com.example.module6_t1to3.presentation.photos_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.module6_t1to3.domain.model.Photo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosListScreen(
    viewModel: PhotosViewModel,
    onPhotoClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Фотокаталог") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PhotosUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is PhotosUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.getPhotos() }) {
                            Text("Повторить")
                        }
                    }
                }
                is PhotosUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.photos) { photo ->
                            PhotoCard(photo = photo, onClick = { onPhotoClick(photo.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCard(photo: Photo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            AsyncImage(
                model = photo.thumbnailUrl,
                contentDescription = "Photo by ${photo.author}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f), // Квадратные миниатюры
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = photo.author, style = MaterialTheme.typography.titleMedium)
                Text(text = "${photo.width} × ${photo.height}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}