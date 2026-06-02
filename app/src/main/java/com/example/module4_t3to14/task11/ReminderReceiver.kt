package com.example.module4_t3to14.task11

import com.example.module4_t3to14.R

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "reminder_channel"
    private val NOTIFICATION_ID = 1001

    override fun onReceive(context: Context, intent: Intent) {
        // Создаем канал уведомлений
        createNotificationChannel(context)

        // Показываем уведомление
        showNotification(context)

        // Планируем следующее напоминание (на завтра)
        ReminderScheduler.scheduleReminder(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминание о таблетке",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для напоминаний"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Время принять таблетку!")
            .setContentText("Не забудьте принять лекарство")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}