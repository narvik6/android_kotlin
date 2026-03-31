package com.example.module5_t1to3.gallery

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val context       = LocalContext.current
    val photos        by viewModel.photos.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    var pendingFile by remember { mutableStateOf<File?>(null) }


    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) pendingFile?.let { viewModel.onPhotoTaken(it) }
        pendingFile = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = viewModel.createPhotoFile()
            pendingFile = file
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            cameraLauncher.launch(uri)
        }
    }


    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        ) {
            val file = viewModel.createPhotoFile()
            pendingFile = file
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    LaunchedEffect(Unit) {
        viewModel.exportResult.collect { message ->
            snackbarState.showSnackbar(message)
        }
    }


    Scaffold(
        topBar = { TopAppBar(title = { Text("Моя галерея") }) },
        snackbarHost = { SnackbarHost(snackbarState) },
        floatingActionButton = {
            FloatingActionButton(onClick = ::launchCamera) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Сделать фото")
            }
        }
    ) { innerPadding ->
        if (photos.isEmpty()) {
            EmptyGalleryPlaceholder(
                modifier    = Modifier.padding(innerPadding),
                onTakePhoto = ::launchCamera
            )
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                modifier              = Modifier.padding(innerPadding),
                contentPadding        = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement   = Arrangement.spacedBy(2.dp)
            ) {
                items(photos, key = { it.name }) { photo ->
                    PhotoGridItem(
                        photo    = photo,
                        onExport = { viewModel.exportToGallery(photo) }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(photo: PhotoItem, onExport: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick     = {},
                onLongClick = { menuExpanded = true }
            )
    ) {
        AsyncImage(
            model              = photo.file,
            contentDescription = photo.name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize()
        )

        DropdownMenu(
            expanded         = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text    = { Text("Экспорт в галерею") },
                onClick = {
                    menuExpanded = false
                    onExport()
                }
            )
        }
    }
}


@Composable
private fun EmptyGalleryPlaceholder(modifier: Modifier = Modifier, onTakePhoto: () -> Unit) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "У вас пока нет фото",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onTakePhoto) {
                Text("Сделать первое фото")
            }
        }
    }
}
