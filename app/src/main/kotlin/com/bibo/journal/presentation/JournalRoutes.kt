package com.bibo.journal.presentation

import com.bibo.core.security.JwtService
import com.bibo.core.security.requireUserId
import com.bibo.journal.domain.GetJournalUseCase
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.journalRoutes(
    getJournalUseCase: GetJournalUseCase,
) {
    authenticate(JwtService.AUTH_PROVIDER) {
        route("/journal") {
            get {
                val userId = call.requireUserId()
                val query = call.request.queryParameters["query"]
                val items = getJournalUseCase.execute(userId, query).map { it.toResponse() }

                call.respond(items)
            }
        }
    }
}
