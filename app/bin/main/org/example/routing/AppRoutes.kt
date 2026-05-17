package org.example.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.di.Injection
import org.example.domain.models.LoginRequest
import org.example.domain.models.TokenResponse
import org.example.security.JwtConfig

fun Application.configureRouting() {
    routing {
        route("/auth") {
            post("/login") {
                val request = call.receive<LoginRequest>()
                val isValid = Injection.authUseCase.authenticate(request)

                if (isValid) {
                    val token = JwtConfig.generateToken(request.username)
                    call.respond(HttpStatusCode.OK, TokenResponse(token))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                }
            }
        }

        // Блок, требующий наличия валидного JWT-токена в заголовке Authorization
        authenticate("auth-jwt") {
            route("/prizes") {
                get {
                    call.respond(HttpStatusCode.OK, Injection.prizeRepository.getAllPrizes())
                }

                route("/{year}/{category}") {
                    get {
                        val year = call.parameters["year"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                        val prize = Injection.prizeRepository.getPrize(year, category)
                        if (prize != null) {
                            call.respond(HttpStatusCode.OK, prize)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Prize not found"))
                        }
                    }

                    get("/laureates") {
                        val year = call.parameters["year"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)

                        val laureates = Injection.prizeRepository.getLaureates(year, category)
                        if (laureates != null && laureates.isNotEmpty()) {
                            call.respond(HttpStatusCode.OK, laureates)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Laureates not found"))
                        }
                    }
                }
            }
        }
    }
}