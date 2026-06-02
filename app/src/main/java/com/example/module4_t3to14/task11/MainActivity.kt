package com.example.module4_t3to14.task11

import com.example.module4_t3to14.R

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView
    private lateinit var nextReminderText: TextView
    private lateinit var actionButton: Button

    private lateinit var prefs: ReminderPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task11_activity_main)

        titleText = findViewById(R.id.titleText)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)
        nextReminderText = findViewById(R.id.nextReminderText)
        actionButton = findViewById(R.id.actionButton)

        prefs = ReminderPrefs(this)

        // Запрашиваем разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // Проверяем разрешение на точные будильники для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Пожалуйста, разрешите точные будильники в настройках",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }

        updateUI()

        actionButton.setOnClickListener {
            toggleReminder()
        }
    }

    private fun toggleReminder() {
        prefs.isEnabled = !prefs.isEnabled

        if (prefs.isEnabled) {
            ReminderScheduler.scheduleReminder(this)
            Toast.makeText(this, "Напоминание включено", Toast.LENGTH_SHORT).show()
        } else {
            ReminderScheduler.cancelReminder(this)
            Toast.makeText(this, "Напоминание выключено", Toast.LENGTH_SHORT).show()
        }

        updateUI()
    }

    private fun updateUI() {
        if (prefs.isEnabled) {
            // Включено
            statusIndicator.setBackgroundResource(R.drawable.circle_green)
            statusText.text = "Включено"
            actionButton.text = "Выключить напоминание"

            val nextTime = ReminderScheduler.getNextReminderTime(this)
            if (nextTime.isNotEmpty()) {
                nextReminderText.text = nextTime
                nextReminderText.visibility = View.VISIBLE
            }
        } else {
            // Выключено
            statusIndicator.setBackgroundResource(R.drawable.circle_gray)
            statusText.text = "Выключено"
            actionButton.text = "Включить напоминание"
            nextReminderText.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Без разрешения уведомления не будут показываться", Toast.LENGTH_LONG).show()
            }
        }
    }
}