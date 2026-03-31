package com.example.module5.gallery

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Работа с файлами галереи:
 *  – хранилище: getExternalFilesDir(DIRECTORY_PICTURES) — app-specific, scoped storage
 *  – экспорт:   MediaStore (рекомендуемый способ для Android 10+, разрешений не нужно)
 */
class GalleryRepository(private val picturesDir: File) {

    // ──────────────────────────────────────────────────────────────────────────
    // Создание файла для камеры
    // ──────────────────────────────────────────────────────────────────────────

    /** Имя файла по формату из задания: IMG_yyyyMMdd_HHmmss.jpg */
    fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(picturesDir, "IMG_$timestamp.jpg")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Загрузка (сканирование папки)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Сканирует папку и возвращает список фото, отсортированных от новых к старым.
     * Вызывается при запуске и сразу после съёмки (по заданию).
     */
    fun loadPhotos(): List<PhotoItem> =
        picturesDir
            .listFiles { f -> f.isFile && f.extension.lowercase() == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { PhotoItem(file = it, name = it.name) }
            ?: emptyList()

    // ──────────────────────────────────────────────────────────────────────────
    // Экспорт в общую галерею через MediaStore
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Копирует фото из приватной папки приложения в общую галерею (Pictures/MyGallery).
     * Используется MediaStore — рекомендуемый способ для Android 10+.
     * Разрешения WRITE_EXTERNAL_STORAGE НЕ требуются (Scoped Storage).
     */
    fun exportToGallery(context: Context, photoFile: File): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MyGallery"
                )
                // IS_PENDING = 1: файл «занят», другие приложения его не видят пока мы пишем
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return false

            // Копируем байты из приватного файла в MediaStore-поток
            resolver.openOutputStream(uri)?.use { out ->
                photoFile.inputStream().use { it.copyTo(out) }
            }

            // IS_PENDING = 0: файл готов, галерея его индексирует
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
