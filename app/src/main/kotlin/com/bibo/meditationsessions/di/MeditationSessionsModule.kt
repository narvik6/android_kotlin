package com.bibo.meditationsessions.di

import com.bibo.meditationsessions.data.FirestoreMeditationSessionRepository
import com.bibo.meditationsessions.domain.CreateMeditationSessionUseCase
import com.bibo.meditationsessions.domain.DeleteMeditationSessionUseCase
import com.bibo.meditationsessions.domain.GetMeditationSessionByIdUseCase
import com.bibo.meditationsessions.domain.GetMeditationSessionsUseCase
import com.bibo.meditationsessions.domain.MeditationSessionRepository
import com.bibo.meditationsessions.domain.UpdateMeditationSessionUseCase
import org.koin.dsl.module

val meditationSessionsModule = module {
    single<MeditationSessionRepository> { FirestoreMeditationSessionRepository(get()) }
    single { CreateMeditationSessionUseCase(get()) }
    single { GetMeditationSessionsUseCase(get()) }
    single { GetMeditationSessionByIdUseCase(get()) }
    single { UpdateMeditationSessionUseCase(get()) }
    single { DeleteMeditationSessionUseCase(get()) }
}
