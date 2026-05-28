package com.bibo.android.core.di

import androidx.room.Room
import com.bibo.android.core.database.AppDatabase
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.network.ApiClient
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
import com.bibo.android.features.diary.domain.SyncPendingChangesUseCase
import com.bibo.android.features.diary.domain.UpdateDiaryEntryUseCase
import com.bibo.android.features.diary.presentation.DiaryViewModel
import com.bibo.android.features.journal.presentation.JournalViewModel
import com.bibo.android.features.main.presentation.ObserveThemeUseCase
import com.bibo.android.features.main.presentation.RootViewModel
import com.bibo.android.features.main.presentation.SetDarkThemeUseCase
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
    single { ApiClient(get(), get()) }
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "bibo.db",
        ).build()
    }
    single { get<AppDatabase>().diaryEntryDao() }
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
    viewModel { RootViewModel(get(), get(), get(), get(), get()) }
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
    factory { SyncPendingChangesUseCase(get()) }
    viewModel { DiaryViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { JournalViewModel(get(), get(), get()) }
}

val appModules = listOf(
    coreModule,
    authModule,
    mainModule,
    diaryModule,
)
