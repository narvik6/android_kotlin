package com.example.module6_t2

import android.app.Application
import com.example.module6_t2.di.AppContainer

class NobelApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}