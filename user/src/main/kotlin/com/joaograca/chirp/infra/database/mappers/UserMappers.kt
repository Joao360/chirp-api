package com.joaograca.chirp.infra.database.mappers

import com.joaograca.chirp.domain.model.User
import com.joaograca.chirp.infra.database.entities.UserEntity

fun UserEntity.toUser() = User(
    id = id!!,
    email = email,
    username = username,
    hasEmailVerified = hasVerifiedEmail
)