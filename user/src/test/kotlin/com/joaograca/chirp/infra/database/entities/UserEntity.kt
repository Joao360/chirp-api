package com.joaograca.chirp.infra.database.entities

import java.util.*

fun userEntity(
    id: UUID = UUID.randomUUID(),
    email: String = "test@example.com",
    username: String = "testuser",
    hashedPassword: String = "hashed_password",
    hasVerifiedEmail: Boolean = true
) = UserEntity(
    id = id,
    email = email,
    username = username,
    hashedPassword = hashedPassword,
    hasVerifiedEmail = hasVerifiedEmail
)