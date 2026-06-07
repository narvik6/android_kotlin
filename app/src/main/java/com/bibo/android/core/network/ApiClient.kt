package com.bibo.android.core.network

import com.bibo.android.BuildConfig
import com.bibo.android.core.datastore.UserPreferences
import com.bibo.android.core.util.AppResult
import com.bibo.android.core.util.DomainError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val NetworkErrorCode = "NETWORK_ERROR"
private const val ApiTimeoutMillis = 3_000L

class ApiClient(
    private val userPreferences: UserPreferences,
    private val json: Json,
) : BiboApi {
    val httpClient: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = ApiTimeoutMillis
            connectTimeoutMillis = ApiTimeoutMillis
            socketTimeoutMillis = ApiTimeoutMillis
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
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

    override suspend fun checkHealth(): AppResult<Unit> =
        runCatching {
            val response = withTimeout(ApiTimeoutMillis) {
                httpClient.get("${BuildConfig.API_BASE_URL}/health")
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> AppResult.Success(Unit)
                else -> AppResult.Error(response.toDomainError())
            }
        }.getOrElse { throwable ->
            AppResult.Error(throwable.toDomainError())
        }

    override suspend fun getDiaryEntries(): AppResult<List<DiaryEntryResponse>> =
        executeRequest {
            val token = currentToken()
            httpClient.get("${BuildConfig.API_BASE_URL}/diary-entries") {
                authorize(token)
            }
        }

    override suspend fun createDiaryEntry(
        request: CreateDiaryEntryRequest,
    ): AppResult<DiaryEntryResponse> =
        executeRequest {
            val token = currentToken()
            httpClient.post("${BuildConfig.API_BASE_URL}/diary-entries") {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    override suspend fun updateDiaryEntry(
        remoteId: String,
        request: UpdateDiaryEntryRequest,
    ): AppResult<DiaryEntryResponse> =
        executeRequest {
            val token = currentToken()
            httpClient.put("${BuildConfig.API_BASE_URL}/diary-entries/$remoteId") {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    override suspend fun deleteDiaryEntry(remoteId: String): AppResult<Unit> =
        runCatching {
            val token = currentToken()
            val response = withTimeout(ApiTimeoutMillis) {
                httpClient.delete("${BuildConfig.API_BASE_URL}/diary-entries/$remoteId") {
                    authorize(token)
                }
            }
            when (response.status) {
                HttpStatusCode.NoContent, HttpStatusCode.NotFound -> AppResult.Success(Unit)
                else -> AppResult.Error(response.toDomainError())
            }
        }.getOrElse { throwable ->
            AppResult.Error(throwable.toDomainError())
        }

    override suspend fun getMeditationSessions(): AppResult<List<MeditationSessionResponse>> =
        executeRequest {
            val token = currentToken()
            httpClient.get("${BuildConfig.API_BASE_URL}/meditation-sessions") {
                authorize(token)
            }
        }

    override suspend fun createMeditationSession(
        request: CreateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse> =
        executeRequest {
            val token = currentToken()
            httpClient.post("${BuildConfig.API_BASE_URL}/meditation-sessions") {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    override suspend fun updateMeditationSession(
        remoteId: String,
        request: UpdateMeditationSessionRequest,
    ): AppResult<MeditationSessionResponse> =
        executeRequest {
            val token = currentToken()
            httpClient.put("${BuildConfig.API_BASE_URL}/meditation-sessions/$remoteId") {
                authorize(token)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    override suspend fun deleteMeditationSession(remoteId: String): AppResult<Unit> =
        runCatching {
            val token = currentToken()
            val response = withTimeout(ApiTimeoutMillis) {
                httpClient.delete("${BuildConfig.API_BASE_URL}/meditation-sessions/$remoteId") {
                    authorize(token)
                }
            }
            when (response.status) {
                HttpStatusCode.NoContent, HttpStatusCode.NotFound -> AppResult.Success(Unit)
                else -> AppResult.Error(response.toDomainError())
            }
    }.getOrElse { throwable ->
        AppResult.Error(throwable.toDomainError())
    }

    private suspend fun currentToken(): String? = userPreferences.authData.first().token

    private fun HttpRequestBuilder.authorize(token: String?) {
        if (!token.isNullOrBlank()) {
            bearerAuth(token)
        }
    }

    private suspend fun executeAuth(
        request: suspend () -> HttpResponse,
    ): AppResult<AuthResponse> = runCatching {
        val response = withTimeout(ApiTimeoutMillis) { request() }
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
        val response = withTimeout(ApiTimeoutMillis) { request() }
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
            is TimeoutCancellationException -> DomainError(NetworkErrorCode, "Сервер не ответил вовремя")
            is SerializationException -> DomainError("INVALID_RESPONSE", "Сервер вернул неожиданный ответ")
            else -> DomainError(NetworkErrorCode, "Не удалось выполнить сетевой запрос")
        }
}
