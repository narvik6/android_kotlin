package com.example.module4_t3to14.task12

import com.example.module4_t3to14.R

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var generateButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var factCard: CardView
    private lateinit var factText: TextView
    private lateinit var placeholderText: TextView

    private lateinit var viewModel: MainViewModel

    private val fadeInAnimation by lazy {
        AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task12_activity_main)

        generateButton = findViewById(R.id.generateButton)
        progressBar = findViewById(R.id.progressBar)
        factCard = findViewById(R.id.factCard)
        factText = findViewById(R.id.factText)
        placeholderText = findViewById(R.id.placeholderText)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Устанавливаем колбэки
        viewModel.setCallbacks(
            onFactLoaded = { fact ->
                factText.text = fact
                factCard.startAnimation(fadeInAnimation)
            },
            onLoadingChanged = { isLoading ->
                if (isLoading) {
                    progressBar.visibility = android.view.View.VISIBLE
                    factCard.visibility = android.view.View.GONE
                    placeholderText.visibility = android.view.View.GONE
                } else {
                    progressBar.visibility = android.view.View.GONE
                }
            },
            onCardVisibilityChanged = { isVisible ->
                if (isVisible) {
                    factCard.visibility = android.view.View.VISIBLE
                    placeholderText.visibility = android.view.View.GONE
                } else {
                    factCard.visibility = android.view.View.GONE
                }
            }
        )

        // Если есть сохраненный факт (при повороте экрана) - показываем его
        if (viewModel.currentFact.isNotEmpty()) {
            factText.text = viewModel.currentFact
            factCard.visibility = android.view.View.VISIBLE
            placeholderText.visibility = android.view.View.GONE
        }

        generateButton.setOnClickListener {
            viewModel.generateNewFact()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearCallbacks()
    }
}