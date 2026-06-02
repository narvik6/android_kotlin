package com.example.module4_t3to14.task7

import com.example.module4_t3to14.R

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), RandomNumberService.NumberUpdateListener {

    private lateinit var numberText: TextView
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button

    private var randomService: RandomNumberService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RandomNumberService.LocalBinder
            randomService = binder.getService()
            isBound = true
            randomService?.addListener(this@MainActivity)
            randomService?.startGenerating()

            statusText.text = "Подключено"
            connectButton.isEnabled = false
            disconnectButton.isEnabled = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            randomService = null

            statusText.text = "Отключено"
            connectButton.isEnabled = true
            disconnectButton.isEnabled = false
            numberText.text = "--"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task7_activity_main)

        numberText = findViewById(R.id.numberText)
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)

        connectButton.setOnClickListener {
            bindService()
        }

        disconnectButton.setOnClickListener {
            unbindService()
        }
    }

    private fun bindService() {
        val intent = Intent(this, RandomNumberService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        if (isBound) {
            randomService?.removeListener(this)
            randomService?.stopGenerating()
            unbindService(connection)
            isBound = false
        }
    }

    override fun onNumberUpdated(number: Int) {
        runOnUiThread {
            numberText.text = number.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService()
        }
    }
}