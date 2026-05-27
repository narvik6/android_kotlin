package com.bibo.diaryentries.data

import com.google.cloud.Timestamp

data class FirestoreDiaryEntryModel(
    val id: String,
    val ownerUserId: String,
    val text: String?,
    val mood: Int?,
    val dateTime: Timestamp,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
