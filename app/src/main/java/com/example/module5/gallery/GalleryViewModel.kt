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

    // SharedFlow (не StateFlow!): одноразовые события для Snackbar
    private val _exportResult = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportResult: SharedFlow<String> = _exportResult.asSharedFlow()

    init {
        // Сканирование при запуске
        loadPhotos()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Публичный API
    // ──────────────────────────────────────────────────────────────────────────

    /** Создаёт пустой File для передачи URI в камеру через FileProvider. */
    fun createPhotoFile(): File = repository.createPhotoFile()

    /**
     * Вызывается после успешного снимка (TakePicture вернул true).
     * По заданию: сканируем папку заново после добавления фото.
     */
    fun onPhotoTaken(file: File) {
        if (!file.exists() || file.length() == 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            // Задание требует сканирования при добавлении нового фото
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

    // ──────────────────────────────────────────────────────────────────────────

    private fun loadPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            _photos.value = repository.loadPhotos()
        }
    }
}
