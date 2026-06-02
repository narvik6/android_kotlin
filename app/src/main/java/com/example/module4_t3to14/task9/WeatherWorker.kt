package com.example.module4_t3to14.task9

import com.example.module4_t3to14.R

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlin.random.Random

data class WeatherResult(
    val city: String,
    val temperature: Int,
    val condition: String
)

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Получаем название города из входных данных
            val city = inputData.getString("city") ?: "Неизвестный город"

            // Имитация загрузки данных о погоде
            setProgress(workDataOf(
                "city" to city,
                "status" to "Загружаем погоду для $city..."
            ))

            // Случайное время загрузки (1-3 секунды)
            val delayTime = Random.nextLong(1000, 3000)
            delay(delayTime)

            // Генерируем случайную температуру
            val temperature = Random.nextInt(-10, 30)
            val conditions = listOf("солнечно", "облачно", "дождь", "снег", "ветрено")
            val condition = conditions.random()

            Result.success(workDataOf(
                "city" to city,
                "temperature" to temperature,
                "condition" to condition,
                "${city}_temp" to temperature,
                "${city}_condition" to condition,
                "status" to "Готово: $city"
            ))
        } catch (e: Exception) {
            Result.failure()
        }
    }

    // Для foreground работы (чтобы уведомление жило долго)
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            1001,
            NotificationUtils.createNotification(
                applicationContext,
                "Загружаем погоду...",
                "Собираем данные о погоде"
            )
        )
    }
}
