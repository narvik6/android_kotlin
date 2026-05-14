package com.example.module6_t2.presentation.nobel_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.module6_t2.NobelApp
import com.example.module6_t2.domain.usecase.GetNobelPrizesUseCase
import com.example.module6_t2.presentation.model.LaureateUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NobelUiState {
    data class Success(val laureates: List<LaureateUiItem>) : NobelUiState
    data class Error(val message: String) : NobelUiState
    data object Loading : NobelUiState
}

class NobelViewModel(
    private val getNobelPrizesUseCase: GetNobelPrizesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NobelUiState>(NobelUiState.Loading)
    val uiState: StateFlow<NobelUiState> = _uiState.asStateFlow()

    init {
        loadPrizes() // Загружаем данные без фильтров при старте
    }

    fun loadPrizes(year: String? = null, category: String? = null) {
        viewModelScope.launch {
            _uiState.value = NobelUiState.Loading

            getNobelPrizesUseCase(year, category).fold(
                onSuccess = { prizes ->
                    // "Сплющиваем" список премий в список лауреатов
                    val uiItems = prizes.flatMap { prize ->
                        prize.laureates.map { laureate ->
                            LaureateUiItem(
                                id = laureate.id,
                                year = prize.year,
                                category = prize.category,
                                fullName = laureate.fullName,
                                motivation = laureate.motivation,
                                birthCountry = laureate.birthCountry
                            )
                        }
                    }
                    _uiState.value = NobelUiState.Success(uiItems)
                },
                onFailure = { exception ->
                    _uiState.value = NobelUiState.Error(exception.localizedMessage ?: "Ошибка загрузки")
                }
            )
        }
    }

    // Вспомогательный метод для экрана деталей
    fun getLaureateById(id: String): LaureateUiItem? {
        val currentState = _uiState.value
        if (currentState is NobelUiState.Success) {
            return currentState.laureates.find { it.id == id }
        }
        return null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NobelApp)
                NobelViewModel(application.container.getNobelPrizesUseCase)
            }
        }
    }
}