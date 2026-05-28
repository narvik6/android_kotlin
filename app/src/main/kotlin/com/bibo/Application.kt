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
import com.bibo.diaryentries.di.diaryEntriesModule
import com.bibo.diaryentries.domain.CreateDiaryEntryUseCase
import com.bibo.diaryentries.domain.DeleteDiaryEntryUseCase
import com.bibo.diaryentries.domain.GetDiaryEntriesUseCase
import com.bibo.diaryentries.domain.GetDiaryEntryByIdUseCase
import com.bibo.diaryentries.domain.UpdateDiaryEntryUseCase
import com.bibo.diaryentries.presentation.diaryEntryRoutes
import com.bibo.journal.di.journalModule
import com.bibo.journal.domain.GetJournalUseCase
import com.bibo.journal.presentation.journalRoutes
import com.bibo.meditationsessions.di.meditationSessionsModule
import com.bibo.meditationsessions.domain.CreateMeditationSessionUseCase
import com.bibo.meditationsessions.domain.DeleteMeditationSessionUseCase
import com.bibo.meditationsessions.domain.GetMeditationSessionByIdUseCase
import com.bibo.meditationsessions.domain.GetMeditationSessionsUseCase
import com.bibo.meditationsessions.domain.UpdateMeditationSessionUseCase
import com.bibo.meditationsessions.presentation.meditationSessionRoutes
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
            diaryEntriesModule,
            meditationSessionsModule,
            journalModule,
        )
    }

    configureErrorHandling()

    val jwtService by inject<JwtService>()
    configureSecurity(config.jwt, jwtService)

    val registerUseCase by inject<RegisterUseCase>()
    val loginUseCase by inject<LoginUseCase>()
    val createDiaryEntryUseCase by inject<CreateDiaryEntryUseCase>()
    val getDiaryEntriesUseCase by inject<GetDiaryEntriesUseCase>()
    val getDiaryEntryByIdUseCase by inject<GetDiaryEntryByIdUseCase>()
    val updateDiaryEntryUseCase by inject<UpdateDiaryEntryUseCase>()
    val deleteDiaryEntryUseCase by inject<DeleteDiaryEntryUseCase>()
    val createMeditationSessionUseCase by inject<CreateMeditationSessionUseCase>()
    val getMeditationSessionsUseCase by inject<GetMeditationSessionsUseCase>()
    val getMeditationSessionByIdUseCase by inject<GetMeditationSessionByIdUseCase>()
    val updateMeditationSessionUseCase by inject<UpdateMeditationSessionUseCase>()
    val deleteMeditationSessionUseCase by inject<DeleteMeditationSessionUseCase>()
    val getJournalUseCase by inject<GetJournalUseCase>()

    routing {
        healthRoutes()
        authRoutes(registerUseCase, loginUseCase)
        diaryEntryRoutes(
            createDiaryEntryUseCase = createDiaryEntryUseCase,
            getDiaryEntriesUseCase = getDiaryEntriesUseCase,
            getDiaryEntryByIdUseCase = getDiaryEntryByIdUseCase,
            updateDiaryEntryUseCase = updateDiaryEntryUseCase,
            deleteDiaryEntryUseCase = deleteDiaryEntryUseCase,
        )
        meditationSessionRoutes(
            createMeditationSessionUseCase = createMeditationSessionUseCase,
            getMeditationSessionsUseCase = getMeditationSessionsUseCase,
            getMeditationSessionByIdUseCase = getMeditationSessionByIdUseCase,
            updateMeditationSessionUseCase = updateMeditationSessionUseCase,
            deleteMeditationSessionUseCase = deleteMeditationSessionUseCase,
        )
        journalRoutes(getJournalUseCase)
    }
}
