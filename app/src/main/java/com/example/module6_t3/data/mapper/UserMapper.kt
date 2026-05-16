package com.example.module6_t3.data.mapper

import com.example.module6_t3.data.remote.dto.UserDto
import com.example.module6_t3.domain.model.User

fun UserDto.toDomain(): User {
    return User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        email = email,
        image = image
    )
}