package com.example.module5_t1to3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.module5_t1to3.diary.DiaryApp
import com.example.module5_t1to3.ui.theme.Module5Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Module5Theme {
                DiaryApp()       // Задание 1
                // GalleryScreen()  // Задание 2
            }
        }
    }
}