package com.example.module4_t3to14.task8

import com.example.module4_t3to14.R

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class CompressWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG = "CompressWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Начало сжатия фото")

            // Имитация процесса сжатия
            for (i in 1..10) {
                // Проверяем не отменена ли работа
                if (!currentCoroutineContext().isActive) {
                    Log.d(TAG, "Работа отменена")
                    return Result.failure()
                }

                // Обновляем прогресс
                setProgress(workDataOf("progress" to i * 10))
                Log.d(TAG, "Прогресс сжатия: ${i * 10}%")
                delay(300) // Имитация работы
            }

            // Имя сжатого файла
            val compressedFileName = "compressed_photo_${System.currentTimeMillis()}.jpg"
            Log.d(TAG, "Сжатие завершено: $compressedFileName")

            Result.success(workDataOf(
                "compressedFile" to compressedFileName
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сжатии: ${e.message}")
            Result.failure()
        }
    }
}