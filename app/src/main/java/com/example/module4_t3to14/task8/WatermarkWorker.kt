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
import kotlin.random.Random

class WatermarkWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG = "WatermarkWorker"

    override suspend fun doWork(): Result {
        return try {
            // Получаем имя файла из предыдущего worker'а
            val compressedFile = inputData.getString("compressedFile") ?: "unknown.jpg"
            Log.d(TAG, "Начало добавления водяного знака к файлу: $compressedFile")

            // Имитация добавления водяного знака
            for (i in 1..10) {
                if (!currentCoroutineContext().isActive) {
                    Log.d(TAG, "Работа отменена")
                    return Result.failure()
                }

                setProgress(workDataOf("progress" to i * 10))
                Log.d(TAG, "Прогресс добавления водяного знака: ${i * 10}%")
                delay(250) // Имитация работы
            }

            // 10% шанс ошибки для демонстрации
            if (Random.nextInt(100) < 10) {
                Log.e(TAG, "Случайная ошибка при добавлении водяного знака")
                throw Exception("Ошибка при добавлении водяного знака")
            }

            val watermarkedFile = "watermarked_${compressedFile}"
            Log.d(TAG, "Водяной знак добавлен: $watermarkedFile")

            Result.success(workDataOf(
                "watermarkedFile" to watermarkedFile
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении водяного знака: ${e.message}")
            Result.failure()
        }
    }
}
