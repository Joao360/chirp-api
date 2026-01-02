package com.joaograca.chirp.infra.database.entities

import java.time.Instant
import java.time.temporal.ChronoUnit

fun createPasswordResetTokenEntity(
    id: Long = 1L,
    token: String = "test-token",
    user: UserEntity,
    expiresAt: Instant = Instant.now().plus(30, ChronoUnit.MINUTES),
    usedAt: Instant? = null
) = PasswordResetTokenEntity(
    id = id,
    token = token,
    user = user,
    expiresAt = expiresAt,
    usedAt = usedAt
)