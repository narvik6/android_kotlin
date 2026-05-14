package com.example.module6_t1to3.presentation.photos_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.module6_t1to3.PhotoApp
import com.example.module6_t1to3.domain.model.Photo
import com.example.module6_t1to3.domain.usecase.GetPhotosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Описание возможных состояний экрана
sealed interface PhotosUiState {
    data class Success(val photos: List<Photo>) : PhotosUiState
    data class Error(val message: String) : PhotosUiState
    data object Loading : PhotosUiState
}

class PhotosViewModel(
    private val getPhotosUseCase: GetPhotosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PhotosUiState>(PhotosUiState.Loading)
    val uiState: StateFlow<PhotosUiState> = _uiState.asStateFlow()

    init {
        getPhotos()
    }

    fun getPhotos() {
        viewModelScope.launch {
            _uiState.value = PhotosUiState.Loading
            getPhotosUseCase().fold(
                onSuccess = { photos ->
                    _uiState.value = PhotosUiState.Success(photos)
                },
                onFailure = { exception ->
                    _uiState.value = PhotosUiState.Error(exception.message ?: "Произошла неизвестная ошибка")
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Достаем экземпляр нашего PhotoApp
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PhotoApp)
                // Берем UseCase из контейнера и передаем в ViewModel
                PhotosViewModel(application.container.getPhotosUseCase)
            }
        }
    }

    fun getPhotoById(id: String): Photo? {
        val currentState = _uiState.value
        if (currentState is PhotosUiState.Success) {
            return currentState.photos.find { it.id == id }
        }
        return null
    }
}