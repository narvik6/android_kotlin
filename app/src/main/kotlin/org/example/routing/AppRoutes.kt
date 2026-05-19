package org.example.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.remote.dto.*
import org.example.di.Injection
import org.example.security.JwtConfig

fun Application.configureRouting() {
    routing {
        route("/auth") {
            post("/login") {
                val requestDto = call.receive<LoginRequestDto>()
                val isValid = Injection.authUseCase.authenticate(requestDto.toDomain())

                if (isValid) {
                    val token = JwtConfig.generateToken(requestDto.username)
                    call.respond(HttpStatusCode.OK, TokenResponseDto(token))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                }
            }
        }

        route("/prizes") {
            get {
                val prizes = Injection.prizeRepository.getAllPrizes().map { it.toDto() }
                call.respond(HttpStatusCode.OK, prizes)
            }

            get("/{year}/{category}") {
                val year = call.parameters["year"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val prize = Injection.prizeRepository.getPrize(year, category)?.toDto()
                if (prize != null) call.respond(prize) else call.respond(HttpStatusCode.NotFound)
            }

            get("/{year}/{category}/laureates") {
                val year = call.parameters["year"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val category = call.parameters["category"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val laureates = Injection.prizeRepository.getLaureates(year, category)?.map { it.toDto() }
                if (laureates != null) call.respond(laureates) else call.respond(HttpStatusCode.NotFound)
            }
        }

        authenticate("auth-jwt") {
            route("/users/me") {
                // Получение профиля
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.payload?.getClaim("username")?.asString() ?: return@get call.respond(
                        HttpStatusCode.Unauthorized
                    )

                    val user = Injection.userRepository.getUserByUsername(username)
                    if (user != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("id" to user.id, "username" to user.username, "role" to user.role)
                        )
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                // Избранные премии пользователя
                route("/prizes") {
                    get {
                        val user = getUserFromToken(call) ?: return@get call.respond(HttpStatusCode.Unauthorized)
                        val favorites = Injection.prizeRepository.getFavorites(user.id).map { it.toDto() }
                        call.respond(HttpStatusCode.OK, favorites)
                    }

                    post("/{prizeId}") {
                        val user = getUserFromToken(call) ?: return@post call.respond(HttpStatusCode.Unauthorized)
                        val prizeId = call.parameters["prizeId"]?.toIntOrNull() ?: return@post call.respond(
                            HttpStatusCode.BadRequest
                        )

                        val added = Injection.prizeRepository.addFavorite(user.id, prizeId)
                        if (added) call.respond(HttpStatusCode.Created) else call.respond(
                            HttpStatusCode.Conflict,
                            mapOf("error" to "Already added")
                        )
                    }

                    delete("/{prizeId}") {
                        val user = getUserFromToken(call) ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                        val prizeId = call.parameters["prizeId"]?.toIntOrNull() ?: return@delete call.respond(
                            HttpStatusCode.BadRequest
                        )

                        val removed = Injection.prizeRepository.removeFavorite(user.id, prizeId)
                        if (removed) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}

// Вспомогательная функция
private suspend fun getUserFromToken(call: ApplicationCall) =
    call.principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()?.let {
        Injection.userRepository.getUserByUsername(it)
    }