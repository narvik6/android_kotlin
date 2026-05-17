package org.example.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.remote.dto.LoginRequestDto
import org.example.data.remote.dto.TokenResponseDto
import org.example.data.remote.dto.toDomain
import org.example.data.remote.dto.toDto
import org.example.di.Injection
import org.example.security.JwtConfig

fun Application.configureRouting() {
    routing {
        route("/auth") {
            post("/login") {
                // Принимаем DTO и сразу конвертируем в Domain модель
                val requestDto = call.receive<LoginRequestDto>()
                val isValid = Injection.authUseCase.authenticate(requestDto.toDomain())

                if (isValid) {
                    val token = JwtConfig.generateToken(requestDto.username)
                    // Отдаем DTO
                    call.respond(HttpStatusCode.OK, TokenResponseDto(token))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                }
            }
        }

        authenticate("auth-jwt") {
            route("/prizes") {
                get {
                    // Достаем Domain модели, маппим в DTO и отдаем
                    val prizes = Injection.prizeRepository.getAllPrizes().map { it.toDto() }
                    call.respond(HttpStatusCode.OK, prizes)
                }

                route("/{year}/{category}") {
                    get {
                        val year = call.parameters["year"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                        val prize = Injection.prizeRepository.getPrize(year, category)
                        if (prize != null) {
                            call.respond(HttpStatusCode.OK, prize.toDto())
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Prize not found"))
                        }
                    }

                    get("/laureates") {
                        val year = call.parameters["year"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                        val laureates = Injection.prizeRepository.getLaureates(year, category)
                        if (laureates != null && laureates.isNotEmpty()) {
                            call.respond(HttpStatusCode.OK, laureates.map { it.toDto() })
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Laureates not found"))
                        }
                    }
                }
            }
        }
    }
}