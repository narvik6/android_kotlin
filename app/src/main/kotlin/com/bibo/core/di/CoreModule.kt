package com.bibo.core.di

import com.bibo.core.config.AppConfig
import com.bibo.core.firestore.FirestoreProvider
import com.bibo.core.security.JwtService
import com.bibo.core.security.PasswordHasher
import org.koin.dsl.module

fun coreModule(config: AppConfig) = module {
    single { config }
    single { config.jwt }
    single { config.firestore }
    single { JwtService(get()) }
    single { PasswordHasher() }
    single { FirestoreProvider(get()) }
}
