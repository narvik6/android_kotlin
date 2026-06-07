package com.bibo.android.core.di

import androidx.room.Room
import com.bibo.android.core.database.AppDatabase
import com.bibo.android.core.datastore.CurrentUserProvider
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.network.ApiClient
import com.bibo.android.core.network.BiboApi
import com.bibo.android.core.network.ServerAvailabilityMonitor
import com.bibo.android.core.sync.SyncManager
import com.bibo.android.core.sync.SyncQueue
import com.bibo.android.core.sync.WorkManagerSyncScheduler
import com.bibo.android.features.auth.data.AuthRepositoryImpl
import com.bibo.android.features.auth.domain.AuthRepository
import com.bibo.android.features.auth.domain.LoginUseCase
import com.bibo.android.features.auth.domain.LogoutUseCase
import com.bibo.android.features.auth.domain.ObserveCurrentUserUseCase
import com.bibo.android.features.auth.domain.RegisterUseCase
import com.bibo.android.features.auth.presentation.AuthViewModel
import com.bibo.android.features.diary.data.DiaryRepositoryImpl
import com.bibo.android.features.diary.domain.CalculateDailyMoodUseCase
import com.bibo.android.features.diary.domain.CreateDiaryEntryUseCase
import com.bibo.android.features.diary.domain.DeleteDiaryEntryUseCase
import com.bibo.android.features.diary.domain.DiaryRepository
import com.bibo.android.features.diary.domain.GetDiaryEntriesUseCase
import com.bibo.android.features.diary.domain.GetDiaryEntryByIdUseCase
import com.bibo.android.features.diary.domain.GetJournalItemsUseCase
import com.bibo.android.features.diary.domain.RefreshDiaryEntriesUseCase
import com.bibo.android.features.diary.domain.UpdateDiaryEntryUseCase
import com.bibo.android.features.diary.presentation.DiaryViewModel
import com.bibo.android.features.journal.presentation.JournalViewModel
import com.bibo.android.features.journal.domain.AddSearchHistoryItemUseCase
import com.bibo.android.features.journal.domain.ClearSearchHistoryUseCase
import com.bibo.android.features.journal.domain.ObserveSearchHistoryUseCase
import com.bibo.android.features.journal.domain.SearchJournalItemsUseCase
import com.bibo.android.features.main.presentation.ObserveThemeUseCase
import com.bibo.android.features.main.presentation.RootViewModel
import com.bibo.android.features.main.presentation.SetDarkThemeUseCase
import com.bibo.android.features.main.presentation.SyncAppDataUseCase
import com.bibo.android.features.meditation.data.MeditationRepositoryImpl
import com.bibo.android.features.meditation.domain.CreateMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.DeleteMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.FinishMeditationSessionUseCase
import com.bibo.android.features.meditation.domain.GetMeditationSessionByIdUseCase
import com.bibo.android.features.meditation.domain.GetMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.MeditationRepository
import com.bibo.android.features.meditation.domain.RefreshMeditationSessionsUseCase
import com.bibo.android.features.meditation.domain.StartMeditationTimerUseCase
import com.bibo.android.features.meditation.domain.UpdateMeditationSessionUseCase
import com.bibo.android.features.meditation.presentation.MeditationViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val coreModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    single { UserPreferences(androidContext()) }
    single<CurrentUserProvider> { get<UserPreferences>() }
    single { ApiClient(get(), get()) }
    single<BiboApi> { get<ApiClient>() }
    single { ServerAvailabilityMonitor(get()) }
    single<com.bibo.android.core.sync.SyncScheduler> { WorkManagerSyncScheduler(androidContext()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "bibo.db",
        )
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }
    single { get<AppDatabase>().diaryEntryDao() }
    single { get<AppDatabase>().meditationSessionDao() }
    single { get<AppDatabase>().syncOperationDao() }
    single { SyncManager(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<SyncQueue> { get<SyncManager>() }
}

private val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { ObserveCurrentUserUseCase(get()) }
    viewModel { AuthViewModel(get(), get()) }
}

private val mainModule = module {
    factory { ObserveThemeUseCase(get()) }
    factory { SetDarkThemeUseCase(get()) }
    factory { SyncAppDataUseCase(get()) }
    viewModel { RootViewModel(get(), get(), get(), get(), get(), get()) }
}

private val diaryModule = module {
    single<DiaryRepository> { DiaryRepositoryImpl(get(), get(), get()) }
    factory { GetDiaryEntriesUseCase(get()) }
    factory { GetDiaryEntryByIdUseCase(get()) }
    factory { CreateDiaryEntryUseCase(get()) }
    factory { UpdateDiaryEntryUseCase(get()) }
    factory { DeleteDiaryEntryUseCase(get()) }
    factory { CalculateDailyMoodUseCase() }
    factory { GetJournalItemsUseCase(get()) }
    factory { RefreshDiaryEntriesUseCase(get()) }
    viewModel { DiaryViewModel(get(), get(), get(), get(), get(), get()) }
    factory { SearchJournalItemsUseCase() }
    factory { ObserveSearchHistoryUseCase(get()) }
    factory { AddSearchHistoryItemUseCase(get()) }
    factory { ClearSearchHistoryUseCase(get()) }
    viewModel { JournalViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}

private val meditationModule = module {
    single<MeditationRepository> { MeditationRepositoryImpl(get(), get(), get()) }
    factory { StartMeditationTimerUseCase() }
    factory { FinishMeditationSessionUseCase(get()) }
    factory { GetMeditationSessionsUseCase(get()) }
    factory { GetMeditationSessionByIdUseCase(get()) }
    factory { CreateMeditationSessionUseCase(get()) }
    factory { UpdateMeditationSessionUseCase(get()) }
    factory { DeleteMeditationSessionUseCase(get()) }
    factory { RefreshMeditationSessionsUseCase(get()) }
    viewModel { MeditationViewModel(get(), get(), get(), get(), get(), get(), androidContext()) }
}

val appModules = listOf(
    coreModule,
    authModule,
    mainModule,
    diaryModule,
    meditationModule,
)
