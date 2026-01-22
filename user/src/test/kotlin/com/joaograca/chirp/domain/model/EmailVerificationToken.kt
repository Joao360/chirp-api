package com.joaograca.chirp.domain.model

fun emailVerificationToken(user: User) =
    EmailVerificationToken(
        id = 1L,
        token = "test-token",
        user = user
    )