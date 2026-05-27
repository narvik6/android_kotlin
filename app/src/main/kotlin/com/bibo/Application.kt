package com.bibo

import com.bibo.auth.di.authModule
import com.bibo.auth.domain.LoginUseCase
import com.bibo.auth.domain.RegisterUseCase
import com.bibo.auth.presentation.authRoutes
import com.bibo.core.config.AppConfig
import com.bibo.core.config.AppConfigLoader
import com.bibo.core.di.coreModule
import com.bibo.core.error.configureErrorHandling
import com.bibo.core.routing.healthRoutes
import com.bibo.core.security.JwtService
import com.bibo.core.security.configureSecurity
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.koin.logger.slf4jLogger
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin

fun main() {
    val config = AppConfigLoader.load()

    embeddedServer(
        factory = Netty,
        host = config.host,
        port = config.port,
        module = { module(config) },
    ).start(wait = true)
}

fun Application.module(config: AppConfig = AppConfigLoader.load()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    install(Koin) {
        slf4jLogger()
        modules(
            coreModule(config),
            authModule,
        )
    }

    configureErrorHandling()

    val jwtService by inject<JwtService>()
    configureSecurity(config.jwt, jwtService)

    val registerUseCase by inject<RegisterUseCase>()
    val loginUseCase by inject<LoginUseCase>()

    routing {
        healthRoutes()
        authRoutes(registerUseCase, loginUseCase)
    }
}
