package com.bibo.diaryentries.domain

import kotlinx.datetime.Instant

data class DiaryEntry(
    val id: String,
    val ownerUserId: String,
    val text: String?,
    val mood: Int?,
    val dateTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)
