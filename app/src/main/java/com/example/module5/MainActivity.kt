package com.example.module5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.module5.diary.DiaryApp
import com.example.module5.gallery.GalleryScreen
import com.example.module5.ui.theme.Module5Theme

class MainActivity : ComponentActivity() {        // ← ComponentActivity, не AppCompatActivity
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