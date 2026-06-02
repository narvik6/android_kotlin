package com.example.module4_t3to14.task5

import com.example.module4_t3to14.R

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {

    private val CHANNEL_ID = "timer_channel"
    private val NOTIFICATION_ID = 1001

    private val binder = TimerBinder()
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var seconds = 0
    private var isRunning = false

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    fun getCurrentSeconds(): Int = seconds

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTimer()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startTimer() {
        if (isRunning) return
        isRunning = true

        serviceScope.launch {
            while (isRunning) {
                delay(1000)
                seconds++
                updateNotification()
            }
        }
    }

    fun stopTimer() {
        isRunning = false
        seconds = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Счётчик времени")
            .setContentText("Прошло $seconds секунд")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Таймер",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Канал для отображения времени"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}