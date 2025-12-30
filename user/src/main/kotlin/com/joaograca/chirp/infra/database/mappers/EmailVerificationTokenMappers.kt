package com.joaograca.chirp.infra.database.mappers

import com.joaograca.chirp.domain.model.EmailVerificationToken
import com.joaograca.chirp.infra.database.entities.EmailVerificationTokenEntity

fun EmailVerificationTokenEntity.toEmailVerificationToken() = EmailVerificationToken(
    id = id,
    token = token,
    user = user.toUser()
)