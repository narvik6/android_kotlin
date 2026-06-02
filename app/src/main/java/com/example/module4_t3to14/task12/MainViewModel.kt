package com.example.module4_t3to14.task12

import com.example.module4_t3to14.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel : ViewModel() {

    // Состояние UI
    var currentFact = ""
        private set

    var isLoading = false
        private set

    var isCardVisible = false
        private set

    // Callback для обновления UI
    private var onFactLoaded: ((String) -> Unit)? = null
    private var onLoadingChanged: ((Boolean) -> Unit)? = null
    private var onCardVisibilityChanged: ((Boolean) -> Unit)? = null

    fun setCallbacks(
        onFactLoaded: (String) -> Unit,
        onLoadingChanged: (Boolean) -> Unit,
        onCardVisibilityChanged: (Boolean) -> Unit
    ) {
        this.onFactLoaded = onFactLoaded
        this.onLoadingChanged = onLoadingChanged
        this.onCardVisibilityChanged = onCardVisibilityChanged
    }

    fun getRandomFactFlow(): Flow<String> = flow {
        emit("")
        val delayTime = Random.nextLong(1500, 3000)
        delay(delayTime)

        val fact = AnimalFacts.getRandomFact()
        emit(fact)
    }

    fun generateNewFact() {
        viewModelScope.launch {
            // Показываем загрузку
            isLoading = true
            onLoadingChanged?.invoke(true)
            onCardVisibilityChanged?.invoke(false)
            // Запускаем Flow
            getRandomFactFlow().collect { fact ->
                if (fact.isEmpty()) {
                    // Начало загрузки - ничего не делаем
                } else {
                    // Факт загружен
                    currentFact = fact
                    isLoading = false
                    isCardVisible = true

                    // Обновляем UI
                    onFactLoaded?.invoke(fact)
                    onLoadingChanged?.invoke(false)
                    onCardVisibilityChanged?.invoke(true)
                }
            }
        }
    }

    fun clearCallbacks() {
        onFactLoaded = null
        onLoadingChanged = null
        onCardVisibilityChanged = null
    }
}