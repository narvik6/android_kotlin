package com.bibo.journal.di

import com.bibo.journal.domain.GetJournalUseCase
import org.koin.dsl.module

val journalModule = module {
    single { GetJournalUseCase(get(), get()) }
}
