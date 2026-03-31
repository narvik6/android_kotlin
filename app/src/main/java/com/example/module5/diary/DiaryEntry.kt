package com.example.module5.diary

data class DiaryEntry(
    val fileName: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val formattedDate: String
)
