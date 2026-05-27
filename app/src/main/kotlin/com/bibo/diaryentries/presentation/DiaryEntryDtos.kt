package com.bibo.diaryentries.presentation

import com.bibo.diaryentries.domain.DiaryEntry
import com.bibo.diaryentries.domain.DiaryEntryInput
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateDiaryEntryRequest(
    val text: String? = null,
    val mood: Int? = null,
    val dateTime: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class UpdateDiaryEntryRequest(
    val text: String? = null,
    val mood: Int? = null,
    val dateTime: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

@Serializable
data class DiaryEntryResponse(
    val id: String,
    val text: String?,
    val mood: Int?,
    val dateTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun CreateDiaryEntryRequest.toInput(): DiaryEntryInput =
    DiaryEntryInput(
        text = text,
        mood = mood,
        dateTime = dateTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun UpdateDiaryEntryRequest.toInput(): DiaryEntryInput =
    DiaryEntryInput(
        text = text,
        mood = mood,
        dateTime = dateTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun DiaryEntry.toResponse(): DiaryEntryResponse =
    DiaryEntryResponse(
        id = id,
        text = text,
        mood = mood,
        dateTime = dateTime,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
