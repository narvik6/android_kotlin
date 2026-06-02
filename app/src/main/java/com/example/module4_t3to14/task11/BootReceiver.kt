package com.example.module4_t3to14.task11

import com.example.module4_t3to14.R

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = ReminderPrefs(context)

            // Если напоминание было включено - восстанавливаем
            if (prefs.isEnabled) {
                ReminderScheduler.scheduleReminder(context)
            }
        }
    }
}