package com.example.module4_t3to14.task9

import com.example.module4_t3to14.R

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay

class ReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Получаем все ключи городов из входных данных
            val cities = listOf("Москва", "Лондон", "Нью-Йорк", "Токио")
            val temperatures = mutableListOf<Int>()
            val cityStatus = mutableListOf<String>()

            setProgress(workDataOf("status" to "Формируем итоговый отчёт..."))

            // Собираем результаты из каждого города
            for (city in cities) {
                val temp = inputData.getInt("${city}_temp", 0)
                temperatures.add(temp)

                val condition = inputData.getString("${city}_condition") ?: "неизвестно"
                cityStatus.add("$city: $temp°C, $condition")
            }

            // Имитация обработки отчета
            delay(1000)

            // Вычисляем среднюю температуру
            val averageTemp = temperatures.average().toInt()

            val report = """
                Итоговый отчет:
                ${cityStatus.joinToString("\n")}
                
                Средняя температура: $averageTemp°C
            """.trimIndent()

            Result.success(workDataOf(
                "report" to report,
                "averageTemp" to averageTemp,
                "status" to "Отчёт готов!"
            ))
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            1002,
            NotificationUtils.createNotification(
                applicationContext,
                "Формируем отчет...",
                "Обработка результатов"
            )
        )
    }
}