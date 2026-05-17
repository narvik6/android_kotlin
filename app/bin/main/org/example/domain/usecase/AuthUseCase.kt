package org.example.domain.usecase

import org.example.domain.models.LoginRequest

class AuthUseCase {
    fun authenticate(request: LoginRequest): Boolean {
        return request.username == "emilys" && request.password == "emilyspass"
    }
}