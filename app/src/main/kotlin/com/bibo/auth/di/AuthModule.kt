package com.bibo.auth.di

import com.bibo.auth.data.FirestoreAuthRepository
import com.bibo.auth.domain.AuthRepository
import com.bibo.auth.domain.LoginUseCase
import com.bibo.auth.domain.RegisterUseCase
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { FirestoreAuthRepository(get()) }
    single { RegisterUseCase(get(), get(), get()) }
    single { LoginUseCase(get(), get(), get()) }
}
