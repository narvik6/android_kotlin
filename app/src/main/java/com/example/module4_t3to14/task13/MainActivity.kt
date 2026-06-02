package com.example.module4_t3to14.task13

import com.example.module4_t3to14.R

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var rateText: TextView
    private lateinit var trendIcon: ImageView
    private lateinit var changeText: TextView
    private lateinit var refreshButton: Button

    private lateinit var viewModel: CurrencyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task13_activity_main)

        rateText = findViewById(R.id.rateText)
        trendIcon = findViewById(R.id.trendIcon)
        changeText = findViewById(R.id.changeText)
        refreshButton = findViewById(R.id.refreshButton)

        viewModel = ViewModelProvider(this)[CurrencyViewModel::class.java]

        // Наблюдаем за изменениями курса
        lifecycleScope.launch {
            viewModel.rate.collect { rate ->
                rateText.text = String.format("%.2f", rate)
            }
        }

        // Наблюдаем за изменениями тренда
        lifecycleScope.launch {
            viewModel.trend.collect { trend ->
                when (trend) {
                    CurrencyViewModel.Trend.UP -> {
                        trendIcon.setImageResource(R.drawable.ic_trend_up)
                        trendIcon.visibility = android.view.View.VISIBLE
                        changeText.setTextColor(Color.parseColor("#4CAF50"))
                    }
                    CurrencyViewModel.Trend.DOWN -> {
                        trendIcon.setImageResource(R.drawable.ic_trend_down)
                        trendIcon.visibility = android.view.View.VISIBLE
                        changeText.setTextColor(Color.parseColor("#F44336"))
                    }
                    null -> {
                        trendIcon.visibility = android.view.View.GONE
                    }
                }
            }
        }

        // Наблюдаем за изменениями процента
        lifecycleScope.launch {
            viewModel.changePercent.collect { change ->
                changeText.text = change
            }
        }

        refreshButton.setOnClickListener {
            viewModel.refreshRate()
        }
    }
}