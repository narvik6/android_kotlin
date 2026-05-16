package com.example.module6_t3.di

import android.content.Context
import android.util.Log
import com.example.module6_t3.data.local.TokenManager
import com.example.module6_t3.data.repository.AuthRepositoryImpl
import com.example.module6_t3.data.repository.UserRepositoryImpl
import com.example.module6_t3.domain.repository.AuthRepository
import com.example.module6_t3.domain.repository.UserRepository
import com.example.module6_t3.domain.usecase.CheckAuthUseCase
import com.example.module6_t3.domain.usecase.GetUserByIdUseCase
import com.example.module6_t3.domain.usecase.GetUsersUseCase
import com.example.module6_t3.domain.usecase.LoginUseCase
import com.example.module6_t3.domain.usecase.LogoutUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AppContainer(private val context: Context) {

    // Инициализируем менеджер токенов
    val tokenManager: TokenManager by lazy {
        TokenManager(context)
    }

    // Настраиваем Ktor
    private val ktorClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.INFO
        }

        // САМОЕ ВАЖНОЕ: Настройка авторизации
        install(Auth) {
            bearer {
                loadTokens {
                    // Достаем токен из DataStore перед каждым запросом
                    val token = tokenManager.getToken()
                    if (token != null) {
                        BearerTokens(accessToken = token, refreshToken = "")
                    } else {
                        null
                    }
                }
            }
        }
    }

    // Репозитории
    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(ktorClient, tokenManager)
    }

    private val userRepository: UserRepository by lazy {
        UserRepositoryImpl(ktorClient)
    }

    // UseCases (сценарии)
    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }
    val checkAuthUseCase by lazy { CheckAuthUseCase(authRepository) }

    val getUsersUseCase by lazy { GetUsersUseCase(userRepository) }
    val getUserByIdUseCase by lazy { GetUserByIdUseCase(userRepository) }
}