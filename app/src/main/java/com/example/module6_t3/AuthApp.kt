package com.example.module6_t3

import android.app.Application
import com.example.module6_t3.di.AppContainer

class AuthApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Передаем Context приложения в контейнер
        container = AppContainer(this)
    }
}