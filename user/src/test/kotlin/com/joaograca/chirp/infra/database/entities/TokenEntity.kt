package com.joaograca.chirp.infra.database.entities

import java.time.Instant
import java.time.temporal.ChronoUnit

fun createTokenEntity(
    id: Long = 1L,
    token: String = "test-token",
    user: UserEntity,
    expiresAt: Instant = Instant.now().plus(24, ChronoUnit.HOURS),
    usedAt: Instant? = null
) = EmailVerificationTokenEntity(
    id = id,
    token = token,
    expiresAt = expiresAt,
    user = user,
    usedAt = usedAt
)