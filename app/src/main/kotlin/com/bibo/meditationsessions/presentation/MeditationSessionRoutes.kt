package com.bibo.meditationsessions.presentation

import com.bibo.core.security.JwtService
import com.bibo.core.security.requireUserId
import com.bibo.meditationsessions.domain.CreateMeditationSessionUseCase
import com.bibo.meditationsessions.domain.DeleteMeditationSessionUseCase
import com.bibo.meditationsessions.domain.GetMeditationSessionByIdUseCase
import com.bibo.meditationsessions.domain.GetMeditationSessionsUseCase
import com.bibo.meditationsessions.domain.UpdateMeditationSessionUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.meditationSessionRoutes(
    createMeditationSessionUseCase: CreateMeditationSessionUseCase,
    getMeditationSessionsUseCase: GetMeditationSessionsUseCase,
    getMeditationSessionByIdUseCase: GetMeditationSessionByIdUseCase,
    updateMeditationSessionUseCase: UpdateMeditationSessionUseCase,
    deleteMeditationSessionUseCase: DeleteMeditationSessionUseCase,
) {
    authenticate(JwtService.AUTH_PROVIDER) {
        route("/meditation-sessions") {
            get {
                val userId = call.requireUserId()
                val query = call.request.queryParameters["query"]
                val sessions = getMeditationSessionsUseCase.execute(userId, query).map { it.toResponse() }

                call.respond(sessions)
            }

            post {
                val userId = call.requireUserId()
                val request = call.receive<CreateMeditationSessionRequest>()
                val session = createMeditationSessionUseCase.execute(userId, request.toInput())

                call.respond(HttpStatusCode.Created, session.toResponse())
            }

            get("/{id}") {
                val userId = call.requireUserId()
                val sessionId = call.parameters["id"].orEmpty()
                val session = getMeditationSessionByIdUseCase.execute(userId, sessionId)

                call.respond(session.toResponse())
            }

            put("/{id}") {
                val userId = call.requireUserId()
                val sessionId = call.parameters["id"].orEmpty()
                val request = call.receive<UpdateMeditationSessionRequest>()
                val session = updateMeditationSessionUseCase.execute(userId, sessionId, request.toInput())

                call.respond(session.toResponse())
            }

            delete("/{id}") {
                val userId = call.requireUserId()
                val sessionId = call.parameters["id"].orEmpty()

                deleteMeditationSessionUseCase.execute(userId, sessionId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
