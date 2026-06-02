package com.example.module4_t3to14.task11

import com.example.module4_t3to14.R

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    private const val REQUEST_CODE = 1001
    private const val REMINDER_DELAY_MS = 30_000L

    fun scheduleReminder(context: Context) {
        val prefs = ReminderPrefs(context)
        if (!prefs.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val triggerAtMillis = System.currentTimeMillis() + REMINDER_DELAY_MS

        // Устанавливаем будильник
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Для Android 12+ проверяем разрешение
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Запрашиваем разрешение через intent
                // В реальном приложении нужно показать диалог
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        saveNextReminderTime(context, triggerAtMillis)
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        saveNextReminderTime(context, null)
    }

    private fun saveNextReminderTime(context: Context, timeInMillis: Long?) {
        val prefs = context.getSharedPreferences("reminder_time", Context.MODE_PRIVATE)
        prefs.edit().putLong("next_reminder", timeInMillis ?: -1).apply()
    }

    fun getNextReminderTime(context: Context): String {
        val prefs = context.getSharedPreferences("reminder_time", Context.MODE_PRIVATE)
        val timeInMillis = prefs.getLong("next_reminder", -1)

        if (timeInMillis == -1L) return ""

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        return "Следующее напоминание: через 30 секунд ($hour:${"%02d".format(minute)}:${"%02d".format(second)})"
    }
}
