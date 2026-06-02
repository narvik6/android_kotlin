package com.example.module4_t3to14.task9

import com.example.module4_t3to14.R

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var resultText: TextView
    private lateinit var citiesStatus: TextView

    private lateinit var workManager: WorkManager

    private val cities = listOf("Москва", "Лондон", "Нью-Йорк", "Токио")
    private val cityStatuses = linkedMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task9_activity_main)

        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        startButton = findViewById(R.id.startButton)
        resultText = findViewById(R.id.resultText)
        citiesStatus = findViewById(R.id.citiesStatus)

        workManager = WorkManager.getInstance(this)

        // Запрашиваем разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        startButton.setOnClickListener {
            startWeatherCollection()
        }
    }

    private fun startWeatherCollection() {
        startButton.isEnabled = false
        resultText.text = ""
        citiesStatus.text = ""
        progressBar.progress = 0
        progressBar.max = 100
        progressBar.visibility = android.view.View.VISIBLE
        cityStatuses.clear()
        cities.forEach { cityStatuses[it] = "$it: ожидает запуска" }
        renderCityStatuses()

        // Показываем начальное уведомление
        NotificationUtils.updateNotification(
            this,
            "Сбор прогноза погоды",
            "Загружаем погоду для ${cities.size} городов..."
        )

        statusText.text = "Загружаем погоду для ${cities.size} городов..."

        val weatherWorkers = cities.map { city ->
            OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(workDataOf("city" to city))
                .addTag("weather")
                .build()
        }

        val reportWorker = OneTimeWorkRequestBuilder<ReportWorker>()
            .addTag("report")
            .build()

        workManager.beginWith(weatherWorkers)
            .then(reportWorker)
            .enqueue()

        trackWorkProgress(weatherWorkers, reportWorker)
    }

    private fun trackWorkProgress(
        weatherWorkers: List<OneTimeWorkRequest>,
        reportWorker: OneTimeWorkRequest
    ) {
        weatherWorkers.forEachIndexed { index, request ->
            val city = cities[index]
            workManager.getWorkInfoByIdLiveData(request.id).observe(this) { info ->
                info?.let { updateWeatherUI(it, city) }
            }
        }

        workManager.getWorkInfoByIdLiveData(reportWorker.id).observe(this) { info ->
            info?.let {
                when (it.state) {
                    WorkInfo.State.RUNNING -> {
                        val reportStatus = it.progress.getString("status") ?: "Формируем итоговый отчёт..."
                        statusText.text = reportStatus
                        NotificationUtils.updateNotification(this, reportStatus, "Обработка результатов...")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val report = it.outputData.getString("report") ?: ""
                        val avgTemp = it.outputData.getInt("averageTemp", 0)

                        resultText.text = "Средняя температура: $avgTemp°C"
                        citiesStatus.text = report

                        NotificationUtils.updateNotification(
                            this,
                            "Отчёт готов!",
                            "Средняя температура: $avgTemp°C"
                        )

                        startButton.isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                    }
                    WorkInfo.State.FAILED -> {
                        resultText.text = "Ошибка при сборе данных"
                        resultText.setTextColor(android.graphics.Color.RED)
                        NotificationUtils.updateNotification(this, "Ошибка", "Не удалось собрать прогноз")
                        startButton.isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateWeatherUI(info: WorkInfo, city: String) {
        when (info.state) {
            WorkInfo.State.RUNNING -> {
                cityStatuses[city] = info.progress.getString("status") ?: "Загружаем погоду для $city..."
                renderCityStatuses()
            }
            WorkInfo.State.SUCCEEDED -> {
                val temp = info.outputData.getInt("temperature", 0)
                val condition = info.outputData.getString("condition") ?: "неизвестно"
                cityStatuses[city] = "$city: $temp°C, $condition"
                renderCityStatuses()

                val completed = cityStatuses.values.count { !it.contains("ожидает") && !it.contains("Загружаем") }
                progressBar.progress = completed * 100 / cities.size
                NotificationUtils.updateNotification(
                    this,
                    "Сбор прогноза погоды",
                    "Готово: $completed/${cities.size} городов"
                )
            }
            WorkInfo.State.FAILED -> {
                cityStatuses[city] = "$city: ошибка загрузки"
                renderCityStatuses()
            }
            else -> {}
        }
    }

    private fun renderCityStatuses() {
        citiesStatus.text = cityStatuses.values.joinToString("\n")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Разрешение получено
        }
    }
}
