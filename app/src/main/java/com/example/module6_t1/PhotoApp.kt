package com.example.module6_t1

import android.app.Application
import com.example.module6_t1.di.AppContainer

class PhotoApp : Application() {
    // Наш контейнер зависимостей
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}