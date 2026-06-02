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

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val TAG = "UploadWorker"

    override suspend fun doWork(): Result {
        return try {
            val watermarkedFile = inputData.getString("watermarkedFile") ?: "unknown.jpg"
            Log.d(TAG, "Начало загрузки файла: $watermarkedFile")
            for (i in 1..10) {
                if (!currentCoroutineContext().isActive) {
                    Log.d(TAG, "Работа отменена")
                    return Result.failure()
                }

                setProgress(workDataOf("progress" to i * 10))
                Log.d(TAG, "Прогресс загрузки: ${i * 10}%")
                delay(200)
            }
            if (Random.nextInt(100) < 10) {
                Log.e(TAG, "Случайная ошибка при загрузке")
                throw Exception("Ошибка при загрузке в облако")
            }

            val cloudUrl = "https://cloud.example.com/photos/${watermarkedFile}"
            Log.d(TAG, "Загрузка завершена: $cloudUrl")

            Result.success(workDataOf(
                "cloudUrl" to cloudUrl,
                "finalFile" to watermarkedFile
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке: ${e.message}")
            Result.failure()
        }
    }
}
