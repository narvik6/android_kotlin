package com.example.module5.gallery

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val picturesDir: File =
        application.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

    private val repository = GalleryRepository(picturesDir)

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _exportResult = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportResult: SharedFlow<String> = _exportResult.asSharedFlow()

    init {
        loadPhotos()
    }


    fun createPhotoFile(): File = repository.createPhotoFile()

    fun onPhotoTaken(file: File) {
        if (!file.exists() || file.length() == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            _photos.value = repository.loadPhotos()
        }
    }

    fun exportToGallery(photo: PhotoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.exportToGallery(getApplication(), photo.file)
            _exportResult.emit(
                if (success) "Фото добавлено в галерею" else "Ошибка при экспорте"
            )
        }
    }


    private fun loadPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _photos.value = repository.loadPhotos()
        }
    }
}
