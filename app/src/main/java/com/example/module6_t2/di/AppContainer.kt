package com.example.module6_t2.di

import android.util.Log
import com.example.module6_t2.data.repository.NobelRepositoryImpl
import com.example.module6_t2.domain.repository.NobelRepository
import com.example.module6_t2.domain.usecase.GetNobelPrizesUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AppContainer {

    // Настраиваем Ktor Client
    private val ktorClient = HttpClient(Android) {
        // Подключаем сериализацию JSON
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        // Подключаем логирование
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.INFO
        }
    }

    private val nobelRepository: NobelRepository by lazy {
        NobelRepositoryImpl(ktorClient)
    }

    val getNobelPrizesUseCase: GetNobelPrizesUseCase by lazy {
        GetNobelPrizesUseCase(nobelRepository)
    }
}