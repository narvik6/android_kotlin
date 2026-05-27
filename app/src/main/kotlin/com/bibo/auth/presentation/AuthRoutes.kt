package com.bibo.auth.presentation

import com.bibo.auth.domain.LoginUseCase
import com.bibo.auth.domain.RegisterUseCase
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(
    registerUseCase: RegisterUseCase,
    loginUseCase: LoginUseCase,
) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val result = registerUseCase.execute(
                email = request.email,
                password = request.password,
            )

            call.respond(result.toResponse())
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = loginUseCase.execute(
                email = request.email,
                password = request.password,
            )

            call.respond(result.toResponse())
        }
    }
}
