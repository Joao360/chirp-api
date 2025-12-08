package com.joaograca.chirp.api.mappers

import com.joaograca.chirp.api.dto.AuthenticatedUserDto
import com.joaograca.chirp.api.dto.UserDto
import com.joaograca.chirp.domain.model.AuthenticatedUser
import com.joaograca.chirp.domain.model.User

fun AuthenticatedUser.toAuthenticatedUserDto() = AuthenticatedUserDto(
    user = user.toUserDto(),
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun User.toUserDto() = UserDto(
    id = id,
    email = email,
    username = username,
    hasVerifiedEmail = hasEmailVerified
)