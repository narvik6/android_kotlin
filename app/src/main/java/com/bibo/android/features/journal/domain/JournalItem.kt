package com.bibo.android.features.journal.domain

import com.bibo.android.features.diary.domain.DiaryEntry
import com.bibo.android.features.meditation.domain.MeditationSession

sealed interface JournalItem {
    val id: String
    val sortDateTime: String

    data class Diary(val entry: DiaryEntry) : JournalItem {
        override val id: String = "diary:${entry.localId}"
        override val sortDateTime: String = entry.dateTime
    }

    data class Meditation(val session: MeditationSession) : JournalItem {
        override val id: String = "meditation:${session.localId}"
        override val sortDateTime: String = session.startedAt
    }
}
