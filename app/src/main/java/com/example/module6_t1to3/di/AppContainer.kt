package com.example.module6_t1to3.di

import com.example.module6_t1to3.data.remote.PicsumApi
import com.example.module6_t1to3.data.repository.PhotoRepositoryImpl
import com.example.module6_t1to3.domain.repository.PhotoRepository
import com.example.module6_t1to3.domain.usecase.GetPhotosUseCase
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class AppContainer {
    private val baseUrl = "https://picsum.photos/"

    // Настраиваем парсер JSON (игнорируем неизвестные поля, чтобы приложение не падало)
    private val json = Json { ignoreUnknownKeys = true }

    // Логирование запросов (очень поможет при отладке)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()

    // Ленивая инициализация: объекты создадутся только в момент первого обращения к ним
    private val retrofitService: PicsumApi by lazy {
        retrofit.create(PicsumApi::class.java)
    }

    private val photoRepository: PhotoRepository by lazy {
        PhotoRepositoryImpl(retrofitService)
    }

    val getPhotosUseCase: GetPhotosUseCase by lazy {
        GetPhotosUseCase(photoRepository)
    }
}