package com.example.module5.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// Навигационные состояния (вместо NavController — простой sealed class)
// ──────────────────────────────────────────────────────────────────────────────

sealed class DiaryNavigation {
    object List : DiaryNavigation()
    object NewEntry : DiaryNavigation()
    data class EditEntry(
        val fileName : String,
        val title    : String,
        val text     : String
    ) : DiaryNavigation()
}

// ──────────────────────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────────────────────

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DiaryRepository(application.filesDir)

    private val _entries    = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries.asStateFlow()

    private val _navigation = MutableStateFlow<DiaryNavigation>(DiaryNavigation.List)
    val navigation: StateFlow<DiaryNavigation> = _navigation.asStateFlow()

    init {
        // Полное сканирование папки — ТОЛЬКО ОДИН РАЗ при запуске
        viewModelScope.launch(Dispatchers.IO) {
            _entries.value = repository.loadAllEntries()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Навигация
    // ──────────────────────────────────────────────────────────────────────────

    fun openNewEntry() {
        _navigation.value = DiaryNavigation.NewEntry
    }

    fun openEntry(entry: DiaryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val (title, text) = repository.readFullEntry(entry.fileName)
            _navigation.value = DiaryNavigation.EditEntry(entry.fileName, title, text)
        }
    }

    fun navigateBack() {
        _navigation.value = DiaryNavigation.List
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Операции со списком (без пересканирования папки!)
    // ──────────────────────────────────────────────────────────────────────────

    /** Сохраняем новую запись → добавляем в начало списка */
    fun saveNewEntry(title: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newEntry = repository.saveEntry(title, text)
            _entries.update { listOf(newEntry) + it }
            _navigation.value = DiaryNavigation.List
        }
    }

    /** Обновляем запись → патчим только нужный элемент в списке */
    fun updateEntry(fileName: String, title: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateEntry(fileName, title, text)
            _entries.update { current ->
                current.map { entry ->
                    if (entry.fileName == fileName)
                        entry.copy(title = title, preview = text.trim().take(40))
                    else
                        entry
                }
            }
            _navigation.value = DiaryNavigation.List
        }
    }

    /** Удаляем запись → убираем из списка по имени файла */
    fun deleteEntry(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(fileName)
            _entries.update { it.filter { e -> e.fileName != fileName } }
        }
    }
}
