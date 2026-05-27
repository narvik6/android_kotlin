package com.bibo.diaryentries.presentation

import com.bibo.core.security.JwtService
import com.bibo.core.security.requireUserId
import com.bibo.diaryentries.domain.CreateDiaryEntryUseCase
import com.bibo.diaryentries.domain.DeleteDiaryEntryUseCase
import com.bibo.diaryentries.domain.GetDiaryEntriesUseCase
import com.bibo.diaryentries.domain.GetDiaryEntryByIdUseCase
import com.bibo.diaryentries.domain.UpdateDiaryEntryUseCase
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

fun Route.diaryEntryRoutes(
    createDiaryEntryUseCase: CreateDiaryEntryUseCase,
    getDiaryEntriesUseCase: GetDiaryEntriesUseCase,
    getDiaryEntryByIdUseCase: GetDiaryEntryByIdUseCase,
    updateDiaryEntryUseCase: UpdateDiaryEntryUseCase,
    deleteDiaryEntryUseCase: DeleteDiaryEntryUseCase,
) {
    authenticate(JwtService.AUTH_PROVIDER) {
        route("/diary-entries") {
            get {
                val userId = call.requireUserId()
                val query = call.request.queryParameters["query"]
                val entries = getDiaryEntriesUseCase.execute(userId, query).map { it.toResponse() }

                call.respond(entries)
            }

            post {
                val userId = call.requireUserId()
                val request = call.receive<CreateDiaryEntryRequest>()
                val entry = createDiaryEntryUseCase.execute(userId, request.toInput())

                call.respond(HttpStatusCode.Created, entry.toResponse())
            }

            get("/{id}") {
                val userId = call.requireUserId()
                val entryId = call.parameters["id"].orEmpty()
                val entry = getDiaryEntryByIdUseCase.execute(userId, entryId)

                call.respond(entry.toResponse())
            }

            put("/{id}") {
                val userId = call.requireUserId()
                val entryId = call.parameters["id"].orEmpty()
                val request = call.receive<UpdateDiaryEntryRequest>()
                val entry = updateDiaryEntryUseCase.execute(userId, entryId, request.toInput())

                call.respond(entry.toResponse())
            }

            delete("/{id}") {
                val userId = call.requireUserId()
                val entryId = call.parameters["id"].orEmpty()

                deleteDiaryEntryUseCase.execute(userId, entryId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
