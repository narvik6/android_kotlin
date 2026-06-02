package com.example.module4_t3to14.task5

import com.example.module4_t3to14.R

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var timerService: TimerService? = null
    private var isServiceBound = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isServiceBound = true
            Toast.makeText(this@MainActivity, "Сервис подключен", Toast.LENGTH_SHORT).show()
            startUpdatingTimer()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            timerService = null
            Toast.makeText(this@MainActivity, "Сервис отключен", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task5_activity_main)

        timerText = findViewById(R.id.timerText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        startButton.setOnClickListener {
            startTimerService()
        }

        stopButton.setOnClickListener {
            stopTimerService()
        }
    }

    private fun startTimerService() {
        val intent = Intent(this, TimerService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun stopTimerService() {
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }

        timerService?.stopTimer()

        val intent = Intent(this, TimerService::class.java)
        stopService(intent)

        timerText.text = "0"
    }

    private fun startUpdatingTimer() {
        serviceScope.launch {
            while (isServiceBound) {
                delay(100)
                timerService?.let { service ->
                    timerText.text = service.getCurrentSeconds().toString()
                }
            }
        }
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
        super.onDestroy()
        serviceScope.cancel()
        if (isServiceBound) {
            unbindService(connection)
        }
    }
}