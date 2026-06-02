package com.example.module4_t3to14.task8

import com.example.module4_t3to14.R

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

    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task8_activity_main)

        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        startButton = findViewById(R.id.startButton)
        resultText = findViewById(R.id.resultText)

        workManager = WorkManager.getInstance(this)
        resetScreen()
        workManager.pruneWork()

        startButton.setOnClickListener {
            startProcessing()
        }
    }

    private fun startProcessing() {
        startButton.isEnabled = false
        progressBar.progress = 0
        progressBar.visibility = android.view.View.VISIBLE
        statusText.text = "Начинаем обработку..."
        resultText.text = ""

        // Создаем Worker'ы
        val compressWorker = OneTimeWorkRequestBuilder<CompressWorker>()
            .addTag("compress")
            .build()

        val watermarkWorker = OneTimeWorkRequestBuilder<WatermarkWorker>()
            .addTag("watermark")
            .build()

        val uploadWorker = OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag("upload")
            .build()

        // Запускаем цепочку
        workManager.beginWith(compressWorker)
            .then(watermarkWorker)
            .then(uploadWorker)
            .enqueue()

        // Отслеживаем прогресс
        trackProgress(compressWorker.id, watermarkWorker.id, uploadWorker.id)
    }

    private fun trackProgress(
        compressId: java.util.UUID,
        watermarkId: java.util.UUID,
        uploadId: java.util.UUID
    ) {
        workManager.getWorkInfoByIdLiveData(compressId).observe(this) { info ->
            info?.let { updateUI(it) }
        }

        workManager.getWorkInfoByIdLiveData(watermarkId).observe(this) { info ->
            info?.let { updateUI(it) }
        }

        workManager.getWorkInfoByIdLiveData(uploadId).observe(this) { info ->
            info?.let {
                updateUI(it)
                if (it.state == WorkInfo.State.SUCCEEDED) {
                    resultText.text = "✅ Готово! Фото загружено"
                    startButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                } else if (it.state == WorkInfo.State.FAILED) {
                    resultText.text = "❌ Ошибка! Обработка прервана"
                    startButton.isEnabled = true
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun updateUI(workInfo: WorkInfo) {
        val progress = workInfo.progress.getInt("progress", 0)
        progressBar.progress = progress

        when {
            "compress" in workInfo.tags -> statusText.text = "Сжимаем фото... $progress%"
            "watermark" in workInfo.tags -> statusText.text = "Добавляем водяной знак... $progress%"
            "upload" in workInfo.tags -> statusText.text = "Загружаем в облако... $progress%"
        }
    }

    private fun resetScreen() {
        statusText.text = "Нажмите кнопку для начала"
        resultText.text = ""
        progressBar.progress = 0
        progressBar.visibility = android.view.View.GONE
        startButton.isEnabled = true
    }
}
