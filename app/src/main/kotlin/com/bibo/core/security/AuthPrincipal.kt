package com.bibo.core.security

import com.bibo.core.error.UnauthorizedException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun ApplicationCall.requireUserId(): String {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException()

    return principal.payload.getClaim(JwtService.USER_ID_CLAIM).asString()
        ?: throw UnauthorizedException()
}
