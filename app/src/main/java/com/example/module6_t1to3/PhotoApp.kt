package com.example.module6_t1to3

import android.app.Application
import com.example.module6_t1to3.di.AppContainer

class PhotoApp : Application() {
    // Наш контейнер зависимостей
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}