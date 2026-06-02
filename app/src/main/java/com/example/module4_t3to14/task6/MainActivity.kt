package com.example.module4_t3to14.task6

import com.example.module4_t3to14.R

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var timeInput: EditText
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task6_activity_main)

        timeInput = findViewById(R.id.timeInput)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        startButton.setOnClickListener {
            startTimer()
        }
    }

    private fun startTimer() {
        val seconds = timeInput.text.toString().toIntOrNull()

        if (seconds == null || seconds <= 0) {
            Toast.makeText(this, "Введите корректное число секунд", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, TimerService::class.java).apply {
            putExtra("seconds", seconds)
        }

        startService(intent)
        showCountdown(seconds)

        Toast.makeText(this, "Таймер запущен на $seconds секунд", Toast.LENGTH_SHORT).show()
        timeInput.text.clear()
    }

    private fun showCountdown(seconds: Int) {
        countDownTimer?.cancel()
        startButton.isEnabled = false

        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val left = (millisUntilFinished / 1000L + 1L).toInt()
                statusText.text = "Осталось: $left сек."
            }

            override fun onFinish() {
                statusText.text = "Таймер завершен. Уведомление отправлено."
                startButton.isEnabled = true
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
