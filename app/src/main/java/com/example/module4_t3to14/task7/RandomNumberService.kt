package com.example.module4_t3to14.task7

import com.example.module4_t3to14.R

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import kotlin.random.Random

class RandomNumberService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentNumber = 0
    private var listeners = mutableListOf<NumberUpdateListener>()
    private var isRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberService = this@RandomNumberService
    }

    interface NumberUpdateListener {
        fun onNumberUpdated(number: Int)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun startGenerating() {
        if (isRunning) return
        isRunning = true

        serviceScope.launch {
            while (isRunning) {
                currentNumber = Random.nextInt(0, 101) // 0..100

                // Уведомляем всех слушателей
                withContext(Dispatchers.Main) {
                    listeners.forEach { it.onNumberUpdated(currentNumber) }
                }

                delay(1000) // Каждую секунду
            }
        }
    }

    fun stopGenerating() {
        isRunning = false
    }

    fun addListener(listener: NumberUpdateListener) {
        listeners.add(listener)
        // Сразу отправляем текущее число
        listener.onNumberUpdated(currentNumber)
    }

    fun removeListener(listener: NumberUpdateListener) {
        listeners.remove(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGenerating()
        serviceScope.cancel()
        listeners.clear()
    }
}