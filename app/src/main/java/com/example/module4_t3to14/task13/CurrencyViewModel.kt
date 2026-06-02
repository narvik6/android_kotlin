package com.example.module4_t3to14.task13

import com.example.module4_t3to14.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class CurrencyViewModel : ViewModel() {

    // Состояние курса
    private val _rate = MutableStateFlow(90.5)
    val rate: StateFlow<Double> = _rate.asStateFlow()

    // Предыдущий курс для сравнения
    private var previousRate = 90.5

    // Тренд (вверх/вниз)
    private val _trend = MutableStateFlow<Trend?>(null)
    val trend: StateFlow<Trend?> = _trend.asStateFlow()

    // Изменение в процентах
    private val _changePercent = MutableStateFlow("")
    val changePercent: StateFlow<String> = _changePercent.asStateFlow()

    enum class Trend {
        UP, DOWN
    }

    init {
        // Запускаем автоматическое обновление каждые 5 секунд
        startAutoUpdate()
    }

    private fun startAutoUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(5000) // 5 секунд
                generateNewRate()
            }
        }
    }

    fun refreshRate() {
        viewModelScope.launch {
            generateNewRate()
        }
    }

    private fun generateNewRate() {
        previousRate = _rate.value
        val newRate = 90.5 + (Random.nextDouble() - 0.5) * 4.0
        val roundedRate = String.format("%.2f", newRate).toDouble()

        _rate.update { roundedRate }
        when {
            roundedRate > previousRate -> {
                _trend.update { Trend.UP }
                val change = ((roundedRate - previousRate) / previousRate * 100)
                _changePercent.update { String.format("+%.2f%%", change) }
            }
            roundedRate < previousRate -> {
                _trend.update { Trend.DOWN }
                val change = ((previousRate - roundedRate) / previousRate * 100)
                _changePercent.update { String.format("-%.2f%%", change) }
            }
            else -> {
                _trend.update { null }
                _changePercent.update { "0.00%" }
            }
        }
    }
}