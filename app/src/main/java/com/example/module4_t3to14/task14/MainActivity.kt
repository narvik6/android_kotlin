package com.example.module4_t3to14.task14

import com.example.module4_t3to14.R

import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var compassArrow: ImageView
    private lateinit var azimuthText: TextView
    private lateinit var errorText: TextView

    private lateinit var viewModel: CompassViewModel

    private var currentAzimuth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task14_activity_main)

        compassArrow = findViewById(R.id.compassArrow)
        azimuthText = findViewById(R.id.azimuthText)
        errorText = findViewById(R.id.errorText)

        viewModel = ViewModelProvider(this)[CompassViewModel::class.java]

        // Устанавливаем колбэки
        viewModel.onAzimuthChanged = { azimuth ->
            runOnUiThread {
                updateAzimuth(azimuth)
            }
        }

        viewModel.onError = { errorMessage ->
            runOnUiThread {
                showError(errorMessage)
            }
        }

        // Инициализируем сенсоры
        viewModel.initSensors(this)
    }

    private fun updateAzimuth(azimuth: Float) {
        // Плавно поворачиваем стрелку
        val animation = RotateAnimation(
            currentAzimuth,
            -azimuth, // Отрицательное значение для правильного направления
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        animation.duration = 500
        animation.fillAfter = true

        compassArrow.startAnimation(animation)

        // Обновляем текст
        azimuthText.text = "Азимут: ${azimuth.toInt()}°"

        // Сохраняем текущее значение
        currentAzimuth = -azimuth
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = android.view.View.VISIBLE
        azimuthText.visibility = android.view.View.GONE
        compassArrow.visibility = android.view.View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.startListening()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopListening()
    }
}