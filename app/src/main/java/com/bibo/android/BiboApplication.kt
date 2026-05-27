package com.bibo.android

import android.app.Application
import com.bibo.android.core.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BiboApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BiboApplication)
            modules(appModules)
        }
    }
}
