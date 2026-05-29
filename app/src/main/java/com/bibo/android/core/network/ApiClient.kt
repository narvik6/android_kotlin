package com.bibo.android.core.network

import com.bibo.android.BuildConfig
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.util.AppResult
import com.bibo.android.core.util.DomainError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val NetworkErrorCode = "NETWORK_ERROR"

class ApiClient(
    private val userPreferences: UserPreferences,
    private val json: Json,
) {
    val httpClient: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = userPreferences.authData.first().token
                    token?.let { BearerTokens(accessToken = it, refreshToken = "") }
                }
            }
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        expectSuccess = false
    }

    suspend fun login(email: String, password: String): AppResult<AuthResponse> =
        executeAuth {
            httpClient.post("${BuildConfig.API_BASE_URL}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password))
            }
        }

    suspend fun register(email: String, password: String): AppResult<AuthResponse> =
        executeAuth {
            httpClient.post("${BuildConfig.API_BASE_URL}/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email = email, password = password))
            }
        }

    suspend fun getDiaryEntries(): AppResult<List<DiaryEntryResponse>> =
        executeRequest {
            httpClient.get("${BuildConfig.API_BASE_URL}/diary-entries")
        }

    suspend fun createDiaryEntry(
        request: CreateDiaryEntryRequest,
    ): AppResult<DiaryEntryResponse> =
        executeRequest {
            httpClient.post("${BuildConfig.API_BASE_URL}/diary-entries") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun updateDiaryEntry(
        remoteId: String,
        request: UpdateDiaryEntryRequest,
    ): AppResult<DiaryEntryResponse> =
        executeRequest {
            httpClient.put("${BuildConfig.API_BASE_URL}/diary-entries/$remoteId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun deleteDiaryEntry(remoteId: String): AppResult<Unit> =
        runCatching {
            val response = httpClient.delete("${BuildConfig.API_BASE_URL}/diary-entries/$remoteId")
            when (response.status) {
                HttpStatusCode.NoContent, HttpStatusCode.NotFound -> AppResult.Success(Unit)
                else -> AppResult.Error(response.toDomainError())
            }
        }.getOrElse { throwable ->
            AppResult.Error(throwable.toDomainError())
        }

    suspend fun getMeditationSessions(): AppResult<List<MeditationSessionResponse>> =
        executeRequest {
            httpClient.get("${BuildConfig.API_BASE_URL}/meditation-sessions")
        }

    suspend fun createMeditationSession(
        request: CreateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse> =
        executeRequest {
            httpClient.post("${BuildConfig.API_BASE_URL}/meditation-sessions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun updateMeditationSession(
        remoteId: String,
        request: UpdateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse> =
        executeRequest {
            httpClient.put("${BuildConfig.API_BASE_URL}/meditation-sessions/$remoteId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    suspend fun deleteMeditationSession(remoteId: String): AppResult<Unit> =
        runCatching {
            val response = httpClient.delete("${BuildConfig.API_BASE_URL}/meditation-sessions/$remoteId")
            when (response.status) {
                HttpStatusCode.NoContent, HttpStatusCode.NotFound -> AppResult.Success(Unit)
                else -> AppResult.Error(response.toDomainError())
            }
        }.getOrElse { throwable ->
            AppResult.Error(throwable.toDomainError())
        }

    private suspend fun executeAuth(
        request: suspend () -> HttpResponse,
    ): AppResult<AuthResponse> = runCatching {
        val response = request()
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> AppResult.Success(response.body<AuthResponse>())
            else -> AppResult.Error(response.toDomainError())
        }
    }.getOrElse { throwable ->
        AppResult.Error(throwable.toDomainError())
    }

    private suspend inline fun <reified T> executeRequest(
        crossinline request: suspend () -> HttpResponse,
    ): AppResult<T> = runCatching {
        val response = request()
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> AppResult.Success(response.body<T>())
            else -> AppResult.Error(response.toDomainError())
        }
    }.getOrElse { throwable ->
        AppResult.Error(throwable.toDomainError())
    }

    private suspend fun io.ktor.client.statement.HttpResponse.toDomainError(): DomainError {
        val text = bodyAsText()
        val apiError = runCatching { json.decodeFromString<ApiError>(text) }.getOrNull()
        return DomainError(
            code = apiError?.code ?: status.value.toString(),
            message = apiError?.message ?: "Сервер вернул ошибку ${status.value}",
        )
    }

    private fun Throwable.toDomainError(): DomainError =
        when (this) {
            is HttpRequestTimeoutException -> DomainError(NetworkErrorCode, "Сервер не ответил вовремя")
            is SerializationException -> DomainError("INVALID_RESPONSE", "Сервер вернул неожиданный ответ")
            else -> DomainError(NetworkErrorCode, "Не удалось выполнить сетевой запрос")
        }
}
