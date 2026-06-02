package com.example.module4_t3to14.task11

import com.example.module4_t3to14.R

import android.content.Context
import android.content.SharedPreferences

class ReminderPrefs(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("is_enabled", false)
        set(value) = prefs.edit().putBoolean("is_enabled", value).apply()

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(value) = prefs.edit().putInt("reminder_hour", value).apply()

    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(value) = prefs.edit().putInt("reminder_minute", value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}