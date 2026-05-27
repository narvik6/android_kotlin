package com.bibo.diaryentries.di

import com.bibo.diaryentries.data.FirestoreDiaryEntryRepository
import com.bibo.diaryentries.domain.CreateDiaryEntryUseCase
import com.bibo.diaryentries.domain.DeleteDiaryEntryUseCase
import com.bibo.diaryentries.domain.DiaryEntryRepository
import com.bibo.diaryentries.domain.GetDiaryEntriesUseCase
import com.bibo.diaryentries.domain.GetDiaryEntryByIdUseCase
import com.bibo.diaryentries.domain.UpdateDiaryEntryUseCase
import org.koin.dsl.module

val diaryEntriesModule = module {
    single<DiaryEntryRepository> { FirestoreDiaryEntryRepository(get()) }
    single { CreateDiaryEntryUseCase(get()) }
    single { GetDiaryEntriesUseCase(get()) }
    single { GetDiaryEntryByIdUseCase(get()) }
    single { UpdateDiaryEntryUseCase(get()) }
    single { DeleteDiaryEntryUseCase(get()) }
}
