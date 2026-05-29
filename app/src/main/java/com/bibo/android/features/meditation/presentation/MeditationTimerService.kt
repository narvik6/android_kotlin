package com.bibo.android.features.meditation.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bibo.android.R

class MeditationTimerService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        val duration = intent?.getLongExtra(EXTRA_DURATION_SECONDS, 0L) ?: 0L
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Медитация")
            .setContentText("Практика запущена: ${duration / 60} мин.")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Медитация",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "meditation_timer"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_DURATION_SECONDS = "duration_seconds"

        fun start(context: Context, durationSeconds: Long) {
            val intent = Intent(context, MeditationTimerService::class.java)
                .putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MeditationTimerService::class.java))
        }
    }
}
