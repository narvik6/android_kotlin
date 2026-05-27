package com.bibo.diaryentries.domain

import kotlinx.datetime.Instant

data class DiaryEntryInput(
    val text: String?,
    val mood: Int?,
    val dateTime: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
